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
 * {@linkplain Service} module status information.
 */
public final class ModuleInfo {

	private final String name;
	private final ModuleState state;

	/**
	 * Constructs a new {@linkplain ModuleInfo} instance.
	 *
	 * @param name the name of the {@linkplain Service} module.
	 * @param state the current state of the {@linkplain Service} module.
	 */
	public ModuleInfo(String name, ModuleState state) {
		this.name = name;
		this.state = state;
	}

	/**
	 * Gets the name of the {@linkplain Service} module.
	 *
	 * @return the name of the {@linkplain Service} module.
	 */
	public String name() {
		return this.name;
	}

	/**
	 * Gets the current state of the {@linkplain Service} module.
	 *
	 * @return the current state of the {@linkplain Service} module.
	 */
	public ModuleState state() {
		return this.state;
	}

	@Override
	public String toString() {
		return ":" + this.name + " (" + this.state + ")";
	}

}
