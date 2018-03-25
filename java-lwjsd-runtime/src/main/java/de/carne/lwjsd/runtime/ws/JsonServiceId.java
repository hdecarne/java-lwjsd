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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.carne.check.Check;
import de.carne.check.Nullable;
import de.carne.lwjsd.api.ServiceId;

/**
 * JSON wrapper for {@linkplain ServiceId}.
 */
public final class JsonServiceId {

	@Nullable
	private String moduleName;
	@Nullable
	private String serviceName;

	/**
	 * Constructs empty {@linkplain JsonServiceId} instance.
	 */
	public JsonServiceId() {
		// Nothing to do here
	}

	/**
	 * Constructs initialized {@linkplain JsonServiceId} instance.
	 *
	 * @param source the source object to use for initialization.
	 */
	public JsonServiceId(ServiceId source) {
		this.moduleName = source.moduleName();
		this.serviceName = source.serviceName();
	}

	/**
	 * Constructs empty {@linkplain JsonServiceId} instance.
	 *
	 * @param moduleName the module name.
	 * @param serviceName the service name.
	 */
	public JsonServiceId(String moduleName, String serviceName) {
		this.moduleName = moduleName;
		this.serviceName = serviceName;
	}

	/**
	 * Sets {@code moduleName}.
	 *
	 * @param moduleName {@code moduleName} attribute.
	 */
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	/**
	 * Gets {@code moduleName} attribute.
	 *
	 * @return {@code moduleName} attribute.
	 */
	public String getModuleName() {
		return Check.notNull(this.moduleName);
	}

	/**
	 * Sets {@code serviceName}.
	 *
	 * @param serviceName {@code serviceName} attribute.
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Gets {@code serviceName} attribute.
	 *
	 * @return {@code serviceName} attribute.
	 */
	public String getServiceName() {
		return Check.notNull(this.serviceName);
	}

	/**
	 * Convert JSON wrapper to source object:
	 *
	 * @return the transferred source object.
	 */
	public ServiceId toSource() {
		return new ServiceId(getModuleName(), getServiceName());
	}

	@Override
	public String toString() {
		return ":" + this.moduleName + ":" + this.serviceName;
	}

	private static final Pattern PARAM_PATTERN = Pattern.compile("^:([^:]*):([^:]*+)$");

	/**
	 * Decode a {@linkplain JsonServiceId} instance from a param string.
	 *
	 * @param param the param string to decode.
	 * @return the decode {@linkplain JsonServiceId} instance.
	 */
	public static JsonServiceId valueOf(String param) {
		Matcher matcher = PARAM_PATTERN.matcher(param);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid parameter: '" + param + "'");
		}
		return new JsonServiceId(matcher.group(1), matcher.group(2));
	}

}
