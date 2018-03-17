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
package de.carne.lwjsd.runtime.config;

import java.net.URI;
import java.nio.file.Path;

/**
 * Base class defining the global configuration options.
 */
public abstract class Config {

	/**
	 * Gets the base {@linkplain URI} to use for the LWJSD server control channel access.
	 *
	 * @return the base {@linkplain URI} to use for the LWJSD server control channel access.
	 */
	public abstract URI getControlBaseUri();

	/**
	 * Gets the SSL protocol to use for HTTPS connections.
	 *
	 * @return the SSL protocol to use for HTTPS connections.
	 * @see javax.net.ssl.SSLContext#getInstance(String)
	 */
	public abstract String getSslProtocol();

	/**
	 * Gets the {@linkplain Path} to use for storing config data.
	 *
	 * @return the {@linkplain Path} to use for storing config data.
	 */
	public abstract Path getConfDir();

	/**
	 * Gets the file name (relative to config data path) to use for storing SSL keys and certificates.
	 *
	 * @return the file name to use for storing SSL keys and certificates.
	 */
	public abstract String getSslKeyStoreFile();

	/**
	 * Gets the SSL key store secret.
	 * <p>
	 * The returned secret may be encrypted and has to decryped using the
	 * {@linkplain SecretsStore#decryptSecret(String)} function.
	 *
	 * @return the SSL key store secret or {@code ""} if none has been configured.
	 */
	public abstract String getSslKeyStoreSecret();

	/**
	 * Gets the {@linkplain Path} to use for storing state data.
	 *
	 * @return the {@linkplain Path} to use for storing state data.
	 */
	public abstract Path getStateDir();

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		buffer.append("config:").append(System.lineSeparator());
		buffer.append(" controlBaseUri = ").append(getControlBaseUri()).append(System.lineSeparator());
		buffer.append(" sslProtocol = ").append(getSslProtocol()).append(System.lineSeparator());
		buffer.append(" confDir = ").append(getConfDir()).append(System.lineSeparator());
		buffer.append("  sslKeyStoreFile = ").append(getSslKeyStoreFile()).append(System.lineSeparator());
		buffer.append("  sslKeyStorePass = <secret>").append(System.lineSeparator());
		buffer.append(" stateDir = ").append(getStateDir());
		return buffer.toString();
	}

}
