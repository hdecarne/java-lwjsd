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

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceManagerInfo;
import de.carne.lwjsd.api.ServiceManagerState;

/**
 * JSON wrapper for {@linkplain ServiceManagerInfo}.
 */
public final class JsonServiceManagerInfo {

	@Nullable
	private URI baseUri = null;
	@Nullable
	private ServiceManagerState state = null;
	@Nullable
	private Collection<JsonModuleInfo> moduleInfos = null;
	@Nullable
	private Collection<JsonServiceInfo> serviceInfos = null;

	/**
	 * Constructs empty {@linkplain JsonServiceManagerInfo} instance.
	 */
	public JsonServiceManagerInfo() {
		// Nothing to do here
	}

	/**
	 * Constructs initialized {@linkplain JsonServiceManagerInfo} instance.
	 *
	 * @param source the source object to use for initialization.
	 */
	public JsonServiceManagerInfo(ServiceManagerInfo source) {
		this.baseUri = source.baseUri();
		this.state = source.state();
		this.moduleInfos = source.moduleInfos().stream().map(JsonModuleInfo::new).collect(Collectors.toList());
		this.serviceInfos = source.serviceInfos().stream().map(JsonServiceInfo::new).collect(Collectors.toList());
	}

	/**
	 * Sets {@code baseUri}.
	 *
	 * @param baseUri {@code baseUri} attribute.
	 */
	public void setBaseUri(URI baseUri) {
		this.baseUri = baseUri;
	}

	/**
	 * Gets {@code baseUri} attribute.
	 *
	 * @return {@code baseUri} attribute.
	 */
	public URI getBaseUri() {
		return Objects.requireNonNull(this.baseUri);
	}

	/**
	 * Sets {@code state}.
	 *
	 * @param state {@code state} attribute.
	 */
	public void setState(ServiceManagerState state) {
		this.state = state;
	}

	/**
	 * Gets {@code state} attribute.
	 *
	 * @return {@code state} attribute.
	 */
	public ServiceManagerState getState() {
		return Objects.requireNonNull(this.state);
	}

	/**
	 * Sets {@code moduleInfos}.
	 *
	 * @param moduleInfos {@code moduleInfos} attribute.
	 */
	public void setModuleInfos(Collection<JsonModuleInfo> moduleInfos) {
		this.moduleInfos = moduleInfos;
	}

	/**
	 * Gets {@code moduleInfos} attribute.
	 *
	 * @return {@code moduleInfos} attribute.
	 */
	public Collection<JsonModuleInfo> getModuleInfos() {
		return Objects.requireNonNull(this.moduleInfos);
	}

	/**
	 * Sets {@code serviceInfos}.
	 *
	 * @param serviceInfos {@code serviceInfos} attribute.
	 */
	public void setServiceInfos(Collection<JsonServiceInfo> serviceInfos) {
		this.serviceInfos = serviceInfos;
	}

	/**
	 * Gets {@code serviceInfos} attribute.
	 *
	 * @return {@code serviceInfos} attribute.
	 */
	public Collection<JsonServiceInfo> getServiceInfos() {
		return Objects.requireNonNull(this.serviceInfos);
	}

	/**
	 * Convert JSON wrapper to source object:
	 *
	 * @return the transferred source object.
	 */
	public ServiceManagerInfo toSource() {
		Collection<ModuleInfo> moduleInfoSources = getModuleInfos().stream().map(JsonModuleInfo::toSource)
				.collect(Collectors.toList());
		Collection<ServiceInfo> serviceInfoSources = getServiceInfos().stream().map(JsonServiceInfo::toSource)
				.collect(Collectors.toList());

		return new ServiceManagerInfo(getBaseUri(), getState(), moduleInfoSources, serviceInfoSources);
	}

}
