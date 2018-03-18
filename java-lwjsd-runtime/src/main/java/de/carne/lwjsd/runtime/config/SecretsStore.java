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
package de.carne.lwjsd.runtime.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.carne.check.Nullable;
import de.carne.lwjsd.runtime.security.AES128CipherFactory;
import de.carne.lwjsd.runtime.security.ByteSecret;
import de.carne.lwjsd.runtime.security.CharSecret;
import de.carne.lwjsd.runtime.security.Cipher;
import de.carne.lwjsd.runtime.security.CipherFactory;
import de.carne.nio.file.attribute.FileAttributes;
import de.carne.util.Strings;
import de.carne.util.logging.Log;

/**
 * This class is used to encrypt and decrypt security related data (e.g. passwords).
 */
public final class SecretsStore {

	private static final Log LOG = new Log();

	private static final String CIPHERS_FILE = "lwjsd.ciphers.xml";
	private static final String DEFAULT_CIPHER = AES128CipherFactory.CIPHER_NAME;
	private static final String SECRET_PREFIX = "secret:";

	private final Map<String, Cipher> cipherMap;

	private SecretsStore(Map<String, Cipher> cipherMap) {
		this.cipherMap = cipherMap;
	}

	/**
	 * Creates a new {@linkplain SecretsStore} instance by loading or (if not yet existing) creating the necessary
	 * security data.
	 *
	 * @param config The (sealed) {@linkplain Config} object to use for initialization.
	 * @return The created {@linkplain SecretsStore} instance.
	 * @throws IOException if an I/O error occurs while accessing the store data.
	 * @throws GeneralSecurityException if an security error occurs while accessing the store data.
	 */
	public static SecretsStore open(Config config) throws IOException, GeneralSecurityException {
		Path stateDir = config.getStateDir();

		Files.createDirectories(stateDir, FileAttributes.userDirectoryDefault(stateDir));

		Path ciphersFile = stateDir.resolve(CIPHERS_FILE);

		LOG.info("Using ciphers file ''{0}''...", ciphersFile);

		Properties ciphers = new Properties();
		boolean updateCiphersFile = false;

		if (Files.exists(ciphersFile)) {
			try (InputStream ciphersStream = Files.newInputStream(ciphersFile, StandardOpenOption.READ)) {
				ciphers.loadFromXML(ciphersStream);
			}
		} else {
			updateCiphersFile = true;
		}

		Map<String, Cipher> cipherMap = new HashMap<>(ciphers.size());

		for (Object cipherNameObject : ciphers.keySet()) {
			String cipherName = cipherNameObject.toString();

			try {
				CipherFactory cipherFactory = CipherFactory.getInstance(cipherName);
				Cipher cipher = decodeCipher(cipherFactory, ciphers.getProperty(cipherName));

				cipherMap.put(cipherName, cipher);
			} catch (GeneralSecurityException | IllegalArgumentException e) {
				LOG.warning(e, "Ignoring invalid cipher entry ''{0}''", cipherName);
			}
		}
		if (!cipherMap.containsKey(DEFAULT_CIPHER)) {
			CipherFactory defaultCipherFactory = CipherFactory.getInstance(DEFAULT_CIPHER);
			Cipher defaultCipher = defaultCipherFactory.createCipher();

			cipherMap.put(DEFAULT_CIPHER, defaultCipher);
			updateCiphersFile = true;
		}
		if (updateCiphersFile) {
			LOG.info("Creating/updating ciphers file ''{0}''...", ciphersFile);

			ciphers.clear();
			for (Map.Entry<String, Cipher> cipherMapEntry : cipherMap.entrySet()) {
				try (ByteSecret encodedCipher = cipherMapEntry.getValue().getEncoded()) {
					ciphers.setProperty(cipherMapEntry.getKey(),
							Base64.getEncoder().encodeToString(encodedCipher.get()));
				}
			}
			try (OutputStream ciphersStream = Files.newOutputStream(ciphersFile, StandardOpenOption.WRITE,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				ciphers.storeToXML(ciphersStream, null);
			}
			LOG.notice("Created/updated ciphers have been written to file ''{0}''...", ciphersFile);
		}
		return new SecretsStore(cipherMap);
	}

	/**
	 * Encrypts a character based secret using the default cipher.
	 *
	 * @param plainSecret the secret to encrypt.
	 * @return the encrypted secret.
	 * @throws GeneralSecurityException if a security error occurs during encryption.
	 */
	public String encryptSecret(CharSecret plainSecret) throws GeneralSecurityException {
		Cipher cipher = this.cipherMap.get(DEFAULT_CIPHER);
		StringBuilder encryptedSecret = new StringBuilder();

		encryptedSecret.append(SECRET_PREFIX);
		encryptedSecret.append(DEFAULT_CIPHER);
		encryptedSecret.append(":");
		encryptedSecret.append(Base64.getEncoder().encodeToString(cipher.encryptChars(plainSecret.get())));
		return encryptedSecret.toString();
	}

	/**
	 * Decrypts a character based secret.
	 * <p>
	 * This function detects automatically which encryption cipher has been applied to the secret or if it is not
	 * encrypted at all.
	 *
	 * @param encryptedSecret the secret string (either plain or encrypted).
	 * @return the decrypted secret.
	 * @throws GeneralSecurityException if a security error occurs during decryption.
	 */
	public CharSecret decryptSecret(String encryptedSecret) throws GeneralSecurityException {
		char[] plainSecret;

		if (encryptedSecret.startsWith(SECRET_PREFIX)) {
			int dataIndex = encryptedSecret.indexOf(':', SECRET_PREFIX.length() + 1);

			if (dataIndex <= SECRET_PREFIX.length()) {
				throw new IllegalArgumentException("Invalid secret: '" + encryptedSecret + "'");
			}

			String cipherName = encryptedSecret.substring(SECRET_PREFIX.length(), dataIndex);
			Cipher cipher = this.cipherMap.get(cipherName);

			if (cipher == null) {
				throw new NoSuchAlgorithmException("Unrecognized cipher: " + cipherName);
			}

			String data = encryptedSecret.substring(dataIndex + 1);

			plainSecret = cipher.decryptChars(Base64.getDecoder().decode(data));
		} else {
			plainSecret = encryptedSecret.toCharArray();
		}
		return CharSecret.wrap(plainSecret);
	}

	private static Cipher decodeCipher(CipherFactory cipherFactory, @Nullable String cipherData)
			throws GeneralSecurityException {
		if (Strings.isEmpty(cipherData)) {
			throw new IllegalArgumentException("No cipher data");
		}

		Cipher cipher;

		try (ByteSecret encodedCipher = ByteSecret.wrap(Base64.getDecoder().decode(cipherData))) {
			cipher = cipherFactory.createCipher(encodedCipher);
		}
		return cipher;
	}

}
