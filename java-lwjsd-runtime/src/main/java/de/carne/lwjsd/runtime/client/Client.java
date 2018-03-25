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

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;

import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.security.CharSecret;
import de.carne.lwjsd.runtime.security.Passwords;
import de.carne.lwjsd.runtime.security.SecretsStore;
import de.carne.lwjsd.runtime.ws.ControlApi;
import de.carne.lwjsd.runtime.ws.JsonServiceId;
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

		ClientBuilder clientBuilder = ClientBuilder.newBuilder();

		if ("https".equals(baseUri.getScheme())) {
			clientBuilder.sslContext(setupSslContext());
		}
		clientBuilder.register(JacksonFeature.class);

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
		String version;

		try {
			version = this.controlApiHolder.get().version();
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
		return version;
	}

	@Override
	public ServiceManagerInfo queryStatus() throws ServiceManagerException {
		ServiceManagerInfo serviceManagerInfo;

		try {
			serviceManagerInfo = this.controlApiHolder.get().queryStatus().toSource();
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
		return serviceManagerInfo;
	}

	@Override
	public void requestStop() throws ServiceManagerException {
		this.controlApiHolder.get().requestStop();
	}

	@Override
	public String registerModule(String file, boolean overwrite) throws ServiceManagerException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void loadModule(String moduleName) throws ServiceManagerException {
		try {
			this.controlApiHolder.get().loadModule(moduleName);
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
	}

	@Override
	public void deleteModule(String moduleName) throws ServiceManagerException {
		try {
			this.controlApiHolder.get().deleteModule(moduleName);
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
	}

	@Override
	public ServiceId registerService(String className) throws ServiceManagerException {
		ServiceId serviceId;

		try {
			serviceId = this.controlApiHolder.get().registerService(className).toSource();
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
		return serviceId;
	}

	@Override
	public void startService(ServiceId serviceId, boolean autoStart) throws ServiceManagerException {
		try {
			this.controlApiHolder.get().startService(new JsonServiceId(serviceId), autoStart);
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
	}

	@Override
	public void stopService(ServiceId serviceId) throws ServiceManagerException {
		try {
			this.controlApiHolder.get().stopService(new JsonServiceId(serviceId));
		} catch (ProcessingException e) {
			throw mapProcessingException(e);
		}
	}

	@Override
	public void close() {
		URI baseUri = this.configStore.getBaseUri();

		LOG.info("Closing connection to server ''{0}''...", baseUri);

		this.controlApiClientHolder.toOptional().ifPresent(javax.ws.rs.client.Client::close);

		LOG.notice("Connection to server ''{0}'' has been closed", this.configStore.getBaseUri());
	}

	private ServiceManagerException mapProcessingException(ProcessingException e) {
		String restCall = Debug.getCaller();
		URI baseUri = this.configStore.getBaseUri();

		return new ServiceManagerException(e, "REST call {0} to master server ''{1}'' failed", restCall, baseUri);
	}

}
