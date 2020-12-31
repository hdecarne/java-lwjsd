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
package de.carne.lwjsd.runtime.logging;

/**
 * The available Syslog options.
 */
public enum SyslogOption {

	/**
	 * Enable TCP transport (default is udp).
	 */
	TRANSPORT_TCP,

	/**
	 * Enable TLS encrypted TCP transport (implies {@linkplain #TRANSPORT_TCP}).
	 */
	TRANSPORT_TCP_TLS,

	/**
	 * Use octect-counting framing for TCP based transports (default is to use non-transparent-framing).
	 */
	OCTET_COUNTING_FRAMING

}
