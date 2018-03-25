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
 * Modifiable {@linkplain Config} object.
 */
public final class RuntimeConfig extends Config {

	private URI baseUri;
	private String sslProtocol;
	private Path confDir;
	private String sslKeyStoreFile;
	private String sslKeyStoreSecret;
	private Path stateDir;

	/**
	 * Constructs new {@linkplain RuntimeConfig} instance.
	 *
	 * @param defaults the {@linkplain Config} object to use for initialization.
	 */
	public RuntimeConfig(Config defaults) {
		this.baseUri = defaults.getBaseUri();
		this.sslProtocol = defaults.getSslProtocol();
		this.confDir = defaults.getConfDir();
		this.sslKeyStoreFile = defaults.getSslKeyStoreFile();
		this.sslKeyStoreSecret = defaults.getSslKeyStoreSecret();
		this.stateDir = defaults.getStateDir();
	}

	@Override
	public URI getBaseUri() {
		return this.baseUri;
	}

	/**
	 * Set {@code baseUri} option.
	 *
	 * @param baseUri the new option value.
	 */
	public void setBaseUri(URI baseUri) {
		this.baseUri = baseUri;
	}

	@Override
	public String getSslProtocol() {
		return this.sslProtocol;
	}

	/**
	 * Set {@code sslProtocol} option.
	 *
	 * @param sslProtocol the new option value.
	 */
	public void setSslProtocol(String sslProtocol) {
		this.sslProtocol = sslProtocol;
	}

	@Override
	public Path getConfDir() {
		return this.confDir;
	}

	/**
	 * Set {@code confDir} option.
	 *
	 * @param confDir the new option value.
	 */
	public void setConfDir(Path confDir) {
		this.confDir = confDir;
	}

	@Override
	public String getSslKeyStoreFile() {
		return this.sslKeyStoreFile;
	}

	/**
	 * Set {@code sslKeyStoreFile} option.
	 *
	 * @param sslKeyStoreFile the new option value.
	 */
	public void setSslKeyStoreFile(String sslKeyStoreFile) {
		this.sslKeyStoreFile = sslKeyStoreFile;
	}

	@Override
	public String getSslKeyStoreSecret() {
		return this.sslKeyStoreSecret;
	}

	/**
	 * Set {@code sslKeyStoreSecret} option.
	 *
	 * @param sslKeyStoreSecret the new option value.
	 */
	public void setSslKeyStoreSecret(String sslKeyStoreSecret) {
		this.sslKeyStoreSecret = sslKeyStoreSecret;
	}

	@Override
	public Path getStateDir() {
		return this.stateDir;
	}

	/**
	 * Set {@code stateDir} option.
	 *
	 * @param stateDir the new option value.
	 */
	public void setStateDir(Path stateDir) {
		this.stateDir = stateDir;
	}

}
