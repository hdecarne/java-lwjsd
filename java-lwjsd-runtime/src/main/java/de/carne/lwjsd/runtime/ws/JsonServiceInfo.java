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

import de.carne.check.Check;
import de.carne.check.Nullable;
import de.carne.lwjsd.api.ServiceInfo;
import de.carne.lwjsd.api.ServiceState;

/**
 * JSON wrapper for {@linkplain ServiceInfo}.
 */
public final class JsonServiceInfo {

	@Nullable
	private JsonServiceId id;
	@Nullable
	private ServiceState state;
	private boolean autoStartFlag;

	/**
	 * Constructs empty {@linkplain JsonServiceInfo} instance.
	 */
	public JsonServiceInfo() {
		// Nothing to do here
	}

	/**
	 * Constructs initialized {@linkplain JsonServiceInfo} instance.
	 *
	 * @param source the source object to use for initialization.
	 */
	public JsonServiceInfo(ServiceInfo source) {
		this.id = new JsonServiceId(source.id());
		this.state = source.state();
		this.autoStartFlag = source.autoStartFlag();
	}

	/**
	 * Sets {@code id}.
	 *
	 * @param id {@code id} attribute.
	 */
	public void setId(JsonServiceId id) {
		this.id = id;
	}

	/**
	 * Gets {@code id} attribute.
	 *
	 * @return {@code id} attribute.
	 */
	public JsonServiceId getId() {
		return Check.notNull(this.id);
	}

	/**
	 * Sets {@code state}.
	 *
	 * @param state {@code state} attribute.
	 */
	public void setState(ServiceState state) {
		this.state = state;
	}

	/**
	 * Gets {@code state} attribute.
	 *
	 * @return {@code state} attribute.
	 */
	public ServiceState getState() {
		return Check.notNull(this.state);
	}

	/**
	 * Sets {@code autoStartFlag}.
	 *
	 * @param autoStartFlag {@code autoStartFlag} attribute.
	 */
	public void setAutoStartFlag(boolean autoStartFlag) {
		this.autoStartFlag = autoStartFlag;
	}

	/**
	 * Gets {@code autoStartFlag} attribute.
	 *
	 * @return {@code autoStartFlag} attribute.
	 */
	public boolean getAutoStartFlag() {
		return this.autoStartFlag;
	}

	/**
	 * Convert JSON wrapper to source object:
	 *
	 * @return the transferred source object.
	 */
	public ServiceInfo toSource() {
		return new ServiceInfo(getId().toSource(), getState(), getAutoStartFlag());
	}

}
