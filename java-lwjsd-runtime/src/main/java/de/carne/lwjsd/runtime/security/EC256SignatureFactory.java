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
package de.carne.lwjsd.runtime.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import de.carne.boot.check.Check;

/**
 * {@linkplain SignatureFactory} implementation providing EC256 signature.
 */
public class EC256SignatureFactory extends SignatureFactory {

	private static final String KEY_PAIR_ALG = "EC";
	private static final int KEY_SIZE = 256;
	private static final String SIGNATURE_ALG = "SHA256withECDSA";

	/**
	 * Signature name.
	 */
	public static final String SIGNATURE_NAME = "ec256-signature";

	/**
	 * Constructs new {@linkplain EC256SignatureFactory} instance.
	 */
	public EC256SignatureFactory() {
		super(SIGNATURE_NAME);
	}

	@Override
	public Signature createSignature() throws GeneralSecurityException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALG);

		keyPairGenerator.initialize(KEY_SIZE, getRandom());

		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		return new EC256Signature(keyPair);
	}

	@Override
	public Signature createSignature(ByteSecret encoded) throws GeneralSecurityException {
		byte[] encodedBytes = encoded.get();

		Check.assertTrue(encodedBytes.length > 4);

		int encodedPrivateLength = (encodedBytes[0] & 0xff) | ((encodedBytes[1] & 0xff) << 8)
				| ((encodedBytes[3] & 0xff) << 16) | ((encodedBytes[3] & 0xff) << 24);

		Check.assertTrue(encodedBytes.length > 4 + encodedPrivateLength);

		KeyFactory keyFactory = KeyFactory.getInstance(KEY_PAIR_ALG);
		int encodedPublicLength = encodedBytes.length - (4 + encodedPrivateLength);
		byte[] encodedPrivateBytes = new byte[encodedPrivateLength];
		byte[] encodedPublicBytes = new byte[encodedPublicLength];
		KeySpec privateKeySpec;
		KeySpec publicKeySpec;

		try (ByteSecret encodedPrivate = ByteSecret.wrap(encodedPrivateBytes);
				ByteSecret encodedPublic = ByteSecret.wrap(encodedPublicBytes)) {
			System.arraycopy(encodedBytes, 4, encodedPrivateBytes, 0, encodedPrivateLength);
			System.arraycopy(encodedBytes, 4 + encodedPrivateLength, encodedPublicBytes, 0, encodedPublicLength);

			privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateBytes);
			publicKeySpec = new X509EncodedKeySpec(encodedPublicBytes);
		}

		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

		return new EC256Signature(new KeyPair(publicKey, privateKey));
	}

	ByteSecret signatureGetEncoded0(KeyPair keyPair) {
		byte[] encoded;

		try (ByteSecret encodedPrivate = ByteSecret.wrap(keyPair.getPrivate().getEncoded());
				ByteSecret encodedPublic = ByteSecret.wrap(keyPair.getPublic().getEncoded())) {
			int encodedPrivateLength = encodedPrivate.length();
			int encodedPublicLength = encodedPublic.length();
			int encodedLength = 4 + encodedPrivateLength + encodedPublicLength;

			encoded = new byte[encodedLength];
			encoded[0] = (byte) (encodedPrivateLength & 0xff);
			encoded[1] = (byte) ((encodedPrivateLength >>> 8) & 0xff);
			encoded[2] = (byte) ((encodedPrivateLength >>> 16) & 0xff);
			encoded[3] = (byte) ((encodedPrivateLength >>> 24) & 0xff);
			System.arraycopy(encodedPrivate.get(), 0, encoded, 4, encodedPrivateLength);
			System.arraycopy(encodedPublic.get(), 0, encoded, 4 + encodedPrivateLength, encodedPublicLength);
		}
		return ByteSecret.wrap(encoded);
	}

	byte[] signatureSign0(KeyPair keyPair, InputStream data) throws IOException, GeneralSecurityException {
		java.security.Signature signature = java.security.Signature.getInstance(SIGNATURE_ALG);

		signature.initSign(keyPair.getPrivate(), getRandom());

		byte[] buffer = new byte[4096];
		int read;

		while ((read = data.read(buffer)) >= 0) {
			signature.update(buffer, 0, read);
		}
		return signature.sign();
	}

	boolean signatureVerify0(KeyPair keyPair, InputStream data, byte[] signatureBytes)
			throws IOException, GeneralSecurityException {
		java.security.Signature signature = java.security.Signature.getInstance(SIGNATURE_ALG);

		signature.initVerify(keyPair.getPublic());

		byte[] buffer = new byte[4096];
		int read;

		while ((read = data.read(buffer)) >= 0) {
			signature.update(buffer, 0, read);
		}
		return signature.verify(signatureBytes);
	}

	void signatureClose0(KeyPair keyPair) {
		safeDestroy(keyPair.getPrivate());
	}

	private class EC256Signature extends Signature {

		private final KeyPair keyPair;

		EC256Signature(KeyPair keyPair) {
			this.keyPair = keyPair;
		}

		@Override
		public SignatureFactory factory() {
			return EC256SignatureFactory.this;
		}

		@Override
		public ByteSecret getEncoded() {
			return signatureGetEncoded0(this.keyPair);
		}

		@Override
		public byte[] sign(InputStream data) throws IOException, GeneralSecurityException {
			return signatureSign0(this.keyPair, data);
		}

		@Override
		public boolean verify(InputStream data, byte[] signatureBytes) throws IOException, GeneralSecurityException {
			return signatureVerify0(this.keyPair, data, signatureBytes);
		}

		@Override
		public void close() throws Exception {
			signatureClose0(this.keyPair);
		}

	}

}
