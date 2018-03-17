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
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.jackson.JacksonFeature;

import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerClientFailureException;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInitializationFailureException;
import de.carne.lwjsd.api.ServiceManagerStartupFailureException;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.config.SecretsStore;
import de.carne.lwjsd.runtime.security.CharSecret;
import de.carne.lwjsd.runtime.ws.ServerStatus;
import de.carne.lwjsd.runtime.ws.ServiceManagerService;
import de.carne.lwjsd.runtime.ws.StatusMessage;
import de.carne.util.Debug;
import de.carne.util.Late;
import de.carne.util.logging.Log;

/**
 * The class runs the LWJSD client and makes the server's {@linkplain ServiceManager} interface locally accessible.
 */
public final class Client implements ServiceManager, AutoCloseable {

	private static final Log LOG = new Log();

	private final SecretsStore secretsStore;
	private final ConfigStore configStore;
	private final Late<javax.ws.rs.client.Client> restClient = new Late<>();

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
			this.secretsStore = SecretsStore.open(config);
			this.configStore = ConfigStore.open(config);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerInitializationFailureException("Failed to open required store", e);
		}
	}

	/**
	 * Establishes the connection to the server.
	 *
	 * @throws ServiceManagerException if the server connections fails.
	 */
	public void connect() throws ServiceManagerException {
		URI controlBaseUri = this.configStore.getControlBaseUri();

		LOG.info("Connecting to server at ''{0}''...", controlBaseUri);
		LOG.debug("Using {0}", this.configStore);

		ClientBuilder clientBuilder = ClientBuilder.newBuilder();

		if ("https".equals(controlBaseUri.getScheme())) {
			clientBuilder.sslContext(setupSslContext());
		}

		this.restClient.set(clientBuilder.register(JacksonFeature.class).build());
		ping();

		LOG.notice("Successfully connected to server ''{0}''", controlBaseUri);
	}

	private SSLContext setupSslContext() throws ServiceManagerException {
		Path sslKeyStore = this.configStore.getConfDir().resolve(this.configStore.getSslKeyStoreFile());

		LOG.info("Using SSL key store: ''{0}''", sslKeyStore);

		SSLContext sslContext;

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

			sslContext = SSLContext.getInstance(this.configStore.getSslProtocol());
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		} catch (IOException | GeneralSecurityException e) {
			throw new ServiceManagerStartupFailureException("Failed to setup SSL context", e);
		}
		return sslContext;
	}

	private void ping() throws ServiceManagerException {
		try {
			WebTarget pingTarget = this.restClient.get().target(this.configStore.getControlBaseUri())
					.path(ServiceManagerService.WEB_CONTEXT_PATH).path(ServiceManagerService.PING_PATH);

			checkRequestStatus(pingTarget.request().get().readEntity(StatusMessage.class));
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
	}

	@Override
	public ServiceManagerState queryStatus() throws ServiceManagerException {
		ServerStatus status;

		try {
			WebTarget queryStatusTarget = this.restClient.get().target(this.configStore.getControlBaseUri())
					.path(ServiceManagerService.WEB_CONTEXT_PATH).path(ServiceManagerService.REQUEST_QUERY_STATUS);

			status = checkRequestStatus(queryStatusTarget.request().get().readEntity(ServerStatus.class));
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
		return status.getServerState();
	}

	@Override
	public void requestStop() throws ServiceManagerException {
		try {
			WebTarget requestStopTarget = this.restClient.get().target(this.configStore.getControlBaseUri())
					.path(ServiceManagerService.WEB_CONTEXT_PATH).path(ServiceManagerService.REQUEST_STOP_PATH);

			checkRequestStatus(requestStopTarget.request().get().readEntity(StatusMessage.class));
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
	}

	@Override
	public void close() {
		URI controlBaseUri = this.configStore.getControlBaseUri();

		LOG.info("Closing connection to server ''{0}''...", controlBaseUri);

		this.restClient.toOptional().ifPresent(javax.ws.rs.client.Client::close);

		LOG.notice("Connection to server ''{0}'' has been closed", this.configStore.getControlBaseUri());
	}

	private <T extends StatusMessage> T checkRequestStatus(T requestStatus) throws ServiceManagerException {
		String status = requestStatus.getStatusMessage();

		if (!StatusMessage.OK.equals(status)) {
			String restCall = Debug.getCaller();

			throw new ServiceManagerClientFailureException("REST call " + restCall + " failed: " + status);
		} else if (LOG.isDebugLoggable()) {
			LOG.debug("REST call {0} (status: ''{1}'')", Debug.getCaller(), requestStatus.getStatusMessage());
		}

		return requestStatus;
	}

	private ServiceManagerException mapProcessingException(ProcessingException e) {
		String restCall = Debug.getCaller();
		URI controlBaseUri = this.configStore.getControlBaseUri();

		return new ServiceManagerClientFailureException(
				"REST call " + restCall + " to server '" + controlBaseUri + "' failed", e);
	}

}
