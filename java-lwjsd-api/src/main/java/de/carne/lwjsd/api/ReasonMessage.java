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

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * The {@code ReasonMessage} class is used to provide additional details in case of an error or other failure
 * situations.
 */
public final class ReasonMessage implements Serializable {

	// Serialization support
	private static final long serialVersionUID = 7667356127095825747L;

	/**
	 * The reason causing this message.
	 */
	public enum Reason {

		/**
		 * An operation failed due to a general error.
		 */
		GENERAL_FAILURE,

		/**
		 * An operation failed due to an illegal argument.
		 */
		ILLEGAL_ARGUMENT,

		/**
		 * An operation failed due to an illegal state.
		 */
		ILLEGAL_STATE

	}

	private final Reason reason;
	private final String message;

	/**
	 * Constructs a new {@linkplain ReasonMessage} instance.
	 *
	 * @param reason the message reason.
	 * @param pattern the message pattern.
	 * @param arguments the message arguments.
	 * @see MessageFormat
	 */
	public ReasonMessage(Reason reason, String pattern, Object... arguments) {
		this.reason = reason;
		this.message = (arguments.length > 0 ? MessageFormat.format(pattern, arguments) : pattern);
	}

	/**
	 * Constructs a new {@linkplain Reason#GENERAL_FAILURE} {@linkplain ReasonMessage} instance with the given message.
	 *
	 * @param pattern the message pattern.
	 * @param arguments the message arguments.
	 * @return the constructed {@linkplain ReasonMessage} instance.
	 * @see MessageFormat
	 */
	public static ReasonMessage generalFailure(String pattern, Object... arguments) {
		return new ReasonMessage(Reason.GENERAL_FAILURE, pattern, arguments);
	}

	/**
	 * Constructs a new {@linkplain Reason#ILLEGAL_ARGUMENT} {@linkplain ReasonMessage} instance with the given message.
	 *
	 * @param pattern the message pattern.
	 * @param arguments the message arguments.
	 * @return the constructed {@linkplain ReasonMessage} instance.
	 * @see MessageFormat
	 */
	public static ReasonMessage illegalArgument(String pattern, Object... arguments) {
		return new ReasonMessage(Reason.ILLEGAL_ARGUMENT, pattern, arguments);
	}

	/**
	 * Constructs a new {@linkplain Reason#ILLEGAL_STATE} {@linkplain ReasonMessage} instance with the given message.
	 *
	 * @param pattern the message pattern.
	 * @param arguments the message arguments.
	 * @return the constructed {@linkplain ReasonMessage} instance.
	 * @see MessageFormat
	 */
	public static ReasonMessage illegalState(String pattern, Object... arguments) {
		return new ReasonMessage(Reason.ILLEGAL_STATE, pattern, arguments);
	}

	/**
	 * Gets the message reason.
	 *
	 * @return the message reason.
	 */
	public Reason reason() {
		return this.reason;
	}

	/**
	 * Gets the message text.
	 *
	 * @return the message text.
	 */
	public String message() {
		return this.message;
	}

	@Override
	public String toString() {
		return this.reason + ": " + this.message;
	}

}
