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
 * Utility class for taking care of proper secret clean-up after usage.
 */
public final class CharSecret implements AutoCloseable {

	private final char[] chars;

	private CharSecret(char[] chars) {
		this.chars = chars;
	}

	/**
	 * Create {@linkplain CharSecret} instance backed up by the submitted array.
	 *
	 * @param chars the char array to wrap.
	 * @return the created {@linkplain CharSecret} instance.
	 */
	public static CharSecret wrap(char[] chars) {
		return new CharSecret(chars);
	}

	/**
	 * Gets the secret's length.
	 *
	 * @return the secret's length.
	 */
	public int length() {
		return this.chars.length;
	}

	/**
	 * Gets the secret.
	 *
	 * @return the secret.
	 */
	public char[] get() {
		return this.chars;
	}

	@Override
	public void close() {
		for (int charIndex = 0; charIndex < this.chars.length; charIndex++) {
			this.chars[charIndex] = '\0';
		}
	}

}
