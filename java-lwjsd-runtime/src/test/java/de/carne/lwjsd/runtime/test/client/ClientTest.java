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
package de.carne.lwjsd.runtime.test.client;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.api.ServiceState;
import de.carne.lwjsd.runtime.client.Client;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;
import de.carne.lwjsd.runtime.test.services.TestService;

/**
 * Test {@linkplain Client} class.
 */
class ClientTest {

	@Test
	void testClientWithoutServer() throws IOException, ServiceManagerException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try (Client client = new Client(config)) {
			Assertions.assertThrows(ServiceManagerException.class, () -> {
				client.connect();
			});
		} finally {
			TestConfig.discardConfig(config);
		}
	}

	@Test
	void testClientWithServer() throws IOException, ServiceManagerException, InterruptedException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try (Server server = new Server(config); Client client = new Client(config)) {
			// Server start & connect
			server.start(false);
			client.connect();

			// Server status
			ServiceManagerInfo status1 = client.queryStatus();

			Assertions.assertEquals(ServiceManagerState.RUNNING, status1.state());
			Assertions.assertEquals(0, status1.moduleInfos().size());
			Assertions.assertEquals(1, status1.serviceInfos().size());

			// Module management
			client.registerModule(TestConfig.TEST_SERVICES_MODULE, false);

			ServiceManagerInfo status2 = client.queryStatus();

			Assertions.assertEquals(1, status2.moduleInfos().size());
			Assertions.assertEquals(2, status2.serviceInfos().size());

			Assertions.assertThrows(ServiceManagerException.class, () -> {
				client.registerModule(TestConfig.TEST_SERVICES_MODULE, false);
			});
			client.registerModule(TestConfig.TEST_SERVICES_MODULE, true);

			ServiceManagerInfo status3 = client.queryStatus();

			Assertions.assertEquals(1, status3.moduleInfos().size());
			Assertions.assertEquals(2, status3.serviceInfos().size());

			ModuleInfo moduleInfo = status3.moduleInfos().iterator().next();

			client.deleteModule(moduleInfo.name());

			ServiceManagerInfo status4 = client.queryStatus();

			Assertions.assertEquals(0, status4.moduleInfos().size());
			Assertions.assertEquals(1, status4.serviceInfos().size());

			// Service management
			Assertions.assertThrows(ServiceManagerException.class, () -> {
				client.startService(new ServiceId("", "unknown"), false);
			});

			ServiceInfo serviceInfo1 = client.registerService(TestService.class.getName());

			Assertions.assertEquals(ServiceState.REGISTERED, serviceInfo1.state());
			Assertions.assertEquals(ServiceState.RUNNING, client.startService(serviceInfo1.id(), false).state());
			Assertions.assertEquals(ServiceState.LOADED, client.stopService(serviceInfo1.id()).state());

			// Server stop
			client.requestStop();
			server.getServerThread().join();

			ServiceManagerInfo status6 = server.queryStatus();

			Assertions.assertEquals(ServiceManagerState.STOPPED, status6.state());
		} finally {
			TestConfig.discardConfig(config);
		}
	}

}
