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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Syslog message object that can be sent via a {@linkplain SyslogDestination}.
 */
public final class SyslogMessage {

	/**
	 * Severity: Emergency: system is unusable
	 */
	public static final int SEV_EMERG = 0;

	/**
	 * Severity: Alert: action must be taken immediately
	 */
	public static final int SEV_ALERT = 1;

	/**
	 * Severity: Critical: critical conditions
	 */
	public static final int SEV_CRIT = 2;

	/**
	 * Severity: Error: error conditions
	 */
	public static final int SEV_ERR = 3;

	/**
	 * Severity: Warning: warning conditions
	 */
	public static final int SEV_WARNING = 4;

	/**
	 * Severity: Notice: normal but significant condition
	 */
	public static final int SEV_NOTICE = 5;

	/**
	 * Severity: Informational: informational messages
	 */
	public static final int SEV_INFO = 6;

	/**
	 * Severity: Debug: debug-level messages
	 */
	public static final int SEV_DEBUG = 7;

	/**
	 * Facility: kernel messages
	 */
	public static final int FAC_KERN = 0;

	/**
	 * Facility: user-level messages
	 */
	public static final int FAC_USER = 1;

	/**
	 * Facility: mail system
	 */
	public static final int FAC_MAIL = 2;

	/**
	 * Facility: system daemons
	 */
	public static final int FAC_DAEMON = 3;

	/**
	 * Facility: security/authorization messages
	 */
	public static final int FAC_AUTH = 4;

	/**
	 * Facility: messages generated internally by syslogd
	 */
	public static final int FAC_SYSLOG = 5;

	/**
	 * Facility: line printer subsystem
	 */
	public static final int FAC_LPR = 6;

	/**
	 * Facility: network news subsystem
	 */
	public static final int FAC_NEWS = 7;

	/**
	 * Facility: UUCP subsystem
	 */
	public static final int FAC_UUCP = 8;

	/**
	 * Facility: clock daemon
	 */
	public static final int FAC_CRON = 9;

	/**
	 * Facility: security/authorization messages
	 */
	public static final int FAC_AUTHPRIV = 10;

	/**
	 * Facility: local use 0
	 */
	public static final int FAC_LOCAL0 = 16;

	/**
	 * Facility: local use 1
	 */
	public static final int FAC_LOCAL1 = 17;

	/**
	 * Facility: local use 2
	 */
	public static final int FAC_LOCAL2 = 18;

	/**
	 * Facility: local use 3
	 */
	public static final int FAC_LOCAL3 = 19;

	/**
	 * Facility: local use 4
	 */
	public static final int FAC_LOCAL4 = 20;

	/**
	 * Facility: local use 5
	 */
	public static final int FAC_LOCAL5 = 21;

	/**
	 * Facility: local use 6
	 */
	public static final int FAC_LOCAL6 = 22;

	/**
	 * Facility: local use 7
	 */
	public static final int FAC_LOCAL7 = 23;

	/**
	 * Field NIL value.
	 */
	public static final String NIL = "-";

	private static final int DEFAULT_MESSAGE_SIZE = 1024;

	private final int pri;
	private final Instant timestamp;
	private final String msg;
	private String host = NIL;
	private String app = NIL;
	private String msgid = NIL;

	/**
	 * Construct {@linkplain SyslogMessage}.
	 *
	 * @param sev The message severity (see SEV_ constants).
	 * @param fac The message facility (see FAC_ constants).
	 * @param timestamp The message timestamp.
	 * @param msg The message text.
	 */
	public SyslogMessage(int sev, int fac, Instant timestamp, String msg) {
		this.pri = (sev & 0x7) | ((fac & 0x7f) << 3);
		this.timestamp = timestamp;
		this.msg = msg;
	}

	/**
	 * Set the message host name.
	 *
	 * @param host The host name to set.
	 * @return The updated {@linkplain SyslogMessage}.
	 */
	public SyslogMessage setHost(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Set the message application name.
	 *
	 * @param app The application name to set.
	 * @return The updated {@linkplain SyslogMessage}.
	 */
	public SyslogMessage setApp(String app) {
		this.app = app;
		return this;
	}

	/**
	 * Set the message id.
	 *
	 * @param msgid The message id to set.
	 * @return The updated {@linkplain SyslogMessage}.
	 */
	public SyslogMessage setMessageId(String msgid) {
		this.msgid = msgid;
		return this;
	}

	/**
	 * Encode the message to bytes.
	 * <p>
	 * The submitted Syslog configuration object defines the actual message protocol ({@linkplain SyslogProtocol}) as
	 * well as defaults for some of the message attributes.
	 *
	 * @param config The Syslog configuration to use.
	 * @return The encoded message.
	 * @throws IOException if an I/O error occurs during encoding.
	 */
	public byte[] encode(SyslogConfig config) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(DEFAULT_MESSAGE_SIZE);

		encodeTo(buffer, config);
		return buffer.toByteArray();
	}

	/**
	 * Encode the message to output stream.
	 * <p>
	 * The submitted Syslog configuration object defines the actual message protocol ({@linkplain SyslogProtocol}) as
	 * well as defaults for some of the message attributes.
	 *
	 * @param out The output stream to encode to.
	 * @param config The Syslog configuration to use.
	 * @throws IOException if an I/O error occurs during encoding.
	 */
	public void encodeTo(OutputStream out, SyslogConfig config) throws IOException {
		SyslogProtocol protocol = config.getProtocol();

		switch (protocol) {
		case RFC3164:
			encodeRfc3164To(out, config);
			break;
		case RFC5424:
			encodeRfc5424To(out, config);
			break;
		default:
			throw new IllegalArgumentException("Unexpected protocol: " + protocol);
		}
	}

	private static final DateTimeFormatter RFC_3164_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss",
			Locale.US);

	private void encodeRfc3164To(OutputStream out, SyslogConfig config) throws IOException {
		StringBuilder prologue = new StringBuilder();

		prologue.append('<');
		prologue.append(Integer.toString(this.pri));
		prologue.append('>');

		ZonedDateTime messageTimestamp = this.timestamp.atZone(ZoneId.systemDefault());

		prologue.append(RFC_3164_TIMESTAMP_FORMAT.format(messageTimestamp));
		prologue.append(' ');

		String messageHost = (!NIL.equals(this.host) ? this.host : config.getDefaultMessageHost());

		prologue.append(messageHost);
		prologue.append(' ');

		String messageApp = (!NIL.equals(this.app) ? this.app : config.getDefaultMessageApp());

		prologue.append(messageApp);
		if (!NIL.equals(messageApp)) {
			prologue.append('[');
			prologue.append(Long.toString(ProcessHandle.current().pid()));
			prologue.append("]: ");
		} else {
			prologue.append(' ');
		}
		if (config.hasOption(SyslogOption.TRANSPORT_TCP) || config.hasOption(SyslogOption.TRANSPORT_TCP_TLS)) {
			if (config.hasOption(SyslogOption.OCTET_COUNTING_FRAMING)) {
				byte[] prologueBytes = prologue.toString().getBytes(StandardCharsets.US_ASCII);
				byte[] msgBytes = this.msg.getBytes(StandardCharsets.US_ASCII);
				StringBuilder octetCount = new StringBuilder();

				octetCount.append(Integer.toString(prologueBytes.length + msgBytes.length));
				octetCount.append(' ');
				out.write(octetCount.toString().getBytes(StandardCharsets.US_ASCII));
				out.write(prologueBytes);
				out.write(msgBytes);
			} else {
				out.write(prologue.toString().getBytes(StandardCharsets.US_ASCII));
				out.write(this.msg.getBytes(StandardCharsets.US_ASCII));
				out.write(Syslog.NON_TRANSPARENT_FRAMING_TRAILER.getBytes(StandardCharsets.US_ASCII));
			}
		} else {
			out.write(prologue.toString().getBytes(StandardCharsets.US_ASCII));
			out.write(this.msg.getBytes(StandardCharsets.US_ASCII));
		}
	}

	private static final DateTimeFormatter RFC_5425_TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;
	private static final byte[] UTF8_BOM_BYTES = { (byte) 0xef, (byte) 0xbb, (byte) 0xbf };

	private void encodeRfc5424To(OutputStream out, SyslogConfig config) throws IOException {
		StringBuilder prologue = new StringBuilder();

		prologue.append('<');
		prologue.append(Integer.toString(this.pri));
		prologue.append(">1 ");
		prologue.append(RFC_5425_TIMESTAMP_FORMAT.format(this.timestamp));
		prologue.append(' ');

		String messageHost = (!NIL.equals(this.host) ? this.host : config.getDefaultMessageHost());

		prologue.append(messageHost);
		prologue.append(' ');

		String messageApp = (!NIL.equals(this.app) ? this.app : config.getDefaultMessageApp());

		prologue.append(messageApp);
		prologue.append(' ');
		prologue.append(Long.toString(ProcessHandle.current().pid()));
		prologue.append(' ');
		prologue.append(this.msgid);
		prologue.append(" - ");
		if (config.hasOption(SyslogOption.TRANSPORT_TCP) || config.hasOption(SyslogOption.TRANSPORT_TCP_TLS)) {
			if (config.hasOption(SyslogOption.OCTET_COUNTING_FRAMING)) {
				byte[] prologueBytes = prologue.toString().getBytes(StandardCharsets.US_ASCII);
				byte[] msgBytes = this.msg.getBytes(StandardCharsets.UTF_8);
				StringBuilder octetCount = new StringBuilder();

				octetCount.append(Integer.toString(prologueBytes.length + UTF8_BOM_BYTES.length + msgBytes.length));
				octetCount.append(' ');
				out.write(octetCount.toString().getBytes(StandardCharsets.US_ASCII));
				out.write(prologueBytes);
				out.write(UTF8_BOM_BYTES);
				out.write(msgBytes);
			} else {
				out.write(prologue.toString().getBytes(StandardCharsets.US_ASCII));
				out.write(UTF8_BOM_BYTES);
				out.write(this.msg.getBytes(StandardCharsets.UTF_8));
				out.write(Syslog.NON_TRANSPARENT_FRAMING_TRAILER.getBytes(StandardCharsets.US_ASCII));
			}
		} else {
			out.write(prologue.toString().getBytes(StandardCharsets.US_ASCII));
			out.write(UTF8_BOM_BYTES);
			out.write(this.msg.getBytes(StandardCharsets.UTF_8));
		}
	}

}
