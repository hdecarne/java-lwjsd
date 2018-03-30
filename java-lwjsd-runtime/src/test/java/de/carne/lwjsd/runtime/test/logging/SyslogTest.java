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
import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.logging.SyslogConfig;
import de.carne.lwjsd.runtime.logging.SyslogOption;
import de.carne.lwjsd.runtime.logging.SyslogProtocol;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;

/**
 * Test Syslog related classes.
 */
class SyslogTest {

	@Test
	void testSyslogConfig() {
		SyslogConfig config1 = new SyslogConfig("localhost").addOption(SyslogOption.TRANSPORT_TCP);
		SyslogConfig config2 = new SyslogConfig("localhost", 1234);
		SyslogConfig config3 = new SyslogConfig("localhost", 4321).addOption(SyslogOption.TRANSPORT_TCP_TLS);

		Assertions.assertEquals("tcp://localhost:514", config1.toString());
		Assertions.assertEquals("udp://localhost:1234", config2.toString());
		Assertions.assertEquals("tcp+tls://localhost:4321", config3.toString());

		Assertions.assertEquals("localhost", config2.host());
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

	@Test
	void testSyslogDestination() throws IOException, ServiceManagerException, InterruptedException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try (Server server = new Server(config)) {
			Thread serverThread = server.start(false);
			ServiceInfo syslogServiceInfo = server.registerService(SyslogReceiverService.class.getName());

			server.startService(syslogServiceInfo.id(), false);
			server.requestStop();
			serverThread.join();
		} finally {
			TestConfig.discardConfig(config);
		}
	}

}
