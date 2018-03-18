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
package de.carne.lwjsd.runtime.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.carne.io.IOUtil;
import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.config.Defaults;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.nio.file.FileUtil;

/**
 * Helper class providing a test {@linkplain RuntimeConfig} instance suitable for testing.
 */
public final class TestConfig {

	private TestConfig() {
		// Prevent instantation
	}

	/**
	 * Prepares a new {@linkplain RuntimeConfig} instance for a test.
	 *
	 * @return the prepared {@linkplain RuntimeConfig} instance.
	 * @throws IOException if an I/O error occurs.
	 */
	public static RuntimeConfig prepareConfig() throws IOException {
		Path tempDir = Files.createTempDirectory(TestConfig.class.getName());

		RuntimeConfig config = new RuntimeConfig(Defaults.get());

		config.setConfDir(tempDir);
		config.setStateDir(tempDir);
		config.setSslKeyStoreFile("localhost.jks");
		config.setSslKeyStoreSecret("secret");
		try (InputStream sslKeyStoreStream = TestConfig.class.getResourceAsStream(config.getSslKeyStoreFile())) {
			IOUtil.copyStream(config.getConfDir().resolve(config.getSslKeyStoreFile()).toFile(), sslKeyStoreStream);
		}

		ConfigStore configStore = ConfigStore.open(config);

		configStore.storeConfigFile(config.getConfDir().resolve(ConfigStore.CONFIG_FILE));
		System.out.println("Using test " + config);
		return config;
	}

	/**
	 * Discards any data related to a previously created and used {@linkplain RuntimeConfig} instance.
	 *
	 * @param config the {@linkplain RuntimeConfig} instance to discard.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void discardConfig(RuntimeConfig config) throws IOException {
		FileUtil.delete(config.getConfDir());
	}

}
