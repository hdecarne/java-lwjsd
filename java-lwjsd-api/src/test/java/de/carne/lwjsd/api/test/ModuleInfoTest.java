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
package de.carne.lwjsd.api.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.carne.lwjsd.api.ModuleInfo;
import de.carne.lwjsd.api.ModuleState;

/**
 * Test {@linkplain ModuleInfo} class.
 */
class ModuleInfoTest {

	@Test
	void testModuleInfo() {
		ModuleInfo moduleInfo = new ModuleInfo("moduleName", "1.2.3", ModuleState.REGISTERED);

		Assertions.assertEquals("moduleName", moduleInfo.name());
		Assertions.assertEquals("1.2.3", moduleInfo.version());
		Assertions.assertEquals(ModuleState.REGISTERED, moduleInfo.state());
		Assertions.assertEquals(":moduleName-1.2.3 (REGISTERED)", moduleInfo.toString());
	}

}
