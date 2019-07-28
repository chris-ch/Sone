/**
 * Sone - DebugInformationTest.kt - Copyright © 2019 David ‘Bombe’ Roden
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

package net.pterodactylus.sone.core

import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import kotlin.test.*

class DebugInformationTest {

	private val debugInformation = DebugInformation()

	@Test
	fun `new debug information has show version information set to off`() {
		assertThat(debugInformation.showVersionInformation, equalTo(false))
	}

	@Test
	fun `new debug information has show metrics set to off`() {
		assertThat(debugInformation.showMetrics, equalTo(false))
	}

}
