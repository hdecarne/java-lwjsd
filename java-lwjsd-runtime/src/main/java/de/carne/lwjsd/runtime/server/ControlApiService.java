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
package de.carne.lwjsd.runtime.server;

import java.io.InputStream;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import de.carne.boot.check.Check;
import de.carne.boot.check.Nullable;
import de.carne.lwjsd.api.ServiceId;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.runtime.ws.ControlApi;
import de.carne.lwjsd.runtime.ws.JsonModuleInfo;
import de.carne.lwjsd.runtime.ws.JsonServiceInfo;
import de.carne.lwjsd.runtime.ws.JsonServiceManagerInfo;
import de.carne.util.ManifestInfos;

class ControlApiService implements ControlApi {

	@Context
	@Nullable
	private Application application;

	@Override
	public String getVersion() {
		return ManifestInfos.APPLICATION_VERSION;
	}

	@Override
	public JsonServiceManagerInfo queryStatus() throws ServiceManagerException {
		return new JsonServiceManagerInfo(getServer().queryStatus());
	}

	@Override
	public void requestStop() throws ServiceManagerException {
		getServer().requestStop();
	}

	@Override
	public JsonModuleInfo registerModule(InputStream fileStream, FormDataContentDisposition fileDetails, boolean force)
			throws ServiceManagerException {
		return new JsonModuleInfo(getServer().receiveAndRegisterModule(fileStream, fileDetails.getFileName(), force));
	}

	@Override
	public JsonModuleInfo loadModule(String moduleName) throws ServiceManagerException {
		return new JsonModuleInfo(getServer().loadModule(moduleName));
	}

	@Override
	public void deleteModule(String moduleName) throws ServiceManagerException {
		getServer().deleteModule(moduleName);
	}

	@Override
	public JsonServiceInfo registerService(String className) throws ServiceManagerException {
		return new JsonServiceInfo(getServer().registerService(className));
	}

	@Override
	public JsonServiceInfo startService(String moduleName, String serviceName, boolean autoStart)
			throws ServiceManagerException {
		return new JsonServiceInfo(getServer().startService(new ServiceId(moduleName, serviceName), autoStart));
	}

	@Override
	public JsonServiceInfo stopService(String moduleName, String serviceName) throws ServiceManagerException {
		return new JsonServiceInfo(getServer().stopService(new ServiceId(moduleName, serviceName)));
	}

	private Server getServer() {
		return Check.isInstanceOf(Check.notNull(this.application).getProperties().get(Server.class.getName()),
				Server.class);
	}

}
