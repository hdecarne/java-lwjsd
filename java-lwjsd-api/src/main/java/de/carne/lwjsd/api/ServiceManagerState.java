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
 * Operational states of {@linkplain ServiceManager}.
 */
public enum ServiceManagerState {

	/**
	 * {@linkplain ServiceManager} has been constructed but has not yet been started.
	 */
	CONFIGURED,

	/**
	 * {@linkplain ServiceManager} is currently starting and not yet fully operational.
	 */
	STARTING,

	/**
	 * {@linkplain ServiceManager} is running and fully operational.
	 */
	RUNNING,

	/**
	 * {@linkplain ServiceManager} is shutting down and no longer fully operational.
	 */
	STOPPING,

	/**
	 * {@linkplain ServiceManager} has been stopped and is no longer accessible.
	 */
	STOPPED

}
