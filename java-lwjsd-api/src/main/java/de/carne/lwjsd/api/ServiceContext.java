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
package de.carne.lwjsd.api;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;

/**
 * This interface defines the necessary functions for a {@linkplain Service} to interact with it's execution
 * environment.
 */
public interface ServiceContext {

	/**
	 * Adds a {@linkplain HttpHandler} for the given mapping.
	 *
	 * @param httpHandler the {@linkplain HttpHandler} to add.
	 * @param mapping the mapping to add.
	 * @throws ServiceManagerException if the mapping is invalid.
	 * @see org.glassfish.grizzly.http.server.ServerConfiguration#addHttpHandler(HttpHandler,
	 *      HttpHandlerRegistration...)
	 */
	void addHttpHandler(HttpHandler httpHandler, HttpHandlerRegistration... mapping) throws ServiceManagerException;

	/**
	 * Removes a previously added {@linkplain HttpHandler}.
	 *
	 * @param httpHandler the {@linkplain HttpHandler} to remove.
	 * @throws ServiceManagerException if the removal fails.
	 */
	void removeHttpHandler(HttpHandler httpHandler) throws ServiceManagerException;

	/**
	 * Locates a {@linkplain Service} instance of a specific type.
	 *
	 * @param <T> the requested service type.
	 * @param serviceClass the type of the {@linkplain Service} to locate.
	 * @return the located {@linkplain Service} instance.
	 * @throws ServiceManagerException if the service type is not available.
	 */
	<T extends Service> T getService(Class<T> serviceClass) throws ServiceManagerException;

}
