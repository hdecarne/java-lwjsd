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

/**
 * Operational states of a {@linkplain Service} module.
 */
public enum ModuleState {

	/**
	 * {@linkplain Service} module has been registered to the {@linkplain ServiceManager} but has not yet been loaded.
	 */
	REGISTERED,

	/**
	 * {@linkplain Service} module has been loaded and any provided {@linkplain Service} has been registered.
	 */
	LOADED

}
