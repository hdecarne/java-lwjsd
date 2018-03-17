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
package de.carne.lwjsd.runtime.ws;

/**
 * Base response structure providing the success status of an executed request.
 */
public class StatusMessage {

	/**
	 * The request has been executed successfully.
	 * <p>
	 * All other status messages indicate an error.
	 */
	public static final String OK = "OK";

	private String message;

	/**
	 * Constructs a new {@linkplain StatusMessage} instance with {@linkplain #OK} message.
	 */
	public StatusMessage() {
		this(OK);
	}

	/**
	 * Construct a new {@linkplain StatusMessage} instance with a given status message.
	 *
	 * @param message the status message.
	 */
	public StatusMessage(String message) {
		this.message = message;
	}

	/**
	 * Gets the status message.
	 * 
	 * @return the status message.
	 */
	public String getStatusMessage() {
		return this.message;
	}

	/**
	 * Sets the status message.
	 * 
	 * @param message the status message to set.
	 */
	public void setStatusMessage(String message) {
		this.message = message;
	}

}
