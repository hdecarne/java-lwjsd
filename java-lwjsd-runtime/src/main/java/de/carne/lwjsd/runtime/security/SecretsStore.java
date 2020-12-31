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
package de.carne.lwjsd.runtime.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.carne.boot.logging.Log;
import de.carne.lwjsd.runtime.config.Config;
import de.carne.nio.file.attribute.FileAttributes;

/**
 * This manages a server's secrets used for encryption and decryption of passwords as well for signature creation and
 * verification.
 */
public final class SecretsStore {

	private static final Log LOG = new Log();

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);

	private static final String SECRETS_FILE = "lwjsd.secrets.json";

	private static final String DEFAULT_CIPHER = AES256CipherFactory.CIPHER_NAME;
	private static final String DEFAULT_SIGNATURE = EC256SignatureFactory.SIGNATURE_NAME;

	private final Map<String, Cipher> cipherMap = new HashMap<>();
	private final Map<String, Signature> signatureMap = new HashMap<>();

	private SecretsStore() {
		// Just to prevent outside instantiation
	}

	/**
	 * Creates a {@linkplain SecretsStore} instance by loading or (if not yet existing) generating the necessary
	 * security data.
	 *
	 * @param config The {@linkplain Config} object to use for initialization.
	 * @return The created {@linkplain SecretsStore} instance.
	 * @throws IOException if an I/O error occurs while accessing the store data.
	 * @throws GeneralSecurityException if an security error occurs while accessing the store data.
	 */
	public static SecretsStore create(Config config) throws IOException, GeneralSecurityException {
		Path stateDir = config.getStateDir();

		Files.createDirectories(stateDir, FileAttributes.userDirectoryDefault(stateDir));

		Path secretsFile = stateDir.resolve(SECRETS_FILE);

		LOG.info("Using secrets file ''{0}''...", secretsFile);

		SecretsStore secretsStore = new SecretsStore();

		if (Files.exists(secretsFile)) {
			JsonSecretsStore json = JSON_OBJECT_MAPPER.readValue(secretsFile.toFile(), JsonSecretsStore.class);

			for (Map.Entry<String, String> jsonCipherKeyEntry : json.getCipherKeys().entrySet()) {
				String cipherName = jsonCipherKeyEntry.getKey();
				String cipherKey = jsonCipherKeyEntry.getValue();
				Cipher cipher = decodeCipher(cipherName, cipherKey);

				secretsStore.cipherMap.put(cipherName, cipher);
			}
			for (Map.Entry<String, String> jsonSignatureKeyEntry : json.getSignatureKeys().entrySet()) {
				String signatureName = jsonSignatureKeyEntry.getKey();
				String signatureKey = jsonSignatureKeyEntry.getValue();
				Signature signature = decodeSignature(signatureName, signatureKey);

				secretsStore.signatureMap.put(signatureName, signature);
			}
		}

		boolean updateSecretsFile = false;

		if (!secretsStore.cipherMap.containsKey(DEFAULT_CIPHER)) {
			secretsStore.cipherMap.put(DEFAULT_CIPHER, createDefaultCipher());
			updateSecretsFile = true;
		}
		if (!secretsStore.signatureMap.containsKey(DEFAULT_SIGNATURE)) {
			secretsStore.signatureMap.put(DEFAULT_SIGNATURE, createDefaultSignature());
			updateSecretsFile = true;
		}

		if (updateSecretsFile) {
			LOG.info("Creating/updating secrets file ''{0}''...", secretsFile);

			Map<String, String> cipherKeys = new HashMap<>();

			for (Map.Entry<String, Cipher> cipherMapEntry : secretsStore.cipherMap.entrySet()) {
				cipherKeys.put(cipherMapEntry.getKey(), encodeCipher(cipherMapEntry.getValue()));
			}

			Map<String, String> signatureKeys = new HashMap<>();

			for (Map.Entry<String, Signature> signatureMapEntry : secretsStore.signatureMap.entrySet()) {
				signatureKeys.put(signatureMapEntry.getKey(), encodeSignature(signatureMapEntry.getValue()));
			}

			JsonSecretsStore json = new JsonSecretsStore(cipherKeys, signatureKeys);

			JSON_OBJECT_MAPPER.writeValue(secretsFile.toFile(), json);

			LOG.notice("Created/updated secrets have been written to file ''{0}''...", secretsFile);
		}
		return secretsStore;
	}

	/**
	 * Gets the default {@linkplain Cipher}.
	 *
	 * @return the default {@linkplain Cipher}.
	 * @throws NoSuchAlgorithmException if the requested cipher name is not known.
	 */
	public Cipher getDefaultCipher() throws NoSuchAlgorithmException {
		return getCipher(DEFAULT_CIPHER);
	}

	/**
	 * Gets a specific {@linkplain Cipher}.
	 *
	 * @param name the name of the {@linkplain Cipher} to retrieve.
	 * @return the found {@linkplain Cipher}.
	 * @throws NoSuchAlgorithmException if the requested cipher name is not known.
	 */
	public Cipher getCipher(String name) throws NoSuchAlgorithmException {
		Cipher cipher = this.cipherMap.get(name);

		if (cipher == null) {
			throw new NoSuchAlgorithmException("Unknown cipher: " + name);
		}
		return cipher;
	}

	/**
	 * Gets the default {@linkplain Signature}.
	 *
	 * @return the default {@linkplain Signature}.
	 * @throws NoSuchAlgorithmException if the requested signature name is not known.
	 */
	public Signature getDefaultSignature() throws NoSuchAlgorithmException {
		return getSignature(DEFAULT_SIGNATURE);
	}

	/**
	 * Gets a specific {@linkplain Signature}.
	 *
	 * @param name the name of the {@linkplain Signature} to retrieve.
	 * @return the found {@linkplain Signature}.
	 * @throws NoSuchAlgorithmException if the requested signature name is not known.
	 */
	public Signature getSignature(String name) throws NoSuchAlgorithmException {
		Signature signature = this.signatureMap.get(name);

		if (signature == null) {
			throw new NoSuchAlgorithmException("Unknown signature: " + name);
		}
		return signature;
	}

	private static Cipher createDefaultCipher() throws GeneralSecurityException {
		return CipherFactory.getInstance(DEFAULT_CIPHER).createCipher();
	}

	private static Signature createDefaultSignature() throws GeneralSecurityException {
		return SignatureFactory.getInstance(DEFAULT_SIGNATURE).createSignature();
	}

	private static String encodeCipher(Cipher cipher) {
		String encodedCipher;

		try (ByteSecret encodedCipherSecret = cipher.getEncoded()) {
			encodedCipher = Base64.getEncoder().encodeToString(encodedCipherSecret.get());
		}
		return encodedCipher;
	}

	private static String encodeSignature(Signature signature) {
		String encodedSignature;

		try (ByteSecret encodedSignatureSecret = signature.getEncoded()) {
			encodedSignature = Base64.getEncoder().encodeToString(encodedSignatureSecret.get());
		}
		return encodedSignature;
	}

	private static Cipher decodeCipher(String cipherName, String cipherKey) throws GeneralSecurityException {
		CipherFactory cipherFactory = CipherFactory.getInstance(cipherName);

		Cipher cipher;

		try (ByteSecret encodedCipher = ByteSecret.wrap(Base64.getDecoder().decode(cipherKey))) {
			cipher = cipherFactory.createCipher(encodedCipher);
		}
		return cipher;
	}

	private static Signature decodeSignature(String signatureName, String signatureKey)
			throws GeneralSecurityException {
		SignatureFactory signatureFactory = SignatureFactory.getInstance(signatureName);

		Signature signature;

		try (ByteSecret encodedSignature = ByteSecret.wrap(Base64.getDecoder().decode(signatureKey))) {
			signature = signatureFactory.createSignature(encodedSignature);
		}
		return signature;
	}

	private static final class JsonSecretsStore {

		@Nullable
		private Map<String, String> cipherKeys;
		@Nullable
		private Map<String, String> signatureKeys;

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public JsonSecretsStore() {
			// Nothing to do here
		}

		public JsonSecretsStore(Map<String, String> cipherKeys, Map<String, String> signatureKeys) {
			this.cipherKeys = cipherKeys;
			this.signatureKeys = signatureKeys;
		}

		public Map<String, String> getCipherKeys() {
			return Collections.unmodifiableMap(Objects.requireNonNull(this.cipherKeys));
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setCipherKeys(Map<String, String> cipherKeys) {
			this.cipherKeys = cipherKeys;
		}

		public Map<String, String> getSignatureKeys() {
			return Collections.unmodifiableMap(Objects.requireNonNull(this.signatureKeys));
		}

		// Implicitly used by ObjectMapper
		@SuppressWarnings("unused")
		public void setSignatureKeys(Map<String, String> signatureKeys) {
			this.signatureKeys = signatureKeys;
		}

	}

}
