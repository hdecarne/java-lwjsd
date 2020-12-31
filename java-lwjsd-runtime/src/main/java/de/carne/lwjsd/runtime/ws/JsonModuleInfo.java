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
package de.carne.lwjsd.runtime.ws;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ModuleState;

/**
 * JSON wrapper for {@linkplain ModuleInfo}.
 */
public final class JsonModuleInfo {

	@Nullable
	private String name;
	@Nullable
	private String version;
	@Nullable
	private ModuleState state;

	/**
	 * Constructs empty {@linkplain JsonModuleInfo} instance.
	 */
	public JsonModuleInfo() {
		// Nothing to do here
	}

	/**
	 * Constructs initialized {@linkplain JsonModuleInfo} instance.
	 *
	 * @param source the source object to use for initialization.
	 */
	public JsonModuleInfo(ModuleInfo source) {
		this.name = source.name();
		this.version = source.version();
		this.state = source.state();
	}

	/**
	 * Sets {@code name}.
	 *
	 * @param name {@code name} attribute.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets {@code name} attribute.
	 *
	 * @return {@code name} attribute.
	 */
	public String getName() {
		return Objects.requireNonNull(this.name);
	}

	/**
	 * Sets {@code version}.
	 *
	 * @param version {@code version} attribute.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Gets {@code version} attribute.
	 *
	 * @return {@code version} attribute.
	 */
	public String getVersion() {
		return Objects.requireNonNull(this.version);
	}

	/**
	 * Sets {@code state}.
	 *
	 * @param state {@code state} attribute.
	 */
	public void setState(ModuleState state) {
		this.state = state;
	}

	/**
	 * Gets {@code state} attribute.
	 *
	 * @return {@code state} attribute.
	 */
	public ModuleState getState() {
		return Objects.requireNonNull(this.state);
	}

	/**
	 * Convert JSON wrapper to source object:
	 *
	 * @return the transferred source object.
	 */
	public ModuleInfo toSource() {
		return new ModuleInfo(getName(), getVersion(), getState());
	}

}
