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
package de.carne.lwjsd.runtime.test.server;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ModuleState;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.api.ServiceState;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;
import de.carne.lwjsd.runtime.test.services.TestService;

/**
 * Test {@linkplain Server} class.
 */
class ServerTest {

	@Test
	void testServer() throws IOException, ServiceManagerException, InterruptedException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try {
			testServerRun1(config);
			testServerRun2(config);
		} finally {
			TestConfig.discardConfig(config);
		}
	}

	private void testServerRun1(RuntimeConfig config) throws ServiceManagerException, InterruptedException {
		try (Server server = new Server(config)) {
			// Server start
			Assertions.assertEquals(ServiceManagerState.CONFIGURED, server.queryStatus().state());

			server.start(false);

			Assertions.assertEquals(ServiceManagerState.RUNNING, server.queryStatus().state());

			// Runtime service management
			ServiceInfo testServiceInfo = server.registerService(TestService.class.getName());

			server.startService(testServiceInfo.id(), true);

			TestService testService = server.getService(TestService.class);

			Assertions.assertEquals(ServiceState.RUNNING, testService.state());

			server.stopService(testServiceInfo.id());

			Assertions.assertEquals(ServiceState.LOADED, testService.state());

			// Module management
			ModuleInfo testServicesModule = server.registerModule(TestConfig.TEST_SERVICES_MODULE, false);

			Assertions.assertEquals(ModuleState.LOADED, testServicesModule.state());

			// Server stop
			server.requestStop();
			server.getServerThread().join();

			Assertions.assertEquals(ServiceState.REGISTERED, testService.state());
			Assertions.assertEquals(ServiceManagerState.STOPPED, server.queryStatus().state());
		}
	}

	private void testServerRun2(RuntimeConfig config) throws ServiceManagerException, InterruptedException {
		try (Server server = new Server(config)) {
			// Server start
			server.start(false);

			// Automatic restart of registered services
			ServiceManagerInfo serviceManagerInfo1 = server.queryStatus();

			Assertions.assertEquals(ServiceManagerState.RUNNING, serviceManagerInfo1.state());
			Assertions.assertEquals(1, serviceManagerInfo1.moduleInfos().size());
			Assertions.assertEquals(3, serviceManagerInfo1.serviceInfos().size());

			// Module deletion
			ModuleInfo moduleInfo = serviceManagerInfo1.moduleInfos().iterator().next();

			server.deleteModule(moduleInfo.name());

			Assertions.assertThrows(ServiceManagerException.class, () -> {
				server.deleteModule(moduleInfo.name());
			});

			ServiceManagerInfo serviceManagerInfo2 = server.queryStatus();

			Assertions.assertEquals(ServiceManagerState.RUNNING, serviceManagerInfo1.state());
			Assertions.assertEquals(0, serviceManagerInfo2.moduleInfos().size());
			Assertions.assertEquals(2, serviceManagerInfo2.serviceInfos().size());

			// Server stop
			server.requestStop();
			server.getServerThread().join();

			Assertions.assertEquals(ServiceManagerState.STOPPED, server.queryStatus().state());
		}
	}

}
