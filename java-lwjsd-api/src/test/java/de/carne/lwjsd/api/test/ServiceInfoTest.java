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

import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceState;

/**
 * Test {@linkplain ServiceInfo} class.
 */
class ServiceInfoTest {

	@Test
	void testServiceInfo() {
		ServiceId serviceId = new ServiceId("moduleName", "serviceName");
		ServiceInfo serviceInfo = new ServiceInfo(serviceId, ServiceState.REGISTERED, true);

		Assertions.assertEquals(serviceId, serviceInfo.id());
		Assertions.assertEquals(ServiceState.REGISTERED, serviceInfo.state());
		Assertions.assertTrue(serviceInfo.autoStartFlag());
		Assertions.assertEquals(":moduleName:serviceName (REGISTERED)", serviceInfo.toString());
	}

}
