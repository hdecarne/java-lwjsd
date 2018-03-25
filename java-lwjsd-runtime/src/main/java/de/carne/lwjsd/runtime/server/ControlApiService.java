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

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import de.carne.check.Check;
import de.carne.check.Nullable;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.runtime.ws.ControlApi;
import de.carne.lwjsd.runtime.ws.JsonServiceId;
import de.carne.lwjsd.runtime.ws.JsonServiceManagerInfo;
import de.carne.util.ManifestInfos;

class ControlApiService implements ControlApi {

	@Context
	@Nullable
	private Application application;

	@Override
	public String version() {
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
	public void loadModule(String moduleName) throws ServiceManagerException {
		getServer().loadModule(moduleName);
	}

	@Override
	public void deleteModule(String moduleName) throws ServiceManagerException {
		getServer().deleteModule(moduleName);
	}

	@Override
	public JsonServiceId registerService(String className) throws ServiceManagerException {
		return new JsonServiceId(getServer().registerService(className));
	}

	@Override
	public void startService(JsonServiceId serviceId, boolean autoStart) throws ServiceManagerException {
		getServer().startService(serviceId.toSource(), autoStart);
	}

	@Override
	public void stopService(JsonServiceId serviceId) throws ServiceManagerException {
		getServer().stopService(serviceId.toSource());
	}

	private Server getServer() {
		return Check.isInstanceOf(Check.notNull(this.application).getProperties().get(Server.class.getName()),
				Server.class);
	}

}
