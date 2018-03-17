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

import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@linkplain CipherFactory} implementation for AES128 cipher.
 */
public class AES128CipherFactory extends CipherFactory {

	private static final String KEY_ALGORITHM = "AES";
	private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final int SALT_LENGTH = 8;
	private static final int IV_LENGTH = 16;

	/**
	 * Cipher name.
	 */
	public static final String CIPHER_NAME = "AES128";

	/**
	 * Constructs new {@linkplain AES128CipherFactory} instance.
	 */
	public AES128CipherFactory() {
		super(CIPHER_NAME);
	}

	@Override
	public Cipher createCipher() throws GeneralSecurityException {
		byte[] salt = new byte[SALT_LENGTH];

		getRandom().nextBytes(salt);

		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec keySpec = new PBEKeySpec(null, salt, 10000, 128);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		SecretKeySpec secretKeySpec;

		try (ByteSecret encodedSecretKey = ByteSecret.wrap(secretKey.getEncoded())) {
			secretKeySpec = new SecretKeySpec(encodedSecretKey.get(), KEY_ALGORITHM);
		} finally {
			safeDestroy(secretKey);
		}
		return new AES128Cipher(salt, secretKeySpec);
	}

	@Override
	public Cipher createCipher(ByteSecret encoded) throws GeneralSecurityException {
		byte[] encodedBytes = encoded.get();
		byte[] salt = new byte[SALT_LENGTH];

		System.arraycopy(encodedBytes, 0, salt, 0, SALT_LENGTH);

		SecretKeySpec secretKeySpec = new SecretKeySpec(encodedBytes, SALT_LENGTH, encodedBytes.length - SALT_LENGTH,
				KEY_ALGORITHM);

		return new AES128Cipher(salt, secretKeySpec);
	}

	ByteSecret cipherGetEncoded0(byte[] salt, SecretKeySpec secretKeySpec) {
		byte[] encoded;

		try (ByteSecret encodedSecretKey = ByteSecret.wrap(secretKeySpec.getEncoded())) {
			encoded = new byte[salt.length + encodedSecretKey.length()];
			System.arraycopy(salt, 0, encoded, 0, salt.length);
			System.arraycopy(encodedSecretKey.get(), 0, encoded, salt.length, encodedSecretKey.length());
		}
		return ByteSecret.wrap(encoded);
	}

	byte[] cipherEncrypt0(SecretKeySpec secretKeySpec, byte[] decrypted) throws GeneralSecurityException {
		byte[] iv = new byte[IV_LENGTH];

		getRandom().nextBytes(iv);

		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CIPHER_ALGORITHM);

		cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

		byte[] encryptedData = cipher.doFinal(decrypted);
		byte[] encrypted = new byte[iv.length + encryptedData.length];

		System.arraycopy(iv, 0, encrypted, 0, iv.length);
		System.arraycopy(encryptedData, 0, encrypted, iv.length, encryptedData.length);
		return encrypted;
	}

	byte[] cipherDecrypt0(SecretKeySpec secretKeySpec, byte[] data) throws GeneralSecurityException {
		IvParameterSpec ivParameterSpec = new IvParameterSpec(data, 0, IV_LENGTH);
		javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CIPHER_ALGORITHM);

		cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
		return cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
	}

	void cipherClose0(SecretKeySpec secretKeySpec) {
		safeDestroy(secretKeySpec);
	}

	private class AES128Cipher extends Cipher {

		private final byte[] salt;
		private final SecretKeySpec secretKeySpec;

		AES128Cipher(byte[] salt, SecretKeySpec secretKeySpec) {
			this.salt = salt;
			this.secretKeySpec = secretKeySpec;
		}

		@Override
		public CipherFactory factory() {
			return AES128CipherFactory.this;
		}

		@Override
		public ByteSecret getEncoded() {
			return cipherGetEncoded0(this.salt, this.secretKeySpec);
		}

		@Override
		public byte[] encrypt(byte[] data) throws GeneralSecurityException {
			return cipherEncrypt0(this.secretKeySpec, data);
		}

		@Override
		public byte[] decrypt(byte[] encrypted) throws GeneralSecurityException {
			return cipherDecrypt0(this.secretKeySpec, encrypted);
		}

		@Override
		public void close() {
			cipherClose0(this.secretKeySpec);
		}

	}

}
