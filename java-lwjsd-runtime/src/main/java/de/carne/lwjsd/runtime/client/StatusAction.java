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
package de.carne.lwjsd.runtime.client;

import java.io.PrintStream;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInfo;

/**
 * Query and display server status.
 */
public class StatusAction implements ClientAction {

	private final PrintStream out;

	/**
	 * Constructs new {@linkplain StatusAction} instance.
	 *
	 * @param out the {@linkplain PrintStream} to print the status info to.
	 */
	public StatusAction(PrintStream out) {
		this.out = out;
	}

	@Override
	public int invoke(Client client) throws ServiceManagerException {
		ServiceManagerInfo status = client.queryStatus();

		this.out.println("[Server]");
		this.out.println(" baseUri: " + status.baseUri());
		this.out.println(" state  : " + status.state());
		for (ModuleInfo moduleInfo : status.moduleInfos()) {
			this.out.println("[Module]");
			this.out.println(" name   :" + moduleInfo.name());
			this.out.println(" version:" + moduleInfo.version());
			this.out.println(" state  :" + moduleInfo.version());
		}
		for (ServiceInfo serviceInfo : status.serviceInfos()) {
			this.out.println("[Service]");
			this.out.println(" id       :" + serviceInfo.id());
			this.out.println(" autoStart:" + serviceInfo.autoStartFlag());
			this.out.println(" state    :" + serviceInfo.state());
		}
		return 0;
	}

}
