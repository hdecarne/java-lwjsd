/*
 * Copyright (c) 2018-2020 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.lwjsd.runtime.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.carne.boot.Exceptions;

/**
 * This class provides default configuration parameters used for client and server setup.
 */
public final class Defaults extends Config {

	private static final Defaults DEFAULTS = new Defaults();

	private final URI baseUri = getUriDefault(".baseUri", "https://localhost:5871");
	private final String sslProtocol = getStringDefault(".sslProtocol", "TLSv1.2");
	private final Path confDir = getPathDefault(".confDir", System.getProperty("user.home", "."), ".lwjsd");
	private final String sslKeyStoreFile = getStringDefault(".sslKeyStoreFile", "lwjsd.jks");
	private final String sslKeyStoreSecret = getStringDefault(".sslKeyStoreSecret", "");
	private final Path stateDir = getPathDefault(".confDir", System.getProperty("user.home", "."), ".lwjsd");

	private Defaults() {
		// Prevent instantiation
	}

	/**
	 * Gets the default {@linkplain Config} object containing the defaults for all configuration parameters.
	 *
	 * @return the default {@linkplain Config} object.
	 */
	public static Defaults get() {
		return DEFAULTS;
	}

	@Override
	public URI getBaseUri() {
		return this.baseUri;
	}

	@Override
	public String getSslProtocol() {
		return this.sslProtocol;
	}

	@Override
	public Path getConfDir() {
		return this.confDir;
	}

	@Override
	public String getSslKeyStoreFile() {
		return this.sslKeyStoreFile;
	}

	@Override
	public String getSslKeyStoreSecret() {
		return this.sslKeyStoreSecret;
	}

	@Override
	public Path getStateDir() {
		return this.stateDir;
	}

	private static String getStringDefault(String propertyKey, String defaultValue) {
		return System.getProperty(Defaults.class.getName() + propertyKey, defaultValue);
	}

	private static URI getUriDefault(String propertyKey, String defaultValue) {
		String uriString = System.getProperty(Defaults.class.getName() + propertyKey, defaultValue);
		URI uri;

		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			throw Exceptions.toRuntime(e);
		}
		return uri;
	}

	private static Path getPathDefault(String propertyKey, String defaultValueFirst, String defaultValueMore) {
		String pathString = System.getProperty(Defaults.class.getName() + propertyKey, defaultValueFirst);

		return (!pathString.equals(defaultValueFirst) ? Paths.get(pathString)
				: Paths.get(pathString, defaultValueMore));
	}

}
