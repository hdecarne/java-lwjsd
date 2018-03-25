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
import java.util.Base64;

/**
 * Helper class for password encryption/decryption.
 */
public final class Passwords {

	private static final String SECRET_PREFIX = "secret:";

	private Passwords() {
		// prevent instantiation
	}

	/**
	 * Encrypts a character based passwords.
	 * <p>
	 * The given {@linkplain SecretsStore}'s default {@linkplain Cipher} is used for encryption.
	 *
	 * @param secretsStore The {@linkplain SecretsStore} providing the {@linkplain Cipher} to use for encryption.
	 * @param plainSecret the secret to encrypt.
	 * @return the encrypted secret.
	 * @throws GeneralSecurityException if a security error occurs during encryption.
	 */
	public static String encryptPassword(SecretsStore secretsStore, CharSecret plainSecret)
			throws GeneralSecurityException {
		Cipher cipher = secretsStore.getDefaultCipher();
		StringBuilder encryptedSecret = new StringBuilder();

		encryptedSecret.append(SECRET_PREFIX);
		encryptedSecret.append(cipher.name());
		encryptedSecret.append(":");
		encryptedSecret.append(Base64.getEncoder().encodeToString(cipher.encryptChars(plainSecret.get())));
		return encryptedSecret.toString();
	}

	/**
	 * Decrypts a character based password.
	 * <p>
	 * This function detects automatically which {@linkplain Cipher} has been applied to the password or if it is not
	 * encrypted at all.
	 *
	 * @param secretsStore The {@linkplain SecretsStore} providing the {@linkplain Cipher} to use for decryption.
	 * @param encryptedSecret the secret string (either plain or encrypted).
	 * @return the decrypted secret.
	 * @throws GeneralSecurityException if a security error occurs during decryption.
	 */
	public static CharSecret decryptPassword(SecretsStore secretsStore, String encryptedSecret)
			throws GeneralSecurityException {
		char[] plainSecret;

		if (encryptedSecret.startsWith(SECRET_PREFIX)) {
			int dataIndex = encryptedSecret.indexOf(':', SECRET_PREFIX.length() + 1);

			if (dataIndex <= SECRET_PREFIX.length()) {
				throw new IllegalArgumentException("Invalid secret: '" + encryptedSecret + "'");
			}

			String cipherName = encryptedSecret.substring(SECRET_PREFIX.length(), dataIndex);
			Cipher cipher = secretsStore.getCipher(cipherName);
			String data = encryptedSecret.substring(dataIndex + 1);

			plainSecret = cipher.decryptChars(Base64.getDecoder().decode(data));
		} else {
			plainSecret = encryptedSecret.toCharArray();
		}
		return CharSecret.wrap(plainSecret);
	}

}
