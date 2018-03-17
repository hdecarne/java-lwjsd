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
package de.carne.lwjsd.runtime.ws;

import de.carne.lwjsd.api.ServiceManagerState;

/**
 * Response object containing server status information.
 */
public class ServerStatus extends StatusMessage {

	private ServiceManagerState state = ServiceManagerState.CONFIGURED;

	/**
	 * Gets the current server state.
	 *
	 * @return the current server state.
	 * @see ServiceManagerState
	 */
	public ServiceManagerState getServerState() {
		return this.state;
	}

	/**
	 * Sets the current server state.
	 * 
	 * @param state the server state to set.
	 */
	public void setServerState(ServiceManagerState state) {
		this.state = state;
	}

}
