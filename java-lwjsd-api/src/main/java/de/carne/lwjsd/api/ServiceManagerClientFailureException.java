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

/**
 * This exception indicates a client error while accessing the ServiceManager.
 */
public class ServiceManagerClientFailureException extends ServiceManagerException {

	// Serialization support
	private static final long serialVersionUID = -8297535194094230939L;

	/**
	 * Constructs {@linkplain ServiceManagerClientFailureException}.
	 *
	 * @param message the exception message.
	 */
	public ServiceManagerClientFailureException(String message) {
		super(message);
	}

	/**
	 * Constructs {@linkplain ServiceManagerClientFailureException}.
	 *
	 * @param cause the causing exception.
	 */
	public ServiceManagerClientFailureException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs {@linkplain ServiceManagerClientFailureException}.
	 *
	 * @param message the exception message.
	 * @param cause the causing exception.
	 */
	public ServiceManagerClientFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
