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
package de.carne.lwjsd.runtime.test.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.runtime.config.Defaults;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.security.Cipher;
import de.carne.lwjsd.runtime.security.SecretsStore;
import de.carne.nio.file.FileUtil;

/**
 * Test {@linkplain SecretsStore} class.
 */
class SecretsStoreTest {

	@Test
	void testSecretsStore() throws IOException, GeneralSecurityException {
		Path tempDir = Files.createTempDirectory(getClass().getName());
		RuntimeConfig config = new RuntimeConfig(Defaults.get());

		config.setConfDir(tempDir);
		config.setStateDir(tempDir);
		try {
			// Test new initialized store
			testSecretsStoreInstance(SecretsStore.create(config));
			// Test re-loaded store
			testSecretsStoreInstance(SecretsStore.create(config));
		} finally {
			FileUtil.delete(tempDir);
		}
	}

	void testSecretsStoreInstance(SecretsStore secretsStore) throws GeneralSecurityException {
		Cipher defaultCipher = secretsStore.getDefaultCipher();

		Assertions.assertNotNull(defaultCipher);

		Cipher namedCipher = secretsStore.getCipher(defaultCipher.name());

		Assertions.assertEquals(defaultCipher, namedCipher);
	}

}
