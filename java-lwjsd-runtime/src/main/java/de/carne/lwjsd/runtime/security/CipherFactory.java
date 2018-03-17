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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ServiceLoader;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import de.carne.util.logging.Log;

/**
 * Base class for all kinds of cipher factories.
 * <p>
 * Cipher factories are used to create and initialize cipher instances suitable for encryption and decryption of data.
 */
public abstract class CipherFactory {

	private static final Log LOG = new Log();

	private final String name;

	/**
	 * Constructs new {@linkplain CipherFactory} instance.
	 *
	 * @param name the cipher name uniquely identifying this factory.
	 */
	protected CipherFactory(String name) {
		this.name = name;
	}

	/**
	 * Gets a {@linkplain SecureRandom} instance for generation of random data.
	 *
	 * @return a {@linkplain SecureRandom} instance for generation of random data.
	 */
	protected static SecureRandom getRandom() {
		return Randomness.get();
	}

	/**
	 * Destroys a {@linkplain Destroyable} object and handles a possible {@linkplain DestroyFailedException} by issuing
	 * an error log statement.
	 *
	 * @param destroyable the object to destroy.
	 */
	protected void safeDestroy(Destroyable destroyable) {
		try {
			destroyable.destroy();
		} catch (DestroyFailedException e) {
			// Ignore exceptions thrown by default implementation
			StackTraceElement[] stes = e.getStackTrace();

			if (stes.length > 0 && !Destroyable.class.getName().equals(stes[0].getClassName())) {
				LOG.error(e, "Failed to destroy security object (type: {0})", destroyable.getClass().getName());
			}
		}
	}

	/**
	 * Gets the name of the cipher provided by this instance.
	 *
	 * @return the name of the cipher provided by this instance.
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * Gets the cipher factory for a specific cipher.
	 *
	 * @param name The cipher name to get the factory for.
	 * @return The found cipher factory.
	 * @throws NoSuchAlgorithmException if no factory has been found for the submitted cipher name.
	 */
	public static CipherFactory getInstance(String name) throws NoSuchAlgorithmException {
		ServiceLoader<CipherFactory> cipherFactories = ServiceLoader.load(CipherFactory.class);
		CipherFactory found = null;

		for (CipherFactory cipherFactory : cipherFactories) {
			if (cipherFactory.name().equals(name)) {
				found = cipherFactory;
			}
		}
		if (found == null) {
			throw new NoSuchAlgorithmException("Unknown cipher: " + name);
		}
		return found;
	}

	/**
	 * Creates a new {@linkplain Cipher} instance.
	 *
	 * @return a new {@linkplain Cipher} instance.
	 * @throws GeneralSecurityException if the cipher creation fails.
	 */
	public abstract Cipher createCipher() throws GeneralSecurityException;

	/**
	 * Re-creates a {@linkplain Cipher} instance from it's encoded representation.
	 *
	 * @param encoded the encoded representation of the cipher to re-create.
	 * @return the re-created {@linkplain Cipher} instance.
	 * @throws GeneralSecurityException if the cipher creation fails.
	 */
	public abstract Cipher createCipher(ByteSecret encoded) throws GeneralSecurityException;

}
