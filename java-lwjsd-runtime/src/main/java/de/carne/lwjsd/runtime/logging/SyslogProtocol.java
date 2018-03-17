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
package de.carne.lwjsd.runtime.logging;

/**
 * The supported Syslog protocols.
 */
public enum SyslogProtocol {

	/**
	 * Syslog format according to <a href="https://www.ietf.org/rfc/rfc3164.txt">RFC 3164</> (The BSD syslog Protocol).
	 */
	RFC3164,

	/**
	 * Syslog format according to <a href="https://www.ietf.org/rfc/rfc5424.txt">RFC 5424</> (The Syslog Protocol).
	 */
	RFC5424

}
