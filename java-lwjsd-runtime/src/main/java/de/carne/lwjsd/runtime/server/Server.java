/*
 * Copyright (c) 2018-2021 Holger de Carne and contributors, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.carne.lwjsd.runtime.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import de.carne.boot.Exceptions;
import de.carne.boot.logging.Log;
import de.carne.io.IOUtil;
import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ReasonMessage;
import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.api.ServiceContext;
import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.security.CharSecret;
import de.carne.lwjsd.runtime.security.Passwords;
import de.carne.lwjsd.runtime.security.SecretsStore;
import de.carne.lwjsd.runtime.ws.ControlApiExceptionMapper;
import de.carne.nio.file.FileUtil;
import de.carne.util.Debug;
import de.carne.util.Late;
import de.carne.util.SystemProperties;
import de.carne.util.function.FunctionException;

/**
 * This class runs the master server and provides the actual {@linkplain ServiceManager} and {@linkplain ServiceContext}
 * interface for service execution.
 */
public class Server implements ServiceManager, ServiceContext, AutoCloseable {

	private static final Log LOG = new Log();

	private static final int REQUEST_BACKLOG = getIntDefault(".requestBacklog", 100);
	private static final long WAIT_TIMEOUT = getLongDefault(".waitTimeout", 1000);

	private interface Request {

		void process() throws ServiceManagerException;

	}

	private volatile ServiceManagerState state = ServiceManagerState.CONFIGURED;
	private final SecretsStore secretsStore;
	private final ConfigStore configStore;
	private final ServiceStore serviceStore;
	private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>(REQUEST_BACKLOG);
	private final Late<Thread> serverThreadHolder = new Late<>();
	private final Late<HttpServer> httpServerHolder = new Late<>();

	/**
	 * Constructs new {@linkplain Server} instance.
	 * <p>
	 * Invoke {@linkplain #start(boolean)} to perform the actual server startup.
	 *
	 * @param config the {@linkplain Config} instance to use.
	 * @throws ServiceManagerException if an initialization error occurs during server setup.
	 */
	public Server(Config config) throws ServiceManagerException {
		try {
			this.secretsStore = SecretsStore.create(config);
			this.configStore = ConfigStore.create(config);
			this.serviceStore = ServiceStore.create(this.secretsStore, this, config);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerException(e, "Failed to open required store");
		}
	}

	/**
	 * Starts the master server and sets up the control interface as well as any already configured services.
	 * <p>
	 * If the server is started in the current thread the function only returns after the server has been stopped.
	 *
	 * @param foreground whether to start and run the server in the current thread ({@code true}) or in a background
	 * thread.
	 * @return the {@linkplain Thread} instance running the server.
	 * @throws ServiceManagerException if the startup fails.
	 * @throws InterruptedException if the thread is interrupted during server startup or execution.
	 */
	public synchronized Thread start(boolean foreground) throws ServiceManagerException, InterruptedException {
		if (this.state != ServiceManagerState.CONFIGURED) {
			throw new ServiceManagerException(
					ReasonMessage.illegalState("Master server has already been started (status: ''{0}'')", this.state));
		}

		Thread thread;

		if (foreground) {
			LOG.info("Starting master server...");
			LOG.debug("Using {0}", this.configStore);

			startHttpServer();

			this.state = ServiceManagerState.RUNNING;
			notifyAll();

			LOG.notice("Master server up and running");

			this.serviceStore.autoStartServices();
			thread = this.serverThreadHolder.set(Thread.currentThread());
			logUsedMemory();
			while (processRequest()) {
				sleep();
			}
			logUsedMemory();
		} else {
			thread = new Thread(() -> {
				try {
					start(true);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw Exceptions.toRuntime(e);
				} catch (ServiceManagerException e) {
					throw Exceptions.toRuntime(e);
				}
			}, toString());
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler(this::uncaughtExceptionHandler);
			thread.start();
			do {
				wait(WAIT_TIMEOUT);
			} while (this.state == ServiceManagerState.CONFIGURED);
		}
		return thread;
	}

	/**
	 * Gets the {@linkplain Thread} running the server.
	 *
	 * @return the {@linkplain Thread} running the server.
	 * @see #start(boolean)
	 */
	public Thread getServerThread() {
		return this.serverThreadHolder.get();
	}

	/**
	 * Receives and registers a {@linkplain Service} module file.
	 *
	 * @param fileStream the file stream to receive.
	 * @param fileName the file name of {@linkplain Service} module.
	 * @param force whether to force unloading and overwriting of an already running {@linkplain Service} module with
	 * the same name.
	 * @return the updated {@linkplain Service} module status.
	 * @throws ServiceManagerException if an error occurs while registering the {@linkplain Service} module.
	 */
	public ModuleInfo receiveAndRegisterModule(InputStream fileStream, String fileName, boolean force)
			throws ServiceManagerException {
		LOG.info("Receiving module file ''{0}''...", fileName);

		Path tempReceiveDir = null;
		ModuleInfo status;

		try {
			tempReceiveDir = Files.createTempDirectory("receive");

			Path file = tempReceiveDir.resolve(fileName);

			IOUtil.copyStream(file.toFile(), fileStream);
			status = registerModule(file, force);
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to receive file ''{0}''", fileName);
		} finally {
			if (tempReceiveDir != null) {
				try {
					FileUtil.delete(tempReceiveDir);
				} catch (IOException e) {
					LOG.warning(e, "Failed to delete temporary directory ''{0}''", tempReceiveDir);
				}
			}
		}
		return status;
	}

	@Override
	public ServiceManagerInfo queryStatus() throws ServiceManagerException {
		Collection<ModuleInfo> moduleInfos = this.serviceStore.queryModuleStatus();
		Collection<ServiceInfo> serviceInfos = this.serviceStore.queryServiceStatus();

		logUsedMemory();
		return new ServiceManagerInfo(this.configStore.getBaseUri(), this.state, moduleInfos, serviceInfos);
	}

	@Override
	public void requestStop() throws ServiceManagerException {
		submitRequest(this::stop);
	}

	@Override
	public ModuleInfo registerModule(Path file, boolean force) throws ServiceManagerException {
		ModuleInfo status = this.serviceStore.registerModule(file, force);

		this.serviceStore.syncStore();
		logUsedMemory();
		return status;
	}

	@Override
	public ModuleInfo loadModule(String moduleName) throws ServiceManagerException {
		ModuleInfo status = this.serviceStore.loadModule(moduleName);

		this.serviceStore.syncStore();
		logUsedMemory();
		return status;
	}

	@Override
	public void deleteModule(String moduleName) throws ServiceManagerException {
		this.serviceStore.deleteModule(moduleName);
		this.serviceStore.syncStore();
		logUsedMemory();
	}

	@Override
	public ServiceInfo registerService(String className) throws ServiceManagerException {
		ServiceId serviceId = new ServiceId(ServiceStore.RUNTIME_MODULE_NAME, className);
		ServiceInfo status = this.serviceStore.registerService(serviceId, false);

		this.serviceStore.syncStore();
		logUsedMemory();
		return status;
	}

	@Override
	public ServiceInfo startService(ServiceId serviceId, boolean autoStart) throws ServiceManagerException {
		ServiceInfo status = this.serviceStore.startService(serviceId, autoStart);

		this.serviceStore.syncStore();
		logUsedMemory();
		return status;
	}

	@Override
	public ServiceInfo stopService(ServiceId serviceId) throws ServiceManagerException {
		ServiceInfo status = this.serviceStore.stopService(serviceId, false);

		logUsedMemory();
		return status;
	}

	@Override
	public void addHttpHandler(HttpHandler httpHandler, HttpHandlerRegistration... mapping)
			throws ServiceManagerException {
		this.httpServerHolder.get().getServerConfiguration().addHttpHandler(httpHandler, mapping);
	}

	@Override
	public void removeHttpHandler(HttpHandler httpHandler) throws ServiceManagerException {
		this.httpServerHolder.get().getServerConfiguration().removeHttpHandler(httpHandler);
	}

	@Override
	public <T extends Service> T getService(Class<T> serviceClass) throws ServiceManagerException {
		return this.serviceStore.getService(serviceClass);
	}

	@Override
	public synchronized void close() {
		LOG.info("Cleaning up master server resources...");

		int pendingRequestCount = this.requests.size();

		if (pendingRequestCount > 0) {
			LOG.warning("Discarding {0} unprocessed server requests", pendingRequestCount);
			this.requests.clear();
		}
		this.httpServerHolder.getOptional().ifPresent(HttpServer::shutdownNow);
		this.serviceStore.close();
	}

	@Override
	public String toString() {
		return "Master server " + this.configStore.getBaseUri();
	}

	private void uncaughtExceptionHandler(Thread thread, Throwable exception) {
		LOG.error(exception, "Server failed with uncaught exception: {0}", exception.getClass().getName());

		try {
			stop();
		} catch (ServiceManagerException e) {
			exception.addSuppressed(e);
		}

		UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

		if (defaultUncaughtExceptionHandler != null) {
			defaultUncaughtExceptionHandler.uncaughtException(thread, exception);
		}
	}

	private synchronized void stop() throws ServiceManagerException {
		LOG.info("Stopping master server...");

		this.serviceStore.safeUnloadAllServices();
		this.state = ServiceManagerState.STOPPED;
		notifyAll();
		try {
			this.httpServerHolder.getOptional().ifPresent(httpServer -> {
				try {
					httpServer.shutdown(WAIT_TIMEOUT, TimeUnit.MILLISECONDS).get();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw Exceptions.toRuntime(e);
				} catch (ExecutionException e) {
					throw new FunctionException(e);
				}
			});
		} catch (FunctionException e) {
			throw new ServiceManagerException(e.getCause(), "HTTP server shutdown failed");
		}
		logUsedMemory();

		LOG.notice("Master server has been stopped");
	}

	private synchronized boolean processRequest() throws ServiceManagerException {
		Request request = this.requests.poll();

		if (request != null) {
			request.process();
		}
		return this.state == ServiceManagerState.RUNNING;
	}

	private synchronized void sleep() throws InterruptedException {
		while (this.state == ServiceManagerState.RUNNING && this.requests.isEmpty()) {
			this.wait(WAIT_TIMEOUT);
		}
	}

	private synchronized void submitRequest(Request request) throws ServiceManagerException {
		try {
			this.requests.add(request);
		} catch (IllegalStateException e) {
			throw new ServiceManagerException(e, "Too many unprocessed requests");
		}
		notifyAll();
	}

	private void startHttpServer() throws ServiceManagerException {
		LOG.info("Starting HTTP server...");

		try {
			Map<String, Object> controlResourceConfigProperties = new HashMap<>();

			controlResourceConfigProperties.put(getClass().getName(), this);

			ResourceConfig controlResourceConfig = new ResourceConfig(ControlApiService.class)
					.addProperties(controlResourceConfigProperties);

			controlResourceConfig.register(JacksonFeature.class).packages(MultiPartFeature.class.getPackageName())
					.register(MultiPartFeature.class).register(ControlApiExceptionMapper.class);

			URI baseUri = this.configStore.getBaseUri();
			SSLEngineConfigurator sslEngineConfigurator;
			boolean secure;

			if ("https".equals(baseUri.getScheme())) {
				sslEngineConfigurator = setupSslEngineConfigurator();
				secure = true;
			} else {
				sslEngineConfigurator = null;
				secure = false;
			}

			HttpServer server = this.httpServerHolder.set(GrizzlyHttpServerFactory.createHttpServer(baseUri,
					controlResourceConfig, secure, sslEngineConfigurator, false));

			server.start();
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to start HTTP server");
		}

		LOG.info("HTTP server up and running");
	}

	private SSLEngineConfigurator setupSslEngineConfigurator() throws ServiceManagerException {
		Path sslKeyStore = this.configStore.getConfDir().resolve(this.configStore.getSslKeyStoreFile());

		LOG.info("Using SSL key store: ''{0}''", sslKeyStore);

		SSLEngineConfigurator sslEngineConfigurator;

		try (CharSecret keyStorePassword = Passwords.decryptPassword(this.secretsStore,
				this.configStore.getSslKeyStoreSecret());
				InputStream keyStoreStream = Files.newInputStream(sslKeyStore)) {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			keyStore.load(keyStoreStream, keyStorePassword.get());

			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());

			keyManagerFactory.init(keyStore, keyStorePassword.get());

			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());

			trustManagerFactory.init(keyStore);

			SSLContext sslContext = SSLContext.getInstance(this.configStore.getSslProtocol());

			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			sslEngineConfigurator = new SSLEngineConfigurator(sslContext, false, true, true);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerException(e, "Failed to setup SSL engine");
		}
		return sslEngineConfigurator;
	}

	private static int getIntDefault(String propertyKey, int defaultValue) {
		return SystemProperties.intValue(Server.class.getName() + propertyKey, defaultValue);
	}

	private static long getLongDefault(String propertyKey, long defaultValue) {
		return SystemProperties.longValue(Server.class.getName() + propertyKey, defaultValue);
	}

	private static void logUsedMemory() {
		if (LOG.isDebugLoggable()) {
			LOG.debug("Used memory {0}", Debug.formatUsedMemory());
		}
	}

}
