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

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ServiceLoader;

/**
 * Base class for all kinds of {@linkplain Signature} providers.
 * <p>
 * Signature factories are used to create and initialize {@linkplain Signature} instances suitable for signing and
 * verifying data.
 */
public abstract class SignatureFactory extends SecretsProvider {

	/**
	 * Constructs new {@linkplain SignatureFactory} instance.
	 *
	 * @param name the signature type uniquely identifying this factory.
	 */
	protected SignatureFactory(String name) {
		super(name);
	}

	/**
	 * Gets the {@linkplain SignatureFactory} for a specific signature type.
	 *
	 * @param name The signature type to get the factory for.
	 * @return The found {@linkplain SignatureFactory}.
	 * @throws NoSuchAlgorithmException if no factory has been found for the submitted signature type.
	 */
	public static SignatureFactory getInstance(String name) throws NoSuchAlgorithmException {
		ServiceLoader<SignatureFactory> signatureFactories = ServiceLoader.load(SignatureFactory.class);
		SignatureFactory found = null;

		for (SignatureFactory signatureFactory : signatureFactories) {
			if (signatureFactory.name().equals(name)) {
				found = signatureFactory;
			}
		}
		if (found == null) {
			throw new NoSuchAlgorithmException("Unknown signature type: " + name);
		}
		return found;
	}

	/**
	 * Creates a new {@linkplain Signature} instance.
	 *
	 * @return a new {@linkplain Signature} instance.
	 * @throws GeneralSecurityException if the {@linkplain Signature} creation fails.
	 */
	public abstract Signature createSignature() throws GeneralSecurityException;

	/**
	 * Re-creates a {@linkplain Signature} instance from it's encoded representation.
	 *
	 * @param encoded the encoded representation of the {@linkplain Signature} to re-create.
	 * @return the re-created {@linkplain Signature} instance.
	 * @throws GeneralSecurityException if the {@linkplain Signature} creation fails.
	 */
	public abstract Signature createSignature(ByteSecret encoded) throws GeneralSecurityException;

}
