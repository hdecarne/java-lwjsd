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
package de.carne.lwjsd.api.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ReasonMessage;
import de.carne.lwjsd.api.ServiceManagerException;

/**
 * Test {@linkplain ServiceManagerException} class.
 */
class ServiceManagerExceptionTest {

	@Test
	void testServiceManagerException() {
		final String exceptionMessage = getClass().getName();

		Assertions.assertEquals(exceptionMessage, new ServiceManagerException(exceptionMessage).getMessage());

		Throwable cause = new IllegalStateException();

		Assertions.assertEquals(cause.getClass().getName(), new ServiceManagerException(cause).getMessage());
		Assertions.assertEquals(exceptionMessage, new ServiceManagerException(cause, exceptionMessage).getMessage());

		ReasonMessage reasonMessage = new ReasonMessage(ReasonMessage.Reason.ILLEGAL_STATE, exceptionMessage);

		Assertions.assertEquals(reasonMessage.toString(),
				new ServiceManagerException(reasonMessage).getReasonMessage().toString());
		Assertions.assertEquals(reasonMessage.toString(),
				new ServiceManagerException(cause, reasonMessage).getReasonMessage().toString());
	}

}
