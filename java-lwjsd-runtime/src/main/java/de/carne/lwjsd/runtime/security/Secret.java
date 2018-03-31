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

/**
 * Base class for objects that represent some kind of security key and are managed via a {@linkplain SecretsProvider}
 * instance.
 */
@SuppressWarnings("squid:S1610")
public abstract class Secret implements AutoCloseable {

	/**
	 * Gets the {@linkplain SecretsProvider} instance managing this {@linkplain Secret}.
	 *
	 * @return the {@linkplain SecretsProvider} instance managing this {@linkplain Secret}.
	 */
	public abstract SecretsProvider provider();

	/**
	 * Gets the name of the {@linkplain Secret} represented by this instance.
	 *
	 * @return the name of the {@linkplain Secret} represented by this instance.
	 */
	public final String name() {
		return provider().name();
	}

	/**
	 * Gets the encoded representation of this {@linkplain Secret}.
	 *
	 * @return the encoded representation of this {@linkplain Secret}.
	 */
	public abstract ByteSecret getEncoded();

}
