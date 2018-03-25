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

/**
 * Base class for all kinds of signatures.
 */
public abstract class Signature extends Secret {

	@Override
	public final SecretsProvider provider() {
		return factory();
	}

	/**
	 * Gets the {@linkplain SignatureFactory} used to create this {@linkplain Signature}.
	 *
	 * @return the {@linkplain SignatureFactory} used to create this {@linkplain Signature}.
	 */
	public abstract SignatureFactory factory();

	/**
	 * Signs the data provided by the given {@linkplain InputStream}.
	 *
	 * @param data the data to sign.
	 * @return the generated signature bytes.
	 * @throws IOException if an I/O error occurs during signing.
	 * @throws GeneralSecurityException if an security error occurs during signing.
	 */
	public abstract byte[] sign(InputStream data) throws IOException, GeneralSecurityException;

	/**
	 * Verifies the data provided by the given {@linkplain InputStream}.
	 *
	 * @param data the data to verify.
	 * @param signatureBytes the signature bytes to verify against.
	 * @return {@code true} if the verification succeeds. {@code false} otherwise.
	 * @throws IOException if an I/O error occurs during signing.
	 * @throws GeneralSecurityException if an security error occurs during verification.
	 */
	public abstract boolean verify(InputStream data, byte[] signatureBytes)
			throws IOException, GeneralSecurityException;

}
