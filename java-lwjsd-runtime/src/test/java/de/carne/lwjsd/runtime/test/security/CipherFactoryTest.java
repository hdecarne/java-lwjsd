/*
 * Copyright (c) 2018-2021 Holger de Carne and contributors, All Rights Reserved.
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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.runtime.security.AES256CipherFactory;
import de.carne.lwjsd.runtime.security.ByteSecret;
import de.carne.lwjsd.runtime.security.Cipher;
import de.carne.lwjsd.runtime.security.CipherFactory;

/**
 * Test {@linkplain CipherFactory} class.
 */
class CipherFactoryTest {

	@Test
	void testUnknownAlgorithm() {
		Assertions.assertThrows(NoSuchAlgorithmException.class, () -> {
			testCipherFactory("");
		});
		Assertions.assertThrows(NoSuchAlgorithmException.class, () -> {
			testCipherFactory("unknown");
		});
	}

	@Test
	void testCipherFactories() throws GeneralSecurityException {
		testCipherFactory(AES256CipherFactory.CIPHER_NAME);
	}

	private void testCipherFactory(String name) throws GeneralSecurityException {
		CipherFactory cipherFactory = CipherFactory.getInstance(name);

		Assertions.assertEquals(name, cipherFactory.name());

		Cipher cipherInstance1 = cipherFactory.createCipher();
		Cipher cipherInstance2;

		try (ByteSecret encodedCipherInstance1 = cipherInstance1.getEncoded()) {
			cipherInstance2 = cipherFactory.createCipher(encodedCipherInstance1);
		}

		byte[] source = getClass().getName().getBytes();
		byte[] encrypted1 = cipherInstance1.encrypt(source);
		byte[] encrypted2 = cipherInstance2.encrypt(source);

		Assertions.assertArrayEquals(source, cipherInstance1.decrypt(encrypted1));
		Assertions.assertArrayEquals(source, cipherInstance1.decrypt(encrypted2));
		Assertions.assertArrayEquals(source, cipherInstance2.decrypt(encrypted2));

		char[] charSource = "secret".toCharArray();
		byte[] encryptedChars = cipherInstance1.encryptChars(charSource);

		Assertions.assertArrayEquals(charSource, cipherInstance1.decryptChars(encryptedChars));
	}

}
