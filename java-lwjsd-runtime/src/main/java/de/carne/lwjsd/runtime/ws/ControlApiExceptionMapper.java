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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.eclipse.jdt.annotation.Nullable;

import de.carne.boot.logging.Log;
import de.carne.boot.logging.LogLevel;
import de.carne.lwjsd.api.ReasonMessage;
import de.carne.lwjsd.api.ServiceManagerException;

/**
 * {@linkplain ExceptionMapper} implementation for making the error details available to the client.
 */
public class ControlApiExceptionMapper implements ExceptionMapper<Exception> {

	private static final Log LOG = new Log();

	/**
	 * Header used to mark response as a mapped exception.
	 */
	public static final String CONTROL_API_EXCEPTION_HEADER = "CONTROL_API_EXCEPTION";

	@Override
	public Response toResponse(@Nullable Exception exception) {
		ReasonMessage reasonMessage;

		if (exception instanceof ServiceManagerException) {
			reasonMessage = ((ServiceManagerException) exception).getReasonMessage();
		} else {
			reasonMessage = new ReasonMessage(ReasonMessage.Reason.GENERAL_FAILURE,
					"Server processing failed (see logs for details)");
		}

		Response.Status status;
		LogLevel logLevel;

		switch (reasonMessage.reason()) {
		case ILLEGAL_ARGUMENT:
			status = Response.Status.NOT_FOUND;
			logLevel = LogLevel.LEVEL_WARNING;
			break;
		case ILLEGAL_STATE:
			status = Response.Status.NOT_ACCEPTABLE;
			logLevel = LogLevel.LEVEL_WARNING;
			break;
		case GENERAL_FAILURE:
		default:
			status = Response.Status.INTERNAL_SERVER_ERROR;
			logLevel = LogLevel.LEVEL_ERROR;
		}

		LOG.log(logLevel, exception, "Server processing failed");

		return Response.status(status).header(CONTROL_API_EXCEPTION_HEADER, Boolean.TRUE)
				.entity(new JsonReasonMessage(reasonMessage)).encoding(MediaType.APPLICATION_JSON).build();
	}

}
