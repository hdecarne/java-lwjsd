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
package de.carne.lwjsd.api;

/**
 * The {@linkplain ServiceManager} class provides the central functions for service management.
 */
public interface ServiceManager {

	/**
	 * Query the current status of the {@linkplain ServiceManager}.
	 *
	 * @return the current status of the {@linkplain ServiceManager}.
	 * @throws ServiceManagerException if the state could not be retrieved.
	 * @see ServiceManagerState
	 */
	ServiceManagerState queryStatus() throws ServiceManagerException;

	/**
	 * Requests a stop of the {@linkplain ServiceManager} including all running services.
	 *
	 * @throws ServiceManagerException if the stop request fails.
	 */
	void requestStop() throws ServiceManagerException;

}
