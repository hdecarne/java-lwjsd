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

import java.nio.file.Path;

/**
 * This interface provides the necessary functions for managing the {@linkplain Service} execution environment.
 */
public interface ServiceManager {

	/**
	 * Queries the status of this {@linkplain ServiceManager} instance.
	 *
	 * @return the status of this {@linkplain ServiceManager} instance.
	 * @throws ServiceManagerException if an error occurs while querying the {@linkplain ServiceManager} status.
	 */
	ServiceManagerInfo queryStatus() throws ServiceManagerException;

	/**
	 * Requests a stop of the {@linkplain ServiceManager} including all running {@linkplain Service}s.
	 *
	 * @throws ServiceManagerException if an error occurs while stopping the {@linkplain ServiceManager}.
	 */
	void requestStop() throws ServiceManagerException;

	/**
	 * Registers a new {@linkplain Service} module into this {@linkplain ServiceManager}.
	 *
	 * @param file the {@linkplain Service} module file to register.
	 * @param force whether to force unloading and overwriting of an already running {@linkplain Service} module with
	 *        the same name.
	 * @return the updated {@linkplain Service} module status.
	 * @throws ServiceManagerException if an error occurs while registering the {@linkplain Service} module.
	 */
	ModuleInfo registerModule(Path file, boolean force) throws ServiceManagerException;

	/**
	 * Loads an already registered {@linkplain Service} module and registers the provided {@linkplain Service}s.
	 *
	 * @param moduleName the name of the {@linkplain Service} module to load.
	 * @return the updated {@linkplain Service} module status.
	 * @throws ServiceManagerException if an error occurs while loading the {@linkplain Service} module.
	 */
	ModuleInfo loadModule(String moduleName) throws ServiceManagerException;

	/**
	 * Deletes an already registered {@linkplain Service} module.
	 *
	 * @param moduleName the name of the {@linkplain Service} module to delete.
	 * @throws ServiceManagerException if an error occurs while deleting the {@linkplain Service} module.
	 */
	void deleteModule(String moduleName) throws ServiceManagerException;

	/**
	 * Registers a {@linkplain Service} provided by the current runtime environment.
	 *
	 * @param className the name of the class providing the {@linkplain Service}.
	 * @return the updated {@linkplain Service} status.
	 * @throws ServiceManagerException if an error occurs while registering the {@linkplain Service}.
	 */
	ServiceInfo registerService(String className) throws ServiceManagerException;

	/**
	 * Starts a {@linkplain Service}.
	 *
	 * @param serviceId the id of the {@linkplain Service} to start.
	 * @param autoStart whether to always start the {@linkplain Service} on server start.
	 * @return the updated {@linkplain Service} status.
	 * @throws ServiceManagerException if an error occurs while starting the {@linkplain Service}.
	 */
	ServiceInfo startService(ServiceId serviceId, boolean autoStart) throws ServiceManagerException;

	/**
	 * Stops a {@linkplain Service}.
	 *
	 * @param serviceId the id of the {@linkplain Service} to stop.
	 * @return the updated {@linkplain Service} status.
	 * @throws ServiceManagerException if an error occurs while stopping the {@linkplain Service}.
	 */
	ServiceInfo stopService(ServiceId serviceId) throws ServiceManagerException;

}
