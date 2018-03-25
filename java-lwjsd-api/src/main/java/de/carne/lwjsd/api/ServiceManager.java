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
	 * @param file the file name of the {@linkplain Service} module to register.
	 * @param overwrite whether to overwrite an already registered {@linkplain Service} module with the same name.
	 * @return the registered module name.
	 * @throws ServiceManagerException if an error occurs while installing the {@linkplain Service} module.
	 */
	String registerModule(String file, boolean overwrite) throws ServiceManagerException;

	/**
	 * Loads an already registered {@linkplain Service} module and registers the provided {@linkplain Service}s.
	 *
	 * @param moduleName the name of the {@linkplain Service} module to load.
	 * @throws ServiceManagerException if an error occurs while loading the {@linkplain Service} module.
	 */
	void loadModule(String moduleName) throws ServiceManagerException;

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
	 * @return the registered service id.
	 * @throws ServiceManagerException if an error occurs while registering the {@linkplain Service}.
	 */
	ServiceId registerService(String className) throws ServiceManagerException;

	/**
	 * Starts a {@linkplain Service}.
	 *
	 * @param serviceId the id of the {@linkplain Service} to start.
	 * @param autoStart whether to always start the {@linkplain Service} on server start.
	 * @throws ServiceManagerException if an error occurs while starting the {@linkplain Service}.
	 */
	void startService(ServiceId serviceId, boolean autoStart) throws ServiceManagerException;

	/**
	 * Stops a {@linkplain Service}.
	 *
	 * @param serviceId the id of the {@linkplain Service} to stop.
	 * @throws ServiceManagerException if an error occurs while stopping the {@linkplain Service}.
	 */
	void stopService(ServiceId serviceId) throws ServiceManagerException;

}
