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

import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;

/**
 * Test {@linkplain Server} class.
 */
class ServerTest {

	@Test
	void testServerStartStop() throws IOException, ServiceManagerException, InterruptedException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try (Server server = new Server(config)) {
			Assertions.assertEquals(ServiceManagerState.CONFIGURED, server.queryStatus());

			server.start(true);

			Assertions.assertEquals(ServiceManagerState.RUNNING, server.queryStatus());

			server.requestStop();
			while (server.processRequest()) {
				server.sleep();
			}

			Assertions.assertEquals(ServiceManagerState.STOPPED, server.queryStatus());
		} finally {
			TestConfig.discardConfig(config);
		}
	}

}
