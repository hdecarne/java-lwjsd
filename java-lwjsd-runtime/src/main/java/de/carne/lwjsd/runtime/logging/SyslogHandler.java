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

import java.io.IOException;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import de.carne.check.Nullable;
import de.carne.io.Closeables;
import de.carne.util.logging.LogLevel;
import de.carne.util.logging.Logs;

/**
 * {@linkplain Handler} implementation providing Syslog support.
 */
public class SyslogHandler extends Handler {

	@Nullable
	private SyslogDestination destination = null;
	private final SyslogMessage.Facility facility;

	/**
	 * Constructs a new {@linkplain SyslogHandler} instance.
	 */
	public SyslogHandler() {
		this(null);
	}

	/**
	 * Constructs a new {@linkplain SyslogHandler} instance.
	 * 
	 * @param destination the initial {@linkplain SyslogDestination} to use for sending log messages.
	 */
	public SyslogHandler(@Nullable SyslogDestination destination) {
		LogManager manager = LogManager.getLogManager();
		String propertyBase = getClass().getName();
		SyslogMessage.Facility defaultFacility = SyslogMessage.Facility.FAC_USER;

		this.facility = getFacilityProperty(manager, propertyBase + ".facility", defaultFacility);
		this.destination = destination;
		setFormatter(new Formatter() {
			@Override
			public String format(@Nullable LogRecord record) {
				return formatMessage(record);
			}
		});
	}

	/**
	 * Sets the {@linkplain SyslogDestination} receiving the log message.
	 *
	 * @param destination the {@linkplain SyslogDestination} to set (may be {@code null}).
	 * @return the previous {@linkplain SyslogDestination} (may be {@code null}).
	 */
	@Nullable
	public synchronized SyslogDestination setDestination(@Nullable SyslogDestination destination) {
		SyslogDestination oldDestination = this.destination;

		this.destination = destination;
		return oldDestination;
	}

	@Override
	public void close() {
		try {
			Closeables.close(this.destination);
		} catch (IOException e) {
			getErrorManager().error("Failed to close syslog destination: " + this.destination, e,
					ErrorManager.CLOSE_FAILURE);
		}
	}

	@Override
	public void flush() {
		// Nothing to do here
	}

	@Override
	public void publish(@Nullable LogRecord record) {
		SyslogDestination checkedDestination = this.destination;

		if (record != null && checkedDestination != null) {
			SyslogMessage.Severity severity = level2Severity(record.getLevel());
			String msg = getFormatter().format(record);
			SyslogMessage message = new SyslogMessage(severity, this.facility, record.getInstant(), msg);

			try {
				checkedDestination.send(message);
			} catch (IOException e) {
				getErrorManager().error("Failed to send syslog message to destination: " + checkedDestination, e,
						ErrorManager.WRITE_FAILURE);
			}
		}
	}

	private static SyslogMessage.Facility getFacilityProperty(LogManager manager, String name,
			SyslogMessage.Facility defaultValue) {
		String property = manager.getProperty(name);
		SyslogMessage.Facility propertyValue = defaultValue;

		if (property != null) {
			try {
				propertyValue = SyslogMessage.Facility.valueOf(property);
			} catch (IllegalArgumentException e) {
				Logs.DEFAULT_ERROR_MANAGER.error("Invalid int property " + name, e, ErrorManager.GENERIC_FAILURE);
			}
		}
		return propertyValue;
	}

	private static SyslogMessage.Severity level2Severity(Level level) {
		int levelValue = level.intValue();
		SyslogMessage.Severity severity;

		if (levelValue <= LogLevel.LEVEL_DEBUG.intValue()) {
			severity = SyslogMessage.Severity.SEV_DEBUG;
		} else if (levelValue <= LogLevel.LEVEL_INFO.intValue()) {
			severity = SyslogMessage.Severity.SEV_INFO;
		} else if (levelValue <= LogLevel.LEVEL_WARNING.intValue()) {
			severity = SyslogMessage.Severity.SEV_WARNING;
		} else if (levelValue <= LogLevel.LEVEL_ERROR.intValue()) {
			severity = SyslogMessage.Severity.SEV_ERR;
		} else if (levelValue < LogLevel.LEVEL_NOTICE.intValue()) {
			severity = SyslogMessage.Severity.SEV_CRIT;
		} else {
			severity = SyslogMessage.Severity.SEV_NOTICE;
		}
		return severity;
	}

}
