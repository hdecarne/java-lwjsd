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
import java.util.Arrays;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.logging.SyslogConfig;
import de.carne.lwjsd.runtime.logging.SyslogDestination;
import de.carne.lwjsd.runtime.logging.SyslogHandler;
import de.carne.lwjsd.runtime.logging.SyslogOption;
import de.carne.lwjsd.runtime.logging.SyslogProtocol;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;
import de.carne.boot.logging.LogLevel;

/**
 * Test {@linkplain SyslogHandler} class.
 */
class SyslogHandlerTest {

	@Test
	void testUdpSyslogHandler() throws IOException, ServiceManagerException, InterruptedException {
		testSyslogHandler(UdpSyslogReceiverService.class);
		testSyslogHandler(TcpSyslogReceiverService.class, SyslogOption.TRANSPORT_TCP);
		testSyslogHandler(TcpSyslogReceiverService.class, SyslogOption.TRANSPORT_TCP,
				SyslogOption.OCTET_COUNTING_FRAMING);
	}

	private void testSyslogHandler(Class<? extends SyslogReceiver> receiverClass, SyslogOption... options)
			throws IOException, ServiceManagerException, InterruptedException {
		RuntimeConfig serverConfig = TestConfig.prepareConfig();

		try (Server server = new Server(serverConfig)) {
			Thread serverThread = server.start(false);
			ServiceInfo serviceInfo = server.registerService(receiverClass.getName());

			server.startService(serviceInfo.id(), false);

			SyslogReceiver receiver = server.getService(receiverClass);

			SyslogConfig rfc3164Config = new SyslogConfig(UdpSyslogReceiverService.HOST, UdpSyslogReceiverService.PORT);

			Arrays.stream(options).forEach(rfc3164Config::addOption);
			testSyslogHandlerConfig(receiver, rfc3164Config);

			SyslogConfig rfc54244Config = new SyslogConfig(UdpSyslogReceiverService.HOST, UdpSyslogReceiverService.PORT)
					.setProtocol(SyslogProtocol.RFC5424);

			Arrays.stream(options).forEach(rfc54244Config::addOption);
			testSyslogHandlerConfig(receiver, rfc54244Config);
			server.requestStop();
			serverThread.join();
		} finally {
			TestConfig.discardConfig(serverConfig);
		}
	}

	private void testSyslogHandlerConfig(SyslogReceiver receiver, SyslogConfig config) throws InterruptedException {
		try (SyslogDestination destination = new SyslogDestination(config)) {
			SyslogHandler handler = new SyslogHandler(destination);
			String message = "Syslog message " + System.nanoTime();
			LogRecord logRecord = new LogRecord(LogLevel.LEVEL_NOTICE, message);

			handler.publish(logRecord);

			Assertions.assertEquals(message, receiver.pollMessage(config));
		}
	}

}
