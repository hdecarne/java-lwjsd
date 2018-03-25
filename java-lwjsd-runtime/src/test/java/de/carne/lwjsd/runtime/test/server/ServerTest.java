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
package de.carne.lwjsd.runtime.test.server;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.api.ServiceState;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;
import de.carne.lwjsd.runtime.test.TestService;

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
			Assertions.assertEquals(ServiceManagerState.CONFIGURED, server.queryStatus().state());

			ServiceId testServiceId = server.registerService(TestService.class.getName());

			server.start(false);

			Assertions.assertEquals(ServiceManagerState.RUNNING, server.queryStatus().state());

			server.startService(testServiceId, true);

			TestService testService = server.getService(TestService.class);

			Assertions.assertEquals(ServiceState.RUNNING, testService.state());

			server.stopService(testServiceId);

			Assertions.assertEquals(ServiceState.LOADED, testService.state());

			server.requestStop();
			server.getServerThread().join();

			Assertions.assertEquals(ServiceState.REGISTERED, testService.state());
			Assertions.assertEquals(ServiceManagerState.STOPPED, server.queryStatus().state());
		}
	}

	private void testServerRun2(RuntimeConfig config) throws ServiceManagerException, InterruptedException {
		try (Server server = new Server(config)) {
			server.start(false);

			TestService testService = server.getService(TestService.class);

			Assertions.assertEquals(ServiceState.RUNNING, testService.state());

			server.requestStop();
			server.getServerThread().join();

			Assertions.assertEquals(ServiceState.REGISTERED, testService.state());
			Assertions.assertEquals(ServiceManagerState.STOPPED, server.queryStatus().state());
		}
	}

}
