/*
 * Copyright (c) 2018-2019 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.lwjsd.runtime.logging;

import de.carne.util.SystemProperties;

/**
 * Global Syslog settings.
 */
public final class Syslog {

	private Syslog() {
		// Prevent instantiation
	}

	/**
	 * Time (in nanoseconds) after which an existing connection is automatic closed and re-established.
	 */
	public static final long CONNECTION_TTL = SystemProperties.longValue(".CONNECTION_TTL", 60 * 60 * 1000000000l);

	/**
	 * Number of retries during Syslog message sending.
	 */
	public static final int RETRY_COUNT = SystemProperties.intValue(".RETRY_COUNT", 3);

	/**
	 * The trailer to use for non-transparent-framing for TCP based transports.
	 */
	public static final String NON_TRANSPARENT_FRAMING_TRAILER = "\n";

}
