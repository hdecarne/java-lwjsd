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

import de.carne.lwjsd.api.ServiceManagerClientFailureException;
import de.carne.lwjsd.api.ServiceManagerException;
import de.carne.lwjsd.api.ServiceManagerInitializationFailureException;
import de.carne.lwjsd.api.ServiceManagerOperationFailureException;
import de.carne.lwjsd.api.ServiceManagerShutdownFailureException;
import de.carne.lwjsd.api.ServiceManagerStartupFailureException;

/**
 * Test {@linkplain ServiceManagerException} based classes.
 */
class ServiceManagerExceptionTest {

	@Test
	void testServiceManagerClientFailureException() {
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerClientFailureException(getClass().getName()).getMessage());

		Throwable cause = new IllegalStateException();

		Assertions.assertEquals(cause.getClass().getName(),
				new ServiceManagerClientFailureException(cause).getMessage());
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerClientFailureException(getClass().getName(), cause).getMessage());
	}

	@Test
	void testServiceManagerInitializationFailureException() {
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerInitializationFailureException(getClass().getName()).getMessage());

		Throwable cause = new IllegalStateException();

		Assertions.assertEquals(cause.getClass().getName(),
				new ServiceManagerInitializationFailureException(cause).getMessage());
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerInitializationFailureException(getClass().getName(), cause).getMessage());
	}

	@Test
	void testServiceManagerOperationFailureException() {
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerOperationFailureException(getClass().getName()).getMessage());

		Throwable cause = new IllegalStateException();

		Assertions.assertEquals(cause.getClass().getName(),
				new ServiceManagerOperationFailureException(cause).getMessage());
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerOperationFailureException(getClass().getName(), cause).getMessage());
	}

	@Test
	void testServiceManagerShutdownFailureException() {
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerShutdownFailureException(getClass().getName()).getMessage());

		Throwable cause = new IllegalStateException();

		Assertions.assertEquals(cause.getClass().getName(),
				new ServiceManagerShutdownFailureException(cause).getMessage());
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerShutdownFailureException(getClass().getName(), cause).getMessage());
	}

	@Test
	void testServiceManagerStartupFailureException() {
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerStartupFailureException(getClass().getName()).getMessage());

		Throwable cause = new IllegalStateException();

		Assertions.assertEquals(cause.getClass().getName(),
				new ServiceManagerStartupFailureException(cause).getMessage());
		Assertions.assertEquals(getClass().getName(),
				new ServiceManagerStartupFailureException(getClass().getName(), cause).getMessage());
	}

}
