/*
 * Copyright (c) 2018 Holger de Carne and contributors, All Rights Reserved.
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import de.carne.check.Check;
import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.config.SecretsStore;
import de.carne.lwjsd.runtime.config.ServiceStore;
import de.carne.lwjsd.runtime.security.CharSecret;
import de.carne.lwjsd.runtime.ws.ServiceManagerService;
import de.carne.util.Exceptions;
import de.carne.util.Late;
import de.carne.util.SystemProperties;
import de.carne.util.logging.Log;

/**
 * This class runs the LWJSD server and provides the actual {@linkplain ServiceManager} interface for service
 * configuration and control.
 */
public class Server implements ServiceManager, AutoCloseable {

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
	private final Late<Thread> serverThread = new Late<>();
	private final Late<HttpServer> httpServer = new Late<>();

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
			this.secretsStore = SecretsStore.open(config);
			this.configStore = ConfigStore.open(config);
			this.serviceStore = ServiceStore.open(config);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerException(e, "Failed to open required store");
		}
	}

	/**
	 * Starts the server and sets the control interface as well as all already configured services online.
	 * <p>
	 * If the server is started in the current thread the call is responsible for running the server request dispatch
	 * loop by invoking {@linkplain #processRequest()} and {@linkplain #sleep()}..
	 *
	 * @param foreground whether to start the server in the current thread ({@code true}) or in a background thread.
	 * @return the {@linkplain Thread} instance running the server.
	 * @throws ServiceManagerException if the startup fails.
	 * @throws InterruptedException if the thread is interrupted while waiting for the server startup.
	 * @see #processRequest()
	 * @see #sleep()
	 */
	public synchronized Thread start(boolean foreground) throws ServiceManagerException, InterruptedException {
		if (this.state != ServiceManagerState.CONFIGURED) {
			throw new ServiceManagerException("Server has already been started (status: ''{0}''", this.state);
		}

		Thread thread;

		if (foreground) {
			LOG.info("Starting server...");
			LOG.debug("Using {0}", this.configStore);

			this.state = ServiceManagerState.STARTING;
			notifyAll();

			startHttpServer();

			this.state = ServiceManagerState.RUNNING;
			notifyAll();

			LOG.notice("Server up and running");

			thread = this.serverThread.set(Thread.currentThread());
		} else {
			thread = new Thread(() -> {
				try {
					start(true);
					while (processRequest()) {
						sleep();
					}
				} catch (ServiceManagerException | InterruptedException e) {
					throw Exceptions.toRuntime(e);
				}
			}, toString());
			thread.start();
			do {
				wait(WAIT_TIMEOUT);
			} while (this.state != ServiceManagerState.RUNNING);
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
		return this.serverThread.get();
	}

	/**
	 * Process any pending server request and check whether server is still in running state.
	 *
	 * @return {@code true} if the server is still running.
	 * @throws ServiceManagerException if request processing fails.
	 */
	public synchronized boolean processRequest() throws ServiceManagerException {
		Check.assertTrue(Thread.currentThread().equals(this.serverThread.get()));

		Request request = this.requests.poll();

		if (request != null) {
			request.process();
		}
		return this.state == ServiceManagerState.RUNNING;
	}

	/**
	 * Sleep until the next server request is pending or the server is no longer running.
	 *
	 * @throws InterruptedException if the sleep operation is interrupted.
	 */
	public synchronized void sleep() throws InterruptedException {
		Check.assertTrue(Thread.currentThread().equals(this.serverThread.get()));

		while (this.state == ServiceManagerState.RUNNING && this.requests.isEmpty()) {
			this.wait(WAIT_TIMEOUT);
		}
	}

	/**
	 * Stops the server and sets all services offline.
	 *
	 * @throws ServiceManagerException if the stop operation fails.
	 */
	public synchronized void stop() throws ServiceManagerException {
		LOG.info("Stopping server...");

		this.state = ServiceManagerState.STOPPING;
		// Wake up any waiting thread (e.g. in sleep() call)
		notifyAll();
		try {
			this.httpServer.get().shutdown(WAIT_TIMEOUT, TimeUnit.MILLISECONDS).get();
		} catch (ExecutionException | InterruptedException e) {
			throw new ServiceManagerException(e, "HTTP server shutdown failed");
		}
		this.state = ServiceManagerState.STOPPED;

		LOG.notice("Server has been stopped");
	}

	@Override
	public ServiceManagerState queryStatus() throws ServiceManagerException {
		return this.state;
	}

	@Override
	public void requestStop() throws ServiceManagerException {
		submitRequest(this::stop);
	}

	@Override
	public void deployService(String className, boolean start) throws ServiceManagerException {
		deployService(Optional.empty(), className, start);
	}

	@Override
	public void deployService(String moduleName, String className, boolean start) throws ServiceManagerException {
		deployService(Optional.of(moduleName), className, start);
	}

	private void deployService(Optional<String> moduleName, String className, boolean start)
			throws ServiceManagerException {
		String serviceName = ServiceStore.formatServiceName(moduleName, className);

		LOG.info("Deploying service ''{0}''", serviceName);
		try {
			this.serviceStore.registerService(moduleName, className, false);
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to register service ''{0}''", serviceName);
		}
		if (start) {
			startService(className);
		}
	}

	@Override
	public void startService(String className) throws ServiceManagerException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopService(String className) throws ServiceManagerException {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized void close() {
		LOG.info("Cleaning up server resources...");

		int pendingRequestCount = this.requests.size();

		if (pendingRequestCount > 0) {
			LOG.warning("Discarding {0} unprocessed server requests", pendingRequestCount);
			this.requests.clear();
		}
		this.httpServer.toOptional().ifPresent(HttpServer::shutdownNow);
	}

	@Override
	public String toString() {
		return "Server - " + this.configStore.getControlBaseUri();
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

			ResourceConfig controlResourceConfig = new ResourceConfig(ServiceManagerService.class)
					.addProperties(controlResourceConfigProperties);
			URI controlBaseUri = this.configStore.getControlBaseUri();
			SSLEngineConfigurator sslEngineConfigurator;
			boolean secure;

			if ("https".equals(controlBaseUri.getScheme())) {
				sslEngineConfigurator = setupSslEngineConfigurator();
				secure = true;
			} else {
				sslEngineConfigurator = null;
				secure = false;
			}

			HttpServer server = this.httpServer.set(GrizzlyHttpServerFactory.createHttpServer(controlBaseUri,
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

		try (CharSecret keyStoreSecret = this.secretsStore.decryptSecret(this.configStore.getSslKeyStoreSecret());
				InputStream keyStoreStream = Files.newInputStream(sslKeyStore)) {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			keyStore.load(keyStoreStream, keyStoreSecret.get());

			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());

			keyManagerFactory.init(keyStore, keyStoreSecret.get());

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

}
