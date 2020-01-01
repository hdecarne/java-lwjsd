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
package de.carne.lwjsd.api.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ReasonMessage;

/**
 * Test {@linkplain ReasonMessage} class.
 */
class ReasonMessageTest {

	@Test
	void testReasonMessage() {
		ReasonMessage generalFailure = ReasonMessage.generalFailure("General {0}", "failure");
		ReasonMessage illegalArgument = ReasonMessage.illegalArgument("Illegal {0}", "argument");
		ReasonMessage illegalState = ReasonMessage.illegalState("Illegal {0}", "state");

		testReasonMessage(generalFailure, ReasonMessage.Reason.GENERAL_FAILURE, "General failure");
		testReasonMessage(illegalArgument, ReasonMessage.Reason.ILLEGAL_ARGUMENT, "Illegal argument");
		testReasonMessage(illegalState, ReasonMessage.Reason.ILLEGAL_STATE, "Illegal state");
	}

	private void testReasonMessage(ReasonMessage reference, ReasonMessage.Reason reason, String message) {
		Assertions.assertEquals(reference.reason(), reason);
		Assertions.assertEquals(reference.message(), message);

		ReasonMessage reasonMessage = new ReasonMessage(reason, message);

		Assertions.assertEquals(reason, reasonMessage.reason());
		Assertions.assertEquals(message, reasonMessage.message());
		Assertions.assertTrue(reasonMessage.toString().startsWith(reason.toString()));
	}

}
