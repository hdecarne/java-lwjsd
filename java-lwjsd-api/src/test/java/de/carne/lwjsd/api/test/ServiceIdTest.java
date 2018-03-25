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
package de.carne.lwjsd.api.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.api.ServiceContext;
import de.carne.lwjsd.api.ServiceException;
import de.carne.lwjsd.api.ServiceId;

/**
 * Test {@linkplain ServiceId} class.
 */
class ServiceIdTest {

	class TestService implements Service {

		@Override
		public void load(ServiceContext context) throws ServiceException {
			// Nothing to do
		}

		@Override
		public void start(ServiceContext context) throws ServiceException {
			// Nothing to do
		}

		@Override
		public void stop(ServiceContext context) throws ServiceException {
			// Nothing to do
		}

		@Override
		public void unload(ServiceContext context) throws ServiceException {
			// Nothing to do
		}

	}

	@Test
	void testServiceId() {
		ServiceId serviceId1 = new ServiceId("moduleName", new TestService());

		Assertions.assertEquals("moduleName", serviceId1.moduleName());
		Assertions.assertEquals(TestService.class.getName(), serviceId1.serviceName());

		ServiceId serviceId2 = new ServiceId(serviceId1.moduleName(), serviceId1.serviceName());

		Assertions.assertEquals(serviceId1, serviceId2);
		Assertions.assertNotEquals(serviceId1, this);
		Assertions.assertEquals(serviceId1.hashCode(), serviceId2.hashCode());
	}

}
