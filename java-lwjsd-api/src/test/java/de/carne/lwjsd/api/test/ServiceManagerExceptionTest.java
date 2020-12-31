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
package de.carne.lwjsd.api.test;

import java.text.MessageFormat;

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
		String exceptionMessageWithArgs = "Cause: ''{0}''";
		Throwable cause = new IllegalStateException();
		String exceptionMessageWithoutArgs = MessageFormat.format(exceptionMessageWithArgs, cause);

		Assertions.assertEquals(exceptionMessageWithoutArgs,
				new ServiceManagerException(exceptionMessageWithArgs, cause).getMessage());
		Assertions.assertEquals(exceptionMessageWithoutArgs,
				new ServiceManagerException(exceptionMessageWithoutArgs).getMessage());

		Assertions.assertEquals(cause.getClass().getName(), new ServiceManagerException(cause).getMessage());
		Assertions.assertEquals(exceptionMessageWithoutArgs,
				new ServiceManagerException(cause, exceptionMessageWithArgs, cause).getMessage());
		Assertions.assertEquals(exceptionMessageWithoutArgs,
				new ServiceManagerException(cause, exceptionMessageWithoutArgs).getMessage());

		ReasonMessage reasonMessage = new ReasonMessage(ReasonMessage.Reason.ILLEGAL_STATE, exceptionMessageWithArgs,
				cause);

		Assertions.assertEquals(reasonMessage.toString(),
				new ServiceManagerException(reasonMessage).getReasonMessage().toString());
		Assertions.assertEquals(reasonMessage.toString(),
				new ServiceManagerException(cause, reasonMessage).getReasonMessage().toString());
	}

}
