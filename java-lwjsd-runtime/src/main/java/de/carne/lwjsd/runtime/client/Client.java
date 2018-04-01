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
package de.carne.lwjsd.runtime.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import de.carne.check.Nullable;
import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ReasonMessage;
import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.security.CharSecret;
import de.carne.lwjsd.runtime.security.Passwords;
import de.carne.lwjsd.runtime.security.SecretsStore;
import de.carne.lwjsd.runtime.ws.ControlApi;
import de.carne.lwjsd.runtime.ws.ControlApiExceptionMapper;
import de.carne.lwjsd.runtime.ws.JsonModuleInfo;
import de.carne.lwjsd.runtime.ws.JsonReasonMessage;
import de.carne.lwjsd.runtime.ws.RegisterModuleMultiPartHandler;
import de.carne.util.Debug;
import de.carne.util.Late;
import de.carne.util.ManifestInfos;
import de.carne.util.logging.Log;

/**
 * The class provides remote access to the master server's {@linkplain ServiceManager} interface.
 */
public final class Client implements ServiceManager, AutoCloseable {

	private static final Log LOG = new Log();

	private final SecretsStore secretsStore;
	private final ConfigStore configStore;
	private final Late<javax.ws.rs.client.Client> controlApiClientHolder = new Late<>();
	private final Late<ControlApi> controlApiHolder = new Late<>();

	/**
	 * Constructs new {@linkplain Client} instance.
	 * <p>
	 * Invoke {@linkplain #connect()} to connect to the configured server instance.
	 *
	 * @param config the {@linkplain Config} instance to use.
	 * @throws ServiceManagerException if an initialization error occurs during client setup.
	 */
	public Client(Config config) throws ServiceManagerException {
		try {
			this.secretsStore = SecretsStore.create(config);
			this.configStore = ConfigStore.create(config);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerException(e, "Failed to open required store");
		}
	}

	/**
	 * Establishes the connection to the server.
	 *
	 * @throws ServiceManagerException if the server connections fails.
	 */
	public void connect() throws ServiceManagerException {
		URI baseUri = this.configStore.getBaseUri();

		LOG.info("Connecting to server at ''{0}''...", baseUri);
		LOG.debug("Using {0}", this.configStore);

		ClientBuilder clientBuilder = ClientBuilder.newBuilder().register(JacksonFeature.class)
				.register(MultiPartFeature.class);

		if ("https".equals(baseUri.getScheme())) {
			clientBuilder.sslContext(setupSslContext());
		}

		javax.ws.rs.client.Client controlApiClient = this.controlApiClientHolder.set(clientBuilder.build());

		this.controlApiHolder.set(WebResourceFactory.newResource(ControlApi.class, controlApiClient.target(baseUri)));

		String serverVersion = version();

		LOG.info("Server version: ''{0}''", serverVersion);

		if (!ManifestInfos.APPLICATION_VERSION.equals(serverVersion)) {
			throw new ServiceManagerException("Client/server version mismatch (expected: ''{0}''; actual: ''{1}''",
					ManifestInfos.APPLICATION_VERSION, serverVersion);
		}

		LOG.notice("Successfully connected to server ''{0}'' (version: ''{1}'')", baseUri, serverVersion);
	}

	private SSLContext setupSslContext() throws ServiceManagerException {
		Path sslKeyStore = this.configStore.getConfDir().resolve(this.configStore.getSslKeyStoreFile());

		LOG.info("Using SSL key store: ''{0}''", sslKeyStore);

		SSLContext sslContext;

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

			sslContext = SSLContext.getInstance(this.configStore.getSslProtocol());
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerException(e, "Failed to setup SSL context");
		}
		return sslContext;
	}

	private String version() throws ServiceManagerException {
		LOG.info("Querying server version: ''{0}''", this.configStore.getBaseUri());

		String version;

		try {
			version = this.controlApiHolder.get().getVersion();
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
		return version;
	}

	@Override
	public ServiceManagerInfo queryStatus() throws ServiceManagerException {
		LOG.info("Querying server status: ''{0}''", this.configStore.getBaseUri());

		ServiceManagerInfo serviceManagerInfo;

		try {
			serviceManagerInfo = this.controlApiHolder.get().queryStatus().toSource();
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
		return serviceManagerInfo;
	}

	@Override
	public void requestStop() throws ServiceManagerException {
		LOG.info("Requesting server stop: ''{0}''", this.configStore.getBaseUri());

		this.controlApiHolder.get().requestStop();
	}

	@Override
	public ModuleInfo registerModule(Path file, boolean force) throws ServiceManagerException {
		ModuleInfo status;

		try (FormDataMultiPart multiPart = new FormDataMultiPart()) {
			RegisterModuleMultiPartHandler.fromSource(multiPart, file, force);

			// The proxy client is not yet capable of handling multi-part params correctly.
			// Therefore we have to perform this call manually.
			String basePath = ControlApi.class.getAnnotation(javax.ws.rs.Path.class).value();
			Response response = this.controlApiClientHolder.get().target(this.configStore.getBaseUri()).path(basePath)
					.path("registerModule").request(MediaType.APPLICATION_JSON_TYPE)
					.post(Entity.entity(multiPart, multiPart.getMediaType()));

			status = processResponseStatus(response).readEntity(JsonModuleInfo.class).toSource();
		} catch (IOException e) {
			throw new ServiceManagerException(e, "Failed to access file ''{0}''", file);
		} catch (ProcessingException e) {
			throw mapControlApiException(e);
		}
		return status;
	}

	@Override
	public ModuleInfo loadModule(String moduleName) throws ServiceManagerException {
		ModuleInfo status;

		try {
			status = this.controlApiHolder.get().loadModule(moduleName).toSource();
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
		return status;
	}

	@Override
	public void deleteModule(String moduleName) throws ServiceManagerException {
		try {
			this.controlApiHolder.get().deleteModule(moduleName);
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
	}

	@Override
	public ServiceInfo registerService(String className) throws ServiceManagerException {
		ServiceInfo status;

		try {
			status = this.controlApiHolder.get().registerService(className).toSource();
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
		return status;
	}

	@Override
	public ServiceInfo startService(ServiceId serviceId, boolean autoStart) throws ServiceManagerException {
		ServiceInfo status;

		try {
			status = this.controlApiHolder.get()
					.startService(serviceId.moduleName(), serviceId.serviceName(), autoStart).toSource();
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
		return status;
	}

	@Override
	public ServiceInfo stopService(ServiceId serviceId) throws ServiceManagerException {
		ServiceInfo status;

		try {
			status = this.controlApiHolder.get().stopService(serviceId.moduleName(), serviceId.serviceName())
					.toSource();
		} catch (Exception e) {
			throw mapControlApiException(e);
		}
		return status;
	}

	@Override
	public void close() {
		URI baseUri = this.configStore.getBaseUri();

		LOG.info("Closing connection to server ''{0}''...", baseUri);

		this.controlApiClientHolder.toOptional().ifPresent(javax.ws.rs.client.Client::close);

		LOG.notice("Connection to server ''{0}'' has been closed", this.configStore.getBaseUri());
	}

	private Response processResponseStatus(Response response) throws ServiceManagerException {
		if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
			throw mapFailedResponse(Debug.getCaller(), response);
		}
		return response;
	}

	private ServiceManagerException mapControlApiException(Exception exception) {
		ServiceManagerException serviceManagerException;
		String caller = Debug.getCaller();

		if (exception instanceof WebApplicationException) {
			serviceManagerException = mapFailedResponse(caller, ((WebApplicationException) exception).getResponse());
		} else {
			serviceManagerException = restCallFailure(caller, exception);
		}
		return serviceManagerException;
	}

	private ServiceManagerException mapFailedResponse(String caller, Response response) {
		ServiceManagerException serviceManagerException;

		if (Boolean.parseBoolean(response.getHeaderString(ControlApiExceptionMapper.CONTROL_API_EXCEPTION_HEADER))) {
			ReasonMessage reasonMessage = response.readEntity(JsonReasonMessage.class).toSource();

			serviceManagerException = new ServiceManagerException(reasonMessage);
		} else {
			serviceManagerException = restCallFailure(caller, null);
		}
		return serviceManagerException;
	}

	private ServiceManagerException restCallFailure(String caller, @Nullable Throwable exception) {
		String messagePattern = "REST call {0} to master server ''{1}'' failed";
		URI baseUri = this.configStore.getBaseUri();

		return (exception != null ? new ServiceManagerException(exception, messagePattern, caller, baseUri)
				: new ServiceManagerException(messagePattern, caller, baseUri));
	}

}
