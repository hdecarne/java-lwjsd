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

import java.security.SecureRandom;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

import de.carne.boot.logging.Log;

/**
 * Base class for secret providers like {@linkplain Cipher}, {@linkplain Signature}, ... .
 */
public abstract class SecretsProvider {

	private static final Log LOG = new Log();

	private final String name;

	/**
	 * Constructs new {@linkplain SecretsProvider} instance.
	 *
	 * @param name the name of the provided secret type.
	 */
	protected SecretsProvider(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of provided secret type.
	 *
	 * @return the name of provided secret type.
	 */
	public final String name() {
		return this.name;
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

}
