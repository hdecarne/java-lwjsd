/*
 * Copyright (c) 2018-2019 Holger de Carne and contributors, All Rights Reserved.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@linkplain ServiceManager} status information.
 */
public final class ServiceManagerInfo {

	private final URI baseUri;
	private final ServiceManagerState state;
	private final List<ModuleInfo> moduleInfos;
	private final List<ServiceInfo> serviceInfos;

	/**
	 * Constructs a new {@linkplain ServiceManagerInfo} instance.
	 *
	 * @param baseUri the base {@linkplain URI} of the {@linkplain ServiceManager}.
	 * @param state the current state of the {@linkplain ServiceManager}.
	 * @param moduleInfos the status informations of the registered {@linkplain Service} modules.
	 * @param serviceInfos the status informations of the registered {@linkplain Service}s.
	 */
	public ServiceManagerInfo(URI baseUri, ServiceManagerState state, Collection<ModuleInfo> moduleInfos,
			Collection<ServiceInfo> serviceInfos) {
		this.baseUri = baseUri;
		this.state = state;
		this.moduleInfos = new ArrayList<>(moduleInfos);
		this.serviceInfos = new ArrayList<>(serviceInfos);
	}

	/**
	 * Gets the base {@linkplain URI} of the {@linkplain ServiceManager}.
	 *
	 * @return the base {@linkplain URI} of the {@linkplain ServiceManager}.
	 */
	public URI baseUri() {
		return this.baseUri;
	}

	/**
	 * Gets the current state of the {@linkplain ServiceManager}.
	 *
	 * @return the current state of the {@linkplain ServiceManager}.
	 */
	public ServiceManagerState state() {
		return this.state;
	}

	/**
	 * Gets the status informations of the registered {@linkplain Service} modules.
	 *
	 * @return the status informations of the registered {@linkplain Service} modules.
	 */
	public Collection<ModuleInfo> moduleInfos() {
		return Collections.unmodifiableList(this.moduleInfos);
	}

	/**
	 * Gets the status informations of the registered {@linkplain Service}s.
	 *
	 * @return the status informations of the registered {@linkplain Service}s.
	 */
	public Collection<ServiceInfo> serviceInfos() {
		return Collections.unmodifiableList(this.serviceInfos);
	}

	@Override
	public String toString() {
		return this.baseUri + " (" + this.state + ")";
	}

}
