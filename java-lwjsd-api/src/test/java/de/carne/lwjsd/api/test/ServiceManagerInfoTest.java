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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.api.ServiceManagerState;

/**
 * Test {@linkplain ServiceManagerInfo} class.
 */
class ServiceManagerInfoTest {

	@Test
	void testServiceManagerInfo() throws URISyntaxException {
		URI baseUri = new URI("https://localhost:1234");
		Collection<ModuleInfo> moduleInfos = new ArrayList<>();
		Collection<ServiceInfo> serviceInfos = new ArrayList<>();
		ServiceManagerInfo serviceManagerInfo = new ServiceManagerInfo(baseUri, ServiceManagerState.CONFIGURED,
				moduleInfos, serviceInfos);

		Assertions.assertEquals(baseUri, serviceManagerInfo.baseUri());
		Assertions.assertEquals(ServiceManagerState.CONFIGURED, serviceManagerInfo.state());
		Assertions.assertEquals(0, serviceManagerInfo.moduleInfos().size());
		Assertions.assertEquals(0, serviceManagerInfo.serviceInfos().size());
		Assertions.assertEquals(baseUri + " (CONFIGURED)", serviceManagerInfo.toString());
	}

}
