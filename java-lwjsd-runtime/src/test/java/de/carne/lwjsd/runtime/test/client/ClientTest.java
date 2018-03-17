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
package de.carne.lwjsd.runtime.test.client;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerState;
import de.carne.lwjsd.runtime.client.Client;
import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.lwjsd.runtime.test.TestConfig;

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
			server.start(false);
			client.connect();

			Assertions.assertEquals(ServiceManagerState.RUNNING, client.queryStatus());

			client.requestStop();
			server.getServerThread().join(1000);

			Assertions.assertEquals(ServiceManagerState.STOPPED, server.queryStatus());
		} finally {
			TestConfig.discardConfig(config);
		}
	}

}
