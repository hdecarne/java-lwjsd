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

import de.carne.util.Exceptions;

/**
 * Base exception for all {@linkplain ServiceManager} related exceptions.
 */
public abstract class ServiceManagerException extends Exception {

	// Serialization support
	private static final long serialVersionUID = 5457411671074494509L;

	/**
	 * Constructs {@linkplain ServiceManagerException}.
	 *
	 * @param message the exception message.
	 */
	protected ServiceManagerException(String message) {
		super(message);
	}

	/**
	 * Constructs {@linkplain ServiceManagerException}.
	 *
	 * @param cause the causing exception.
	 */
	protected ServiceManagerException(Throwable cause) {
		super(Exceptions.toString(cause), cause);
	}

	/**
	 * Constructs {@linkplain ServiceManagerException}.
	 *
	 * @param message the exception message.
	 * @param cause the causing exception.
	 */
	protected ServiceManagerException(String message, Throwable cause) {
		super(message, cause);
	}

}
