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
	 * @throws ServiceManagerException if an error occurs while querying the {@linkplain ServiceManager} status.
	 * @see ServiceManagerState
	 */
	ServiceManagerState queryStatus() throws ServiceManagerException;

	/**
	 * Requests a stop of the {@linkplain ServiceManager} including all running services.
	 *
	 * @throws ServiceManagerException if an error occurs while stopping the {@linkplain ServiceManager}.
	 */
	void requestStop() throws ServiceManagerException;

	/**
	 * Loads and optionally starts a {@linkplain Service} from the runtime's classpath.
	 * <p>
	 * This function requires that the requested {@linkplain Service} module is already contained in the runtime's
	 * classpath.
	 *
	 * @param className the name of the {@linkplain Service} class to deploy.
	 * @param start whether to load and start ({@code true}) or only load ({@code false}) the {@linkplain Service}
	 *        class.
	 * @throws ServiceManagerException if an error occurs while deploying the {@linkplain Service}.
	 * @see #startService(String)
	 */
	void deployService(String className, boolean start) throws ServiceManagerException;

	/**
	 * Loads and optionally starts a {@linkplain Service} from an external module.
	 *
	 * @param moduleName the name of the module providing the {@linkplain Service} related classes.
	 * @param className the name of the {@linkplain Service} class to deploy.
	 * @param start whether to load and start ({@code true}) or only load ({@code false}) the {@linkplain Service}
	 *        class.
	 * @throws ServiceManagerException if an error occurs while deploying the {@linkplain Service}.
	 * @see #startService(String)
	 */
	void deployService(String moduleName, String className, boolean start) throws ServiceManagerException;

	/**
	 * Starts a {@linkplain Service}.
	 *
	 * @param className the name of the {@linkplain Service} class to start.
	 * @throws ServiceManagerException if an error occurs while starting the {@linkplain Service}.
	 */
	void startService(String className) throws ServiceManagerException;

	/**
	 * Stops a {@linkplain Service}.
	 *
	 * @param className the name of the {@linkplain Service} class to stop.
	 * @throws ServiceManagerException if an error occurs while stoping the {@linkplain Service}.
	 */
	void stopService(String className) throws ServiceManagerException;

}
