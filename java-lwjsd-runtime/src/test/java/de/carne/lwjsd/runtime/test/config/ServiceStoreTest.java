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
package de.carne.lwjsd.runtime.test.config;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.runtime.config.RuntimeConfig;
import de.carne.lwjsd.runtime.config.ServiceStore;
import de.carne.lwjsd.runtime.test.TestConfig;

/**
 * Test {@linkplain ServiceStore} class.
 */
class ServiceStoreTest {

	@Test
	void testServiceStore() throws IOException {
		RuntimeConfig config = TestConfig.prepareConfig();

		try {
			ServiceStore serviceStore1 = ServiceStore.open(config);

			Assertions.assertEquals(0, serviceStore1.queryEntries().size());

			serviceStore1.registerService(Optional.empty(), getClass().getName(), false);

			Assertions.assertEquals(1, serviceStore1.queryEntries().size());
			Assertions.assertTrue(serviceStore1.queryEntry(getClass().getName()).isPresent());

			ServiceStore serviceStore2 = ServiceStore.open(config);

			Assertions.assertEquals(1, serviceStore2.queryEntries().size());
			Assertions.assertTrue(serviceStore2.queryEntry(getClass().getName()).isPresent());

			serviceStore2.registerService(Optional.empty(), getClass().getName(), false);

			Assertions.assertEquals(1, serviceStore2.queryEntries().size());
		} finally {
			TestConfig.discardConfig(config);
		}
	}

}
