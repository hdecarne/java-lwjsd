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
package de.carne.lwjsd.runtime.logging;

import java.net.Socket;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import de.carne.check.Nullable;

/**
 *
 */
public class SyslogHandler extends Handler {

	@Nullable
	private Socket socket = null;

	@Override
	public void close() {

	}

	@Override
	public void flush() {
		// Nothing to do here
	}

	@Override
	public void publish(@Nullable LogRecord record) {
		LogRecord checkedRecord = record;

		if (checkedRecord != null) {

		}
	}

}
