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
package de.carne.lwjsd.runtime.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.carne.boot.check.Nullable;

class CopyStream extends InputStream {

	private final InputStream in;
	private final OutputStream out;

	public CopyStream(InputStream in, Path outPath) throws IOException {
		this.in = in;
		this.out = Files.newOutputStream(outPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
	}

	@Override
	public int read() throws IOException {
		int b = this.in.read();

		if (b != -1) {
			this.out.write(b);
		}
		return b;
	}

	@Override
	public int read(@Nullable byte[] buf, int off, int len) throws IOException {
		int read = this.in.read(buf, off, len);

		if (read != -1) {
			this.out.write(buf, off, read);
		}
		return read;
	}

	@Override
	public void close() throws IOException {
		this.out.close();
	}

}
