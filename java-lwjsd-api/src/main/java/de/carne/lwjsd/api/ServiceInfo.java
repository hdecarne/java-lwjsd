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

/**
 * {@linkplain Service} status information.
 */
public final class ServiceInfo {

	private final ServiceId id;
	private final ServiceState state;
	private final boolean autoStartFlag;

	/**
	 * Constructs a new {@linkplain ServiceInfo} instance.
	 *
	 * @param id the id of the {@linkplain Service}.
	 * @param state the current state of the {@linkplain Service}.
	 * @param autoStartFlag whether the {@linkplain Service} is automatically started on server startup ({@code true})
	 *        or not ({@code false}).
	 */
	public ServiceInfo(ServiceId id, ServiceState state, boolean autoStartFlag) {
		this.id = id;
		this.state = state;
		this.autoStartFlag = autoStartFlag;
	}

	/**
	 * Gets the id of the {@linkplain Service}.
	 *
	 * @return the id of the {@linkplain Service}.
	 */
	public ServiceId id() {
		return this.id;
	}

	/**
	 * Gets the current state of the {@linkplain Service}.
	 *
	 * @return the current state of the {@linkplain Service}.
	 */
	public ServiceState state() {
		return this.state;
	}

	/**
	 * Gets the auto start flag of the {@linkplain Service}.
	 *
	 * @return the the auto start flag of the {@linkplain Service}.
	 */
	public boolean autoStartFlag() {
		return this.autoStartFlag;
	}

	@Override
	public String toString() {
		return id().toString() + " (" + this.state + ")";
	}

}
