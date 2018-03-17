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
 * This exception indicates an error during ServiceManager operation.
 */
public class ServiceManagerOperationFailureException extends ServiceManagerException {

	// Serialization support
	private static final long serialVersionUID = -8612424502439213058L;

	/**
	 * Constructs {@linkplain ServiceManagerOperationFailureException}.
	 *
	 * @param message the exception message.
	 */
	public ServiceManagerOperationFailureException(String message) {
		super(message);
	}

	/**
	 * Constructs {@linkplain ServiceManagerOperationFailureException}.
	 *
	 * @param cause the causing exception.
	 */
	public ServiceManagerOperationFailureException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs {@linkplain ServiceManagerOperationFailureException}.
	 *
	 * @param message the exception message.
	 * @param cause the causing exception.
	 */
	public ServiceManagerOperationFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
