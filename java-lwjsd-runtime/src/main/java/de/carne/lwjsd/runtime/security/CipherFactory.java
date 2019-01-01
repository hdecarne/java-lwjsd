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
import java.security.NoSuchAlgorithmException;
import java.util.ServiceLoader;

/**
 * Base class for all kinds of {@linkplain Cipher} factories.
 * <p>
 * Cipher factories are used to create and initialize {@linkplain Cipher} instances suitable for encryption and
 * decryption of data.
 */
public abstract class CipherFactory extends SecretsProvider {

	/**
	 * Constructs new {@linkplain CipherFactory} instance.
	 *
	 * @param name the cipher type uniquely identifying this factory.
	 */
	protected CipherFactory(String name) {
		super(name);
	}

	/**
	 * Gets the {@linkplain CipherFactory} for a specific cipher type.
	 *
	 * @param name The cipher type to get the factory for.
	 * @return The found {@linkplain CipherFactory}.
	 * @throws NoSuchAlgorithmException if no factory has been found for the submitted cipher type.
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
			throw new NoSuchAlgorithmException("Unknown cipher type: " + name);
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
