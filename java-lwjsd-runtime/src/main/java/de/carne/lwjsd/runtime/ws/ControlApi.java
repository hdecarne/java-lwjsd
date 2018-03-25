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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.carne.lwjsd.api.ServiceManager;
import de.carne.lwjsd.api.ServiceManagerException;

/**
 * REST interface for remote {@linkplain ServiceManager} access.
 */
@Path("control/api")
public interface ControlApi {

	/**
	 * Gets the version of the server side runtime.
	 *
	 * @return the version of the server side runtime.
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	String version();

	/**
	 * Queries the {@linkplain ServiceManager} status.
	 *
	 * @return the {@linkplain ServiceManager} status.
	 * @throws ServiceManagerException if an error occurs during the request.
	 */
	@GET
	@Path("queryStatus")
	@Produces(MediaType.APPLICATION_JSON)
	JsonServiceManagerInfo queryStatus() throws ServiceManagerException;

	/**
	 * Requests a server stop.
	 *
	 * @throws ServiceManagerException if an error occurs during the request.
	 */
	@GET
	@Path("requestStop")
	void requestStop() throws ServiceManagerException;

}
