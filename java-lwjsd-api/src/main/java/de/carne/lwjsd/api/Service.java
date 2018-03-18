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
 * A {@linkplain Service} represents a self-contained server application which can be executed by the
 * {@linkplain ServiceManager}. This interface defines necessary functions for controlling a {@linkplain Service}
 * instance's state within the {@linkplain ServiceManager}.
 */
public interface Service {

	/**
	 * Performs any necessary per {@linkplain Service} setup tasks after it has been instantiated by the
	 * {@linkplain ServiceManager}.
	 *
	 * @param serviceManager the executing {@linkplain ServiceManager}.
	 * @throws ServiceException if an error occurs while loading the {@linkplain Service}.
	 */
	void load(ServiceManager serviceManager) throws ServiceException;

	/**
	 * Acquires any necessary resource for {@linkplain Service} execution and starts the {@linkplain Service}.
	 *
	 * @param serviceManager the executing {@linkplain ServiceManager}.
	 * @throws ServiceException if an error occurs while starting the {@linkplain Service}.
	 */
	void start(ServiceManager serviceManager) throws ServiceException;

	/**
	 * Stops the {@linkplain Service} and releases any execution related resources.
	 *
	 * @param serviceManager the executing {@linkplain ServiceManager}.
	 * @throws ServiceException if an error occurs while stopping the {@linkplain Service}.
	 */
	void stop(ServiceManager serviceManager) throws ServiceException;

	/**
	 * Performs any necessary shutdown tasks before the {@linkplain ServiceManager} discards this {@linkplain Service}
	 * instance.
	 *
	 * @param serviceManager the executing {@linkplain ServiceManager}.
	 * @throws ServiceException if an error occurs while unloading the {@linkplain Service}.
	 */
	void unload(ServiceManager serviceManager) throws ServiceException;

}
