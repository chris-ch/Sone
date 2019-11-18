/**
 * Sone - Translation.kt - Copyright © 2019 David ‘Bombe’ Roden
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

package net.pterodactylus.sone.freenet

import java.util.*

/**
 * Facade for Fred’s [freenet.l10n.BaseL10n] object.
 */
interface Translation {

	/** The currently selected locale. */
	val currentLocale: Locale

	/**
	 * Returns the translated string for the given key, defaulting to `""`.
	 *
	 * @param key The key to return the translated string for
	 * @return The translated string, or `""` if there is no translation
	 */
	fun translate(key: String): String

}
