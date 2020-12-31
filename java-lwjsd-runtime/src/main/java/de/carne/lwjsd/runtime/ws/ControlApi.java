/*
 * Copyright (c) 2018-2021 Holger de Carne and contributors, All Rights Reserved.
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

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.carne.lwjsd.api.Service;
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
	@Path("getVersion")
	@Produces(MediaType.TEXT_PLAIN)
	String getVersion();

	/**
	 * Queries the status of this {@linkplain ServiceManager} instance.
	 *
	 * @return the status of this {@linkplain ServiceManager} instance.
	 * @throws ServiceManagerException if an error occurs while querying the {@linkplain ServiceManager} status.
	 */
	@GET
	@Path("queryStatus")
	@Produces(MediaType.APPLICATION_JSON)
	JsonServiceManagerInfo queryStatus() throws ServiceManagerException;

	/**
	 * Requests a stop of the {@linkplain ServiceManager} including all running {@linkplain Service}s.
	 *
	 * @throws ServiceManagerException if an error occurs while stopping the {@linkplain ServiceManager}.
	 */
	@DELETE
	@Path("requestStop")
	void requestStop() throws ServiceManagerException;

	/**
	 * Registers a {@linkplain Service} module and makes it available on the server.
	 *
	 * @param fileStream the file stream providing module data access.
	 * @param fileDetails the {@linkplain FormDataContentDisposition} object providing module details access.
	 * @param force whether to force unloading and overwriting of an already running {@linkplain Service} module with
	 *        the same name.
	 * @return the updated {@linkplain Service} module status.
	 * @throws ServiceManagerException if an error occurs while registering the {@linkplain Service} module.
	 */
	@POST
	@Path("registerModule")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	JsonModuleInfo registerModule(@FormDataParam("file") InputStream fileStream,
			@FormDataParam("file") FormDataContentDisposition fileDetails, @FormDataParam("force") boolean force)
			throws ServiceManagerException;

	/**
	 * Loads an already registered {@linkplain Service} module and registers the provided {@linkplain Service}s.
	 *
	 * @param moduleName the name of the {@linkplain Service} module to load.
	 * @return the updated {@linkplain Service} module status.
	 * @throws ServiceManagerException if an error occurs while loading the {@linkplain Service} module.
	 */
	@PUT
	@Path("loadModule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	JsonModuleInfo loadModule(@QueryParam(value = "moduleName") String moduleName) throws ServiceManagerException;

	/**
	 * Deletes an already registered {@linkplain Service} module.
	 *
	 * @param moduleName the name of the {@linkplain Service} module to delete.
	 * @throws ServiceManagerException if an error occurs while deleting the {@linkplain Service} module.
	 */
	@DELETE
	@Path("deleteModule")
	@Consumes(MediaType.APPLICATION_JSON)
	void deleteModule(@QueryParam(value = "moduleName") String moduleName) throws ServiceManagerException;

	/**
	 * Registers a {@linkplain Service} provided by the runtime environment.
	 *
	 * @param className the name of the class providing the {@linkplain Service}.
	 * @return the updated {@linkplain Service} status.
	 * @throws ServiceManagerException if an error occurs while registering the {@linkplain Service}.
	 */
	@POST
	@Path("registerService")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	JsonServiceInfo registerService(@QueryParam(value = "className") String className) throws ServiceManagerException;

	/**
	 * Starts a previously registered {@linkplain Service}.
	 *
	 * @param moduleName the name of the module providing the {@linkplain Service} to start.
	 * @param serviceName the name of the {@linkplain Service} to start.
	 * @param autoStart whether to always start the {@linkplain Service} on server start.
	 * @return the updated {@linkplain Service} status.
	 * @throws ServiceManagerException if an error occurs while starting the {@linkplain Service}.
	 */
	@POST
	@Path("startService")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	JsonServiceInfo startService(@QueryParam(value = "moduleName") String moduleName,
			@QueryParam(value = "serviceName") String serviceName, @QueryParam(value = "autoStart") boolean autoStart)
			throws ServiceManagerException;

	/**
	 * Stops a {@linkplain Service}.
	 *
	 * @param moduleName the name of the module providing the {@linkplain Service} to start.
	 * @param serviceName the name of the {@linkplain Service} to start.
	 * @return the updated {@linkplain Service} status.
	 * @throws ServiceManagerException if an error occurs while stopping the {@linkplain Service}.
	 */
	@POST
	@Path("stopService")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	JsonServiceInfo stopService(@QueryParam(value = "moduleName") String moduleName,
			@QueryParam(value = "serviceName") String serviceName) throws ServiceManagerException;

}
