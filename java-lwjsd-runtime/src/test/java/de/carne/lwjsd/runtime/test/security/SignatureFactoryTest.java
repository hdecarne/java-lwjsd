/*
 * Copyright (c) 2018-2019 Holger de Carne and contributors, All Rights Reserved.
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
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.runtime.security.EC256SignatureFactory;
import de.carne.lwjsd.runtime.security.Signature;
import de.carne.lwjsd.runtime.security.SignatureFactory;

/**
 * Test {@linkplain SignatureFactory} class.
 */
class SignatureFactoryTest {

	@Test
	void testUnknownAlgorithm() {
		Assertions.assertThrows(NoSuchAlgorithmException.class, () -> {
			testSignatureFactory("");
		});
		Assertions.assertThrows(NoSuchAlgorithmException.class, () -> {
			testSignatureFactory("unknown");
		});
	}

	@Test
	void testSignatureFactories() throws IOException, GeneralSecurityException {
		testSignatureFactory(EC256SignatureFactory.SIGNATURE_NAME);
	}

	private void testSignatureFactory(String name) throws IOException, GeneralSecurityException {
		SignatureFactory signatureFactory = SignatureFactory.getInstance(name);

		Assertions.assertEquals(name, signatureFactory.name());

		Signature signatureInstance1 = signatureFactory.createSignature();
		byte[] signature1;

		try (InputStream data = getTestData1()) {
			signature1 = signatureInstance1.sign(data);
		}
		try (InputStream data = getTestData1()) {
			Assertions.assertTrue(signatureInstance1.verify(data, signature1));
		}
		try (InputStream data = getTestData2()) {
			Assertions.assertFalse(signatureInstance1.verify(data, signature1));
		}

		Signature signatureInstance2 = signatureFactory.createSignature(signatureInstance1.getEncoded());
		byte[] signature2;

		try (InputStream data = getTestData2()) {
			signature2 = signatureInstance2.sign(data);
		}
		try (InputStream data = getTestData2()) {
			Assertions.assertTrue(signatureInstance2.verify(data, signature2));
		}
		try (InputStream data = getTestData2()) {
			Assertions.assertFalse(signatureInstance2.verify(data, signature1));
		}
	}

	private InputStream getTestData1() {
		return getClass().getResourceAsStream(getClass().getSimpleName() + ".class");
	}

	private InputStream getTestData2() {
		return getClass().getResourceAsStream("package-info.class");
	}

}
