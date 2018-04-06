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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.carne.boot.Exceptions;

/**
 * Syslog configuration object
 */
public final class SyslogConfig {

	/**
	 * Default Syslog port.
	 */
	public static final int DEFAULT_PORT = 514;

	private final String host;
	private final int port;
	private SyslogProtocol protocol = SyslogProtocol.RFC3164;
	private Set<SyslogOption> options = new HashSet<>();
	private String defaultMessageHost = defaultMessageHost();
	private String defaultMessageApp = SyslogMessage.NIL;

	/**
	 * Construct {@linkplain SyslogConfig}.
	 *
	 * @param host The host to send Syslog messages to.
	 */
	public SyslogConfig(String host) {
		this(host, DEFAULT_PORT);
	}

	/**
	 * Construct {@linkplain SyslogConfig}.
	 *
	 * @param host The host to send Syslog messages to.
	 * @param port The port to send Syslog messages to.
	 */
	public SyslogConfig(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Get the Syslog host.
	 *
	 * @return The Syslog host.
	 */
	public String host() {
		return this.host;
	}

	/**
	 * Get the Syslog port.
	 *
	 * @return The Syslog port.
	 */
	public int port() {
		return this.port;
	}

	/**
	 * Set the Syslog protocol to use.
	 *
	 * @param protocol The Syslog protocl to use.
	 * @return The updated {@linkplain SyslogConfig} object.
	 * @see SyslogProtocol
	 */
	public SyslogConfig setProtocol(SyslogProtocol protocol) {
		this.protocol = protocol;
		return this;
	}

	/**
	 * Get the Syslog protocol to use.
	 *
	 * @return The Syslog protocol to use.
	 * @see SyslogProtocol
	 */
	public SyslogProtocol getProtocol() {
		return this.protocol;
	}

	/**
	 * Add/enable a Syslog option.
	 *
	 * @param option The Syslog option to add/enable.
	 * @return The updated {@linkplain SyslogConfig} object.
	 * @see SyslogOption
	 */
	public SyslogConfig addOption(SyslogOption option) {
		this.options.add(option);
		return this;
	}

	/**
	 * Remove/disable a Syslog option.
	 *
	 * @param option The Syslog option to remove/disable.
	 * @return The updated {@linkplain SyslogConfig} object.
	 * @see SyslogOption
	 */
	public SyslogConfig removeOption(SyslogOption option) {
		this.options.remove(option);
		return this;
	}

	/**
	 * Get the enabled Syslog options.
	 *
	 * @return The enabled Syslog options.
	 * @see SyslogOption
	 */
	public Set<SyslogOption> getOptions() {
		return Collections.unmodifiableSet(this.options);
	}

	/**
	 * Check whether a specific Syslog option is set.
	 * 
	 * @param option The Syslog option to check.
	 * @return {@code true} if the submitted Syslog option is set.
	 */
	public boolean hasOption(SyslogOption option) {
		return this.options.contains(option);
	}

	/**
	 * Set the default message host for message sending.
	 *
	 * @param defaultMessageHost The default message host to use for message sending if a message does not specify one
	 *        explicitly.
	 * @return The updated {@linkplain SyslogConfig} object.
	 */
	public SyslogConfig setDefaultMessageHost(String defaultMessageHost) {
		this.defaultMessageHost = defaultMessageHost;
		return this;
	}

	/**
	 * Get the default message host for message sending.
	 *
	 * @return The default message host to use for message sending if a message does not specify one explicitly.
	 */
	public String getDefaultMessageHost() {
		return this.defaultMessageHost;
	}

	/**
	 * Set the default application name for message sending.
	 *
	 * @param defaultMessageApp The default message application name to use for message sending if a message does not
	 *        specify one explicitly.
	 * @return The updated {@linkplain SyslogConfig} object.
	 */
	public SyslogConfig setDefaultMessageApp(String defaultMessageApp) {
		this.defaultMessageApp = defaultMessageApp;
		return this;
	}

	/**
	 * Get the default message application name for message sending.
	 *
	 * @return The default message application name to use for message sending if a message does not specify one
	 *         explicitly.
	 */
	public String getDefaultMessageApp() {
		return this.defaultMessageApp;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		if (this.options.contains(SyslogOption.TRANSPORT_TCP_TLS)) {
			buffer.append("tcp+tls://");
		} else if (this.options.contains(SyslogOption.TRANSPORT_TCP)) {
			buffer.append("tcp://");
		} else {
			buffer.append("udp://");
		}
		buffer.append(this.host);
		buffer.append(':');
		buffer.append(this.port);
		return buffer.toString();
	}

	private static String defaultMessageHost() {
		String defaultMessageHost;

		try {
			defaultMessageHost = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			Exceptions.ignore(e);
			defaultMessageHost = SyslogMessage.NIL;
		}
		return defaultMessageHost;
	}

}
