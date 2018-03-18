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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

import de.carne.util.Exceptions;
import de.carne.util.Strings;
import de.carne.util.logging.Log;

/**
 * This class provides access to all necessary configuration data used during client and server execution.
 */
public final class ConfigStore extends Config {

	private static final Log LOG = new Log();

	/**
	 * The {@linkplain ConfigStore} file name.
	 */
	public static final String CONFIG_FILE = "lwjsd.conf";

	private final URIConfigStoreOption controlBaseUri;
	private final StringConfigStoreOption sslProtocol;
	private final PathConfigStoreOption confDir;
	private final StringConfigStoreOption sslKeyStoreFile;
	private final StringConfigStoreOption sslKeyStoreSecret;
	private final PathConfigStoreOption stateDir;
	private final Map<String, ConfigStoreOption> optionMap = new LinkedHashMap<>();

	private ConfigStore(Config config) {
		this.controlBaseUri = new URIConfigStoreOption("controlBaseUri", true, config.getControlBaseUri());
		this.sslProtocol = new StringConfigStoreOption("sslProtocol", true, config.getSslProtocol());
		this.confDir = new PathConfigStoreOption("confDir", false, config.getConfDir());
		this.sslKeyStoreFile = new StringConfigStoreOption("sslKeyStoreFile", true, config.getSslKeyStoreFile());
		this.sslKeyStoreSecret = new StringConfigStoreOption("sslKeyStoreSecret", true, config.getSslKeyStoreSecret());
		this.stateDir = new PathConfigStoreOption("stateDir", false, config.getStateDir());
		this.optionMap.put(this.controlBaseUri.name(), this.controlBaseUri);
		this.optionMap.put(this.sslProtocol.name(), this.sslProtocol);
		this.optionMap.put(this.confDir.name(), this.confDir);
		this.optionMap.put(this.sslKeyStoreFile.name(), this.sslKeyStoreFile);
		this.optionMap.put(this.sslKeyStoreSecret.name(), this.sslKeyStoreSecret);
		this.optionMap.put(this.stateDir.name(), this.stateDir);
	}

	/**
	 * Creates a new {@linkplain ConfigStore} instance.
	 *
	 * @param config the {@linkplain Config} object to use for store initialization.
	 * @return the created {@linkplain ConfigStore} instance.
	 * @throws IOException if an I/O error occurs while accessing the store data.
	 */
	public static ConfigStore open(Config config) throws IOException {
		ConfigStore configStore = new ConfigStore(config);

		configStore.loadConfigFile();
		return configStore;
	}

	private void loadConfigFile() throws IOException {
		Path configFile = this.confDir.get().resolve(CONFIG_FILE);

		LOG.info("Using config file ''{0}''", configFile);

		if (Files.exists(configFile)) {
			try (BufferedReader configReader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
				String configLine;

				while ((configLine = configReader.readLine()) != null) {
					configLine = configLine.trim();
					if (!Strings.isEmpty(configLine) && !configLine.startsWith("#")) {
						loadConfigLine(configLine);
					}
				}
			}
		} else {
			LOG.info("Ignoring non-existent config file ''{0}''", configFile);
		}
	}

	private void loadConfigLine(String configLine) {
		boolean configLineProcessed = false;
		String[] keyValue = Strings.split(configLine, '=', false);

		if (keyValue.length == 2) {
			String key = Strings.safeTrim(keyValue[0]);
			String value = Strings.safeTrim(keyValue[1]);
			ConfigStoreOption option = this.optionMap.get(key);

			if (option != null && option.isPersistent()) {
				try {
					option.loadFromString(Strings.decode(value));
					configLineProcessed = true;
				} catch (IllegalArgumentException e) {
					Exceptions.ignore(e);
				}
			}
		}
		if (!configLineProcessed) {
			LOG.warning("Ignoring unrecognized or invalid configuration ''{0}''", configLine);
		}
	}

	/**
	 * Store this {@linkplain ConfigStore} instance's configuration information into a file.
	 * <p>
	 * If the file does not yet exist, it will be created. If the file already exists, it will be overwritten.
	 *
	 * @param file the file to write to.
	 * @throws IOException if an I/O error occurs during file access.
	 */
	public void storeConfigFile(Path file) throws IOException {
		try (BufferedWriter out = Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			out.write("#");
			out.newLine();
			out.write("# LWJSD config file");
			out.newLine();
			out.write("#");
			out.newLine();
			for (ConfigStoreOption option : this.optionMap.values()) {
				if (option.isPersistent()) {
					out.newLine();
					out.write("#");
					out.newLine();
					out.write("# ");
					out.write(option.name());
					out.write(": ");
					out.write(option.description());
					out.newLine();
					out.write("#");
					out.newLine();
					if (!option.isModified()) {
						out.write("#");
					}
					out.write(option.name());
					out.write(" = ");
					out.write(Strings.encode(option.toString()));
					out.newLine();
				}
			}
		}
	}

	@Override
	public URI getControlBaseUri() {
		return this.controlBaseUri.get();
	}

	/**
	 * Sets {@code controlBaseUri} option.
	 *
	 * @param controlBaseUri the new option value.
	 */
	public void setControlBaseUri(URI controlBaseUri) {
		this.controlBaseUri.accept(controlBaseUri);
	}

	@Override
	public String getSslProtocol() {
		return this.sslProtocol.get();
	}

	/**
	 * Sets {@code sslProtocol} option.
	 *
	 * @param sslProtocol the new option value.
	 */
	public void setSslProtocol(String sslProtocol) {
		this.sslProtocol.accept(sslProtocol);
	}

	@Override
	public Path getConfDir() {
		return this.confDir.get();
	}

	/**
	 * Sets {@code confDir} option.
	 *
	 * @param confDir the new option value.
	 */
	public void setConfDir(Path confDir) {
		this.confDir.accept(confDir);
	}

	@Override
	public String getSslKeyStoreFile() {
		return this.sslKeyStoreFile.get();
	}

	/**
	 * Sets {@code sslKeyStoreFile} option.
	 *
	 * @param sslKeyStoreFile the new option value.
	 */
	public void setSslKeyStoreFile(String sslKeyStoreFile) {
		this.sslKeyStoreFile.accept(sslKeyStoreFile);
	}

	@Override
	public String getSslKeyStoreSecret() {
		return this.sslKeyStoreSecret.get();
	}

	/**
	 * Sets {@code sslKeyStoreSecret} option.
	 *
	 * @param sslKeyStoreSecret the new option value.
	 */
	public void setSslKeyStoreSecret(String sslKeyStoreSecret) {
		this.sslKeyStoreSecret.accept(sslKeyStoreSecret);
	}

	@Override
	public Path getStateDir() {
		return this.stateDir.get();
	}

	/**
	 * Sets {@code stateDir} option.
	 *
	 * @param stateDir the new option value.
	 */
	public void setStateDir(Path stateDir) {
		this.stateDir.accept(stateDir);
	}

}
