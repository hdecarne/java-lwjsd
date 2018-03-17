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
package de.carne.lwjsd.runtime.test.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.check.Nullable;
import de.carne.lwjsd.runtime.logging.SyslogConfig;
import de.carne.lwjsd.runtime.logging.SyslogDestination;
import de.carne.lwjsd.runtime.logging.SyslogMessage;
import de.carne.lwjsd.runtime.logging.SyslogOption;
import de.carne.lwjsd.runtime.logging.SyslogProtocol;
import de.carne.util.logging.Log;

/**
 * Test Syslog related classes.
 */
class SyslogTest {

	private static final Log LOG = new Log();

	private static final String LOCALHOST = "localhost";
	private static final String APP = "lwjsd/test";
	private static final String MSGID = "msgid";
	private static final Instant TIMESTAMP = Instant.ofEpochSecond(1);

	@Test
	void testSyslogConfig() {
		SyslogConfig config1 = new SyslogConfig(LOCALHOST).addOption(SyslogOption.TRANSPORT_TCP);
		SyslogConfig config2 = new SyslogConfig(LOCALHOST, 1234);
		SyslogConfig config3 = new SyslogConfig(LOCALHOST, 4321).addOption(SyslogOption.TRANSPORT_TCP_TLS);

		Assertions.assertEquals("tcp://localhost:514", config1.toString());
		Assertions.assertEquals("udp://localhost:1234", config2.toString());
		Assertions.assertEquals("tcp+tls://localhost:4321", config3.toString());

		Assertions.assertEquals(LOCALHOST, config2.host());
		Assertions.assertEquals(1234, config2.port());

		Assertions.assertEquals(SyslogProtocol.RFC3164, config2.getProtocol());
		config2.setProtocol(SyslogProtocol.RFC5424);
		Assertions.assertEquals(SyslogProtocol.RFC5424, config2.getProtocol());

		Assertions.assertEquals(new HashSet<>(Arrays.asList()), config2.getOptions());
		Assertions.assertFalse(config2.hasOption(SyslogOption.TRANSPORT_TCP));
		config2.addOption(SyslogOption.TRANSPORT_TCP);
		Assertions.assertTrue(config2.hasOption(SyslogOption.TRANSPORT_TCP));
		config2.addOption(SyslogOption.TRANSPORT_TCP);
		Assertions.assertTrue(config2.hasOption(SyslogOption.TRANSPORT_TCP));
		Assertions.assertFalse(config2.hasOption(SyslogOption.TRANSPORT_TCP_TLS));
		config2.addOption(SyslogOption.TRANSPORT_TCP_TLS);
		Assertions.assertTrue(config2.hasOption(SyslogOption.TRANSPORT_TCP_TLS));
		Assertions.assertEquals(
				new HashSet<>(Arrays.asList(SyslogOption.TRANSPORT_TCP, SyslogOption.TRANSPORT_TCP_TLS)),
				config2.getOptions());
		config2.removeOption(SyslogOption.TRANSPORT_TCP_TLS);
		Assertions.assertFalse(config2.hasOption(SyslogOption.TRANSPORT_TCP_TLS));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(SyslogOption.TRANSPORT_TCP)), config2.getOptions());

		config2.setDefaultMessageHost("thishost");
		Assertions.assertEquals("thishost", config2.getDefaultMessageHost());
		config2.setDefaultMessageApp("lwjsd");
		Assertions.assertEquals("lwjsd", config2.getDefaultMessageApp());
	}

	// @Test
	void testSyslogMessage() throws IOException {
		SyslogConfig rfc3164Config = new SyslogConfig(LOCALHOST, 1234).setProtocol(SyslogProtocol.RFC3164);
		SyslogConfig rfc5424Config = new SyslogConfig(LOCALHOST, 1234).setProtocol(SyslogProtocol.RFC5424);
		SyslogMessage message1 = new SyslogMessage(SyslogMessage.SEV_ALERT, SyslogMessage.FAC_LOCAL0, TIMESTAMP,
				getClass().getName());

		testSyslogMessageHelper(message1, rfc3164Config,
				new byte[] { 60, 49, 50, 57, 62, 74, 97, 110, 32, 48, 49, 32, 48, 49, 58, 48, 48, 58, 48, 49, 32, 104,
						111, 108, 103, 101, 114, 51, 46, 104, 111, 109, 101, 46, 104, 111, 108, 103, 101, 114, 46, 109,
						111, 98, 105, 32, 45, 32, 100, 101, 46, 99, 97, 114, 110, 101, 46, 108, 119, 106, 115, 100, 46,
						116, 101, 115, 116, 46, 117, 116, 105, 108, 46, 108, 111, 103, 103, 105, 110, 103, 46, 83, 121,
						115, 108, 111, 103, 84, 101, 115, 116 });
		testSyslogMessageHelper(message1, rfc5424Config,
				new byte[] { 60, 49, 50, 57, 62, 49, 32, 49, 57, 55, 48, 45, 48, 49, 45, 48, 49, 84, 48, 48, 58, 48, 48,
						58, 48, 49, 90, 32, 104, 111, 108, 103, 101, 114, 51, 46, 104, 111, 109, 101, 46, 104, 111, 108,
						103, 101, 114, 46, 109, 111, 98, 105, 32, 45, 32, 52, 53, 55, 55, 50, 32, 45, 32, 45, 32, -17,
						-69, -65, 100, 101, 46, 99, 97, 114, 110, 101, 46, 108, 119, 106, 115, 100, 46, 116, 101, 115,
						116, 46, 117, 116, 105, 108, 46, 108, 111, 103, 103, 105, 110, 103, 46, 83, 121, 115, 108, 111,
						103, 84, 101, 115, 116 });

		SyslogMessage message2 = new SyslogMessage(SyslogMessage.SEV_ALERT, SyslogMessage.FAC_LOCAL0, TIMESTAMP,
				getClass().getName()).setHost(LOCALHOST).setApp(APP).setMessageId(MSGID);

		testSyslogMessageHelper(message2, rfc3164Config,
				new byte[] { 60, 49, 50, 57, 62, 74, 97, 110, 32, 48, 49, 32, 48, 49, 58, 48, 48, 58, 48, 49, 32, 108,
						111, 99, 97, 108, 104, 111, 115, 116, 32, 108, 119, 106, 115, 100, 47, 116, 101, 115, 116, 91,
						52, 54, 49, 54, 49, 93, 58, 32, 100, 101, 46, 99, 97, 114, 110, 101, 46, 108, 119, 106, 115,
						100, 46, 116, 101, 115, 116, 46, 117, 116, 105, 108, 46, 108, 111, 103, 103, 105, 110, 103, 46,
						83, 121, 115, 108, 111, 103, 84, 101, 115, 116 });
		testSyslogMessageHelper(message2, rfc5424Config,
				new byte[] { 60, 49, 50, 57, 62, 49, 32, 49, 57, 55, 48, 45, 48, 49, 45, 48, 49, 84, 48, 48, 58, 48, 48,
						58, 48, 49, 90, 32, 108, 111, 99, 97, 108, 104, 111, 115, 116, 32, 108, 119, 106, 115, 100, 47,
						116, 101, 115, 116, 32, 52, 54, 49, 57, 53, 32, 109, 115, 103, 105, 100, 32, 45, 32, -17, -69,
						-65, 100, 101, 46, 99, 97, 114, 110, 101, 46, 108, 119, 106, 115, 100, 46, 116, 101, 115, 116,
						46, 117, 116, 105, 108, 46, 108, 111, 103, 103, 105, 110, 103, 46, 83, 121, 115, 108, 111, 103,
						84, 101, 115, 116 });
	}

	private void testSyslogMessageHelper(SyslogMessage message, SyslogConfig config, byte[] expectedBytes)
			throws IOException {
		byte[] actualBytes = message.encode(config);

		printMessage(actualBytes);

		// If existent, replace pid value in message with the one from the expected message.
		// Not 100% fool proof (e.g. if pid is same as pri), but should be safe enough.
		if (actualBytes.length == expectedBytes.length) {
			byte[] pidBytes = Long.toString(ProcessHandle.current().pid()).getBytes(StandardCharsets.US_ASCII);

			for (int byteIndex = 0; byteIndex < actualBytes.length; byteIndex++) {
				if (Arrays.compare(pidBytes, 0, pidBytes.length, actualBytes, byteIndex,
						Math.min(byteIndex + pidBytes.length, actualBytes.length)) == 0) {
					System.arraycopy(expectedBytes, byteIndex, actualBytes, byteIndex, pidBytes.length);
					break;
				}
			}
		}
		Assertions.assertArrayEquals(expectedBytes, actualBytes);
	}

	// @Test
	void testSyslogDestination() throws IOException, InterruptedException {
		SyslogConfig udpConfig = new SyslogConfig(LOCALHOST, 1234);

		try (SyslogMessageReceiver udpReceiver = new SyslogMessageReceiver(udpConfig)) {
			udpConfig.setDefaultMessageHost(LOCALHOST).setDefaultMessageApp(APP);
			testSyslogDestinationHelper(udpReceiver, udpConfig);
			udpConfig.setProtocol(SyslogProtocol.RFC5424);
			testSyslogDestinationHelper(udpReceiver, udpConfig);
		}

		SyslogConfig tcpConfig1 = new SyslogConfig(LOCALHOST, 1235).addOption(SyslogOption.TRANSPORT_TCP);

		try (SyslogMessageReceiver tcpReceiver = new SyslogMessageReceiver(tcpConfig1)) {
			tcpConfig1.setDefaultMessageHost(LOCALHOST).setDefaultMessageApp(APP);
			testSyslogDestinationHelper(tcpReceiver, tcpConfig1);
			tcpConfig1.setProtocol(SyslogProtocol.RFC5424);
			testSyslogDestinationHelper(tcpReceiver, tcpConfig1);
		}

		SyslogConfig tcpConfig2 = new SyslogConfig(LOCALHOST, 1236).addOption(SyslogOption.TRANSPORT_TCP)
				.addOption(SyslogOption.OCTET_COUNTING_FRAMING);

		try (SyslogMessageReceiver tcpReceiver = new SyslogMessageReceiver(tcpConfig2)) {
			tcpConfig2.setDefaultMessageHost(LOCALHOST).setDefaultMessageApp(APP);
			testSyslogDestinationHelper(tcpReceiver, tcpConfig2);
			tcpConfig2.setProtocol(SyslogProtocol.RFC5424);
			testSyslogDestinationHelper(tcpReceiver, tcpConfig2);
		}
	}

	private void testSyslogDestinationHelper(SyslogMessageReceiver receiver, SyslogConfig config)
			throws IOException, InterruptedException {
		try (SyslogDestination destination = new SyslogDestination(config)) {
			SyslogMessage message1 = new SyslogMessage(SyslogMessage.SEV_DEBUG, SyslogMessage.FAC_LOCAL0, TIMESTAMP,
					getClass().getName()).setHost(LOCALHOST).setApp(APP);

			destination.send(message1);

			byte[] received1 = receiver.pollMessage();

			printMessage(received1);

			Assertions.assertArrayEquals(message1.encode(config), received1);

			SyslogMessage message2 = new SyslogMessage(SyslogMessage.SEV_INFO, SyslogMessage.FAC_LOCAL1, TIMESTAMP,
					getClass().getName());

			destination.send(message2);

			byte[] received2 = receiver.pollMessage();

			printMessage(received2);

			Assertions.assertArrayEquals(message2.encode(config), received2);
		}
	}

	private void printMessage(@Nullable byte[] message) {
		StringBuilder buffer = new StringBuilder();

		buffer.append("Message: byte[] '{ ");
		if (message != null) {
			for (int messageIndex = 0; messageIndex < message.length; messageIndex++) {
				if (messageIndex > 0) {
					buffer.append(", ");
				}
				buffer.append(message[messageIndex]);
			}
			buffer.append(" }' //");
			buffer.append(new String(message, StandardCharsets.UTF_8));
		} else {
			buffer.append("}'");
		}
		LOG.info(buffer.toString());
	}

}
