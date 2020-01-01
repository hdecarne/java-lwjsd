/*
 * Copyright (c) 2018-2020 Holger de Carne and contributors, All Rights Reserved.
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
package de.carne.lwjsd.runtime.test.services;

import org.junit.jupiter.api.Assertions;

import de.carne.lwjsd.api.Service;
import de.carne.lwjsd.api.ServiceContext;
import de.carne.lwjsd.api.ServiceException;
import de.carne.lwjsd.api.ServiceState;

/**
 * Test {@linkplain Service}.
 */
public class TestService implements Service {

	private ServiceState state = ServiceState.REGISTERED;

	/**
	 * Gets the current instance state.
	 *
	 * @return the current instance state.
	 */
	public ServiceState state() {
		return this.state;
	}

	@Override
	public void load(ServiceContext context) throws ServiceException {
		Assertions.assertEquals(ServiceState.REGISTERED, this.state);
		this.state = ServiceState.LOADED;
	}

	@Override
	public void start(ServiceContext context) throws ServiceException {
		Assertions.assertEquals(ServiceState.LOADED, this.state);
		this.state = ServiceState.RUNNING;
	}

	@Override
	public void stop(ServiceContext context) throws ServiceException {
		Assertions.assertEquals(ServiceState.RUNNING, this.state);
		this.state = ServiceState.LOADED;
	}

	@Override
	public void unload(ServiceContext context) throws ServiceException {
		Assertions.assertEquals(ServiceState.LOADED, this.state);
		this.state = ServiceState.REGISTERED;
	}

}
