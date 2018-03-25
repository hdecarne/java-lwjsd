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
package de.carne.lwjsd.api;

import java.util.Objects;

import de.carne.check.Nullable;

/**
 * Unique identifier for a {@linkplain Service} instance.
 */
public final class ServiceId {

	private final String moduleName;
	private final String serviceName;

	/**
	 * Constructs a new {@linkplain ServiceId} instance.
	 *
	 * @param moduleName the name of the module providing the {@linkplain Service}.
	 * @param service the {@linkplain Service} represented by this id.
	 */
	public ServiceId(String moduleName, Service service) {
		this(moduleName, service.getClass().getName());
	}

	/**
	 * Constructs a new {@linkplain ServiceId} instance.
	 *
	 * @param moduleName the name of the module providing the {@linkplain Service}.
	 * @param serviceName the name of {@linkplain Service} represented by this id.
	 */
	public ServiceId(String moduleName, String serviceName) {
		this.moduleName = moduleName;
		this.serviceName = serviceName;
	}

	/**
	 * Gets the name of the module providing the {@linkplain Service}.
	 *
	 * @return the name of the module providing the {@linkplain Service}.
	 */
	public String moduleName() {
		return this.moduleName;
	}

	/**
	 * Gets the name of the {@linkplain Service}.
	 *
	 * @return the name of the {@linkplain Service}.
	 */
	public String serviceName() {
		return this.serviceName;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		boolean equal = false;

		if (this == obj) {
			equal = true;
		} else if (obj instanceof ServiceId) {
			ServiceId serviceId = (ServiceId) obj;

			equal = this.moduleName.equals(serviceId.moduleName) && this.serviceName.equals(serviceId.serviceName);
		}
		return equal;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.moduleName, this.serviceName);
	}

	@Override
	public String toString() {
		return ":" + this.moduleName + ":" + this.serviceName;
	}

}
