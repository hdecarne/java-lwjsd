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
package de.carne.lwjsd.runtime.security;

import java.security.GeneralSecurityException;

/**
 * Base class for all kind of ciphers.
 */
public abstract class Cipher extends Secret {

	@Override
	public final SecretsProvider provider() {
		return factory();
	}

	/**
	 * Gets the {@linkplain CipherFactory} used to create this {@linkplain Cipher}.
	 *
	 * @return the {@linkplain CipherFactory} used to create this {@linkplain Cipher}.
	 */
	public abstract CipherFactory factory();

	/**
	 * Encrypts data bytes.
	 *
	 * @param data the data bytes to encrypt.
	 * @return the encrypted data.
	 * @throws GeneralSecurityException if an security error occurs.
	 */
	public abstract byte[] encrypt(byte[] data) throws GeneralSecurityException;

	/**
	 * Encrypted character data.
	 *
	 * @param chars the character data to encrypt.
	 * @return the encrypted data.
	 * @throws GeneralSecurityException if an security error occurs.
	 * @see #encrypt(byte[])
	 */
	public byte[] encryptChars(char[] chars) throws GeneralSecurityException {
		byte[] encrypted;

		try (ByteSecret data = ByteSecret.wrap(new byte[chars.length * 2])) {
			byte[] dataBytes = data.get();

			for (int charIndex = 0; charIndex < chars.length; charIndex++) {
				char c = chars[charIndex];

				dataBytes[charIndex * 2] = (byte) (c & 0xff);
				dataBytes[charIndex * 2 + 1] = (byte) ((c >> 8) & 0xff);
			}
			encrypted = encrypt(data.get());
		}
		return encrypted;
	}

	/**
	 * Decrypts encrypted data bytes.
	 *
	 * @param encrypted the encrypted data bytes to decrypt.
	 * @return the decrypted data.
	 * @throws GeneralSecurityException if an security error occurs.
	 */
	public abstract byte[] decrypt(byte[] encrypted) throws GeneralSecurityException;

	/**
	 * Decrypts encrypted character data.
	 *
	 * @param encrypted the encrypted character data to decrypt.
	 * @return the decrypted data.
	 * @throws GeneralSecurityException if an security error occurs.
	 * @see #decrypt(byte[])
	 */
	public char[] decryptChars(byte[] encrypted) throws GeneralSecurityException {
		char[] decrypted;

		try (ByteSecret data = ByteSecret.wrap(decrypt(encrypted))) {
			byte[] dataBytes = data.get();

			decrypted = new char[dataBytes.length >> 1];
			for (int dataByteIndex = 0; dataByteIndex < dataBytes.length; dataByteIndex += 2) {
				decrypted[dataByteIndex >> 1] = (char) ((dataBytes[dataByteIndex] & 0xff)
						| ((dataBytes[dataByteIndex + 1] & 0xff) << 8));
			}
		}
		return decrypted;
	}

}
