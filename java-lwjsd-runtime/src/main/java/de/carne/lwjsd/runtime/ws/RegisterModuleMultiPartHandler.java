/*
 * Copyright (c) 2018-2020 Holger de Carne and contributors, All Rights Reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

/**
 * Helper class used to wrap the
 * {@linkplain ControlApi#registerModule(java.io.InputStream, FormDataContentDisposition, boolean)} parameters into a
 * {@linkplain FormDataMultiPart} instance.
 */
public final class RegisterModuleMultiPartHandler {

	private static final String FILE_PART_NAME = "file";
	private static final String FORCE_FIELD_NAME = "force";

	private RegisterModuleMultiPartHandler() {
		// prevent instantiation
	}

	/**
	 * Wrap {@linkplain ControlApi#registerModule(java.io.InputStream, FormDataContentDisposition, boolean)} parameters
	 * into a {@linkplain FormDataMultiPart} instance.
	 *
	 * @param multiPart the {@linkplain FormDataMultiPart} instance receiving the parameters.
	 * @param file the {@code file} parameter.
	 * @param force the {@code force} parameter.
	 * @return the wrapped parameters.
	 * @throws IOException if an I/O error occurs while accessing the submitted file.
	 */
	public static FormDataMultiPart fromSource(FormDataMultiPart multiPart, Path file, boolean force)
			throws IOException {
		FileDataBodyPart filePart = new FileDataBodyPart(FILE_PART_NAME, file.toFile(),
				MediaType.APPLICATION_OCTET_STREAM_TYPE);

		filePart.setContentDisposition(FormDataContentDisposition.name(FILE_PART_NAME)
				.fileName(file.getFileName().toString()).size(Files.size(file)).build());

		multiPart.bodyPart(filePart);
		multiPart.field(FORCE_FIELD_NAME, Boolean.valueOf(force), MediaType.APPLICATION_JSON_TYPE);
		multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
		return multiPart;
	}

}
