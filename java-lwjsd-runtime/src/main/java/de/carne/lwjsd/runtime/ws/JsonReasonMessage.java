/*
 * Copyright (c) 2018-2021 Holger de Carne and contributors, All Rights Reserved.
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

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import de.carne.lwjsd.api.ReasonMessage;

/**
 * JSON wrapper for {@linkplain ReasonMessage}.
 */
public final class JsonReasonMessage {

	private ReasonMessage.@Nullable Reason reason = null;
	@Nullable
	private String message = null;

	/**
	 * Constructs empty {@linkplain JsonReasonMessage} instance.
	 */
	public JsonReasonMessage() {
		// Nothing to do here
	}

	/**
	 * Constructs initialized {@linkplain JsonReasonMessage} instance.
	 *
	 * @param source the source object to use for initialization.
	 */
	public JsonReasonMessage(ReasonMessage source) {
		this.reason = source.reason();
		this.message = source.message();
	}

	/**
	 * Sets {@code reason}.
	 *
	 * @param reason {@code reason} attribute.
	 */
	public void setReason(ReasonMessage.Reason reason) {
		this.reason = reason;
	}

	/**
	 * Gets {@code reason} attribute.
	 *
	 * @return {@code reason} attribute.
	 */
	public ReasonMessage.Reason getReason() {
		return Objects.requireNonNull(this.reason);
	}

	/**
	 * Sets {@code message}.
	 *
	 * @param message {@code message} attribute.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Gets {@code message} attribute.
	 *
	 * @return {@code message} attribute.
	 */
	public String getMessage() {
		return Objects.requireNonNull(this.message);
	}

	/**
	 * Convert JSON wrapper to source object:
	 *
	 * @return the transferred source object.
	 */
	public ReasonMessage toSource() {
		return new ReasonMessage(getReason(), getMessage());
	}

}
