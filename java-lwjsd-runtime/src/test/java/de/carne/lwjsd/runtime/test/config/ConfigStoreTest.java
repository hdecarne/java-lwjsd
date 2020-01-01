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
package de.carne.lwjsd.runtime.test.config;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.runtime.config.ConfigStore;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.test.TestConfig;

/**
 * Test {@linkplain ConfigStore} class.
 */
class ConfigStoreTest {

	@Test
	void testConfigStore() throws IOException, URISyntaxException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try {
			ConfigStore configStore1 = ConfigStore.create(config);
			Path referenceFile = configStore1.getConfDir().resolve(ConfigStore.CONFIG_FILE);
			Path testFile = configStore1.getConfDir().resolve("test1." + ConfigStore.CONFIG_FILE);

			configStore1.storeConfigFile(testFile);

			Assertions.assertArrayEquals(new String[] {}, diffFiles(referenceFile, testFile));

			configStore1.setConfDir(config.getConfDir().resolve("testconf"));
			configStore1.setStateDir(config.getStateDir().resolve("teststate"));
			configStore1.storeConfigFile(testFile);

			Assertions.assertArrayEquals(new String[] {}, diffFiles(referenceFile, testFile));

			configStore1.setConfDir(config.getConfDir());
			configStore1.setStateDir(config.getStateDir());
			configStore1.setBaseUri(new URI("https://lwjsd.localhost"));
			configStore1.setSslProtocol("TLS");
			configStore1.setSslKeyStoreFile("lwjsd.localhost.jks");
			configStore1.setSslKeyStoreSecret("terces");
			configStore1.storeConfigFile(testFile);

			Assertions.assertArrayEquals(new String[] {

					"baseUri = https://lwjsd.localhost",

					"sslProtocol = TLS",

					"sslKeyStoreFile = lwjsd.localhost.jks",

					"sslKeyStoreSecret = terces"

			}, diffFiles(referenceFile, testFile));

			Files.copy(testFile, referenceFile, StandardCopyOption.REPLACE_EXISTING);

			ConfigStore configStore2 = ConfigStore.create(config);

			configStore2.storeConfigFile(testFile);

			Assertions.assertArrayEquals(new String[] {}, diffFiles(referenceFile, testFile));
		} finally {
			TestConfig.discardConfig(config);
		}
	}

	private String[] diffFiles(Path reference, Path test) throws IOException {
		List<String> referenceLines = Files.readAllLines(reference);
		List<String> testLines = Files.readAllLines(test);
		List<String> differences = new ArrayList<>();
		int referenceIndex = 0;

		for (String testLine : testLines) {
			if (referenceIndex < referenceLines.size() && !testLine.equals(referenceLines.get(referenceIndex))) {
				differences.add(testLine);
			}
			referenceIndex++;
		}
		return differences.toArray(new String[differences.size()]);
	}

}
