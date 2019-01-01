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
package de.carne.lwjsd.runtime.security;

import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@linkplain CipherFactory} implementation providing AES256 cipher.
 */
public class AES256CipherFactory extends CipherFactory {

	private static final String KEY_FACTORY_ALG = "PBKDF2WithHmacSHA256";
	private static final String KEY_ALG = "AES";
	private static final String CIPHER_ALG = "AES/GCM/NoPadding";
	private static final int SALT_LENGTH = 8;
	private static final int IV_LENGTH = 12;
	private static final int GCM_TLEN = 128;

	/**
	 * Cipher name.
	 */
	public static final String CIPHER_NAME = "aes256-cipher";

	/**
	 * Constructs new {@linkplain AES256CipherFactory} instance.
	 */
	public AES256CipherFactory() {
		super(CIPHER_NAME);
	}

	@Override
	public Cipher createCipher() throws GeneralSecurityException {
		byte[] salt = new byte[SALT_LENGTH];

		getRandom().nextBytes(salt);

		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(KEY_FACTORY_ALG);
		KeySpec keySpec = new PBEKeySpec(null, salt, 65536, 256);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		SecretKeySpec secretKeySpec;

		try (ByteSecret encodedSecretKey = ByteSecret.wrap(secretKey.getEncoded())) {
			secretKeySpec = new SecretKeySpec(encodedSecretKey.get(), KEY_ALG);
		} finally {
			safeDestroy(secretKey);
		}
		return new AES256Cipher(salt, secretKeySpec);
	}

	@Override
	public Cipher createCipher(ByteSecret encoded) throws GeneralSecurityException {
		byte[] encodedBytes = encoded.get();
		byte[] salt = new byte[SALT_LENGTH];

		System.arraycopy(encodedBytes, 0, salt, 0, SALT_LENGTH);

		SecretKeySpec secretKeySpec = new SecretKeySpec(encodedBytes, SALT_LENGTH, encodedBytes.length - SALT_LENGTH,
				KEY_ALG);

		return new AES256Cipher(salt, secretKeySpec);
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

		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TLEN, iv);
		javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CIPHER_ALG);

		cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);

		byte[] encryptedData = cipher.doFinal(decrypted);
		byte[] encrypted = new byte[iv.length + encryptedData.length];

		System.arraycopy(iv, 0, encrypted, 0, iv.length);
		System.arraycopy(encryptedData, 0, encrypted, iv.length, encryptedData.length);
		return encrypted;
	}

	byte[] cipherDecrypt0(SecretKeySpec secretKeySpec, byte[] data) throws GeneralSecurityException {
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TLEN, data, 0, IV_LENGTH);
		javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CIPHER_ALG);

		cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
		return cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
	}

	void cipherClose0(SecretKeySpec secretKeySpec) {
		safeDestroy(secretKeySpec);
	}

	private class AES256Cipher extends Cipher {

		private final byte[] salt;
		private final SecretKeySpec secretKeySpec;

		AES256Cipher(byte[] salt, SecretKeySpec secretKeySpec) {
			this.salt = salt;
			this.secretKeySpec = secretKeySpec;
		}

		@Override
		public CipherFactory factory() {
			return AES256CipherFactory.this;
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
