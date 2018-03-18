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
package de.carne.lwjsd.api;

import java.text.MessageFormat;

/**
 * This exception indicates an error during {@linkplain Service} operation.
 */
public class ServiceException extends Exception {

	// Serialization support
	private static final long serialVersionUID = -2408713377102975667L;

	/**
	 * Constructs {@linkplain ServiceException}.
	 *
	 * @param pattern the exception message pattern.
	 * @param arguments the exception message arguments.
	 * @see MessageFormat
	 */
	public ServiceException(String pattern, Object... arguments) {
		super(MessageFormat.format(pattern, arguments));
	}

	/**
	 * Constructs {@linkplain ServiceException}.
	 *
	 * @param cause the causing exception.
	 */
	public ServiceException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs {@linkplain ServiceException}.
	 *
	 * @param cause the causing exception.
	 * @param pattern the exception message pattern.
	 * @param arguments the exception message arguments.
	 * @see MessageFormat
	 */
	public ServiceException(Throwable cause, String pattern, Object... arguments) {
		super(MessageFormat.format(pattern, arguments), cause);
	}

}
