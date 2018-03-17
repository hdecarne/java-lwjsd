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
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import de.carne.check.Check;
import de.carne.check.Nullable;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.runtime.server.Server;
import de.carne.util.Exceptions;

/**
 * This class defines the control REST interface provided by the LWJSD server runtime.
 */
@Path(ServiceManagerService.WEB_CONTEXT_PATH)
public class ServiceManagerService {

	@Context
	@Nullable
	private Application application;

	/**
	 * The base REST path (relative to baseControlUri).
	 */
	public static final String WEB_CONTEXT_PATH = "control/api";

	/**
	 * The Ping command path.
	 *
	 * @see #ping()
	 */
	public static final String PING_PATH = "ping";

	/**
	 * The RequestStop command path.
	 *
	 * @see #requestStop()
	 */
	public static final String REQUEST_STOP_PATH = "requestStop";

	/**
	 * Pings the server.
	 *
	 * @return {@linkplain StatusMessage} object containing the request status.
	 */
	@GET
	@Path(PING_PATH)
	@Produces(MediaType.APPLICATION_JSON)
	public StatusMessage ping() {
		return new StatusMessage();
	}

	/**
	 * Requests a server stop.
	 *
	 * @return {@linkplain StatusMessage} object containing the request status.
	 */
	@GET
	@Path(REQUEST_STOP_PATH)
	@Produces(MediaType.APPLICATION_JSON)
	public StatusMessage requestStop() {
		StatusMessage status = new StatusMessage();

		try {
			getServer().requestStop();
		} catch (ServiceManagerException e) {
			status.setStatusMessage(Exceptions.toString(e));
		}
		return status;
	}

	private Server getServer() {
		return Check.isInstanceOf(Check.notNull(this.application).getProperties().get(Server.class.getName()),
				Server.class);
	}

}
