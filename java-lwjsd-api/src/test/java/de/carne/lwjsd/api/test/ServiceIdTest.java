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

	private class TestService1 implements Service {

		public TestService1() {
			// Nothing to do
		}

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

	private class TestService2 extends TestService1 {

		public TestService2() {
			// Nothing to do
		}

	}

	@Test
	void testServiceId() {
		ServiceId serviceId1 = new ServiceId("moduleName1", new TestService1());

		Assertions.assertEquals("moduleName1", serviceId1.moduleName());
		Assertions.assertEquals(TestService1.class.getName(), serviceId1.serviceName());

		ServiceId serviceId2 = new ServiceId(serviceId1.moduleName(), serviceId1.serviceName());

		Assertions.assertNotEquals(serviceId1, this);
		Assertions.assertEquals(serviceId1, serviceId2);
		Assertions.assertEquals(serviceId1.hashCode(), serviceId2.hashCode());

		ServiceId serviceId3 = new ServiceId("moduleName2", serviceId1.serviceName());

		Assertions.assertNotEquals(serviceId1, serviceId3);

		ServiceId serviceId4 = new ServiceId("moduleName2", new TestService2());

		Assertions.assertNotEquals(serviceId3, serviceId4);
	}

}
