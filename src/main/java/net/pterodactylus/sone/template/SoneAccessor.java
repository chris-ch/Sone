/*
 * Sone - SoneAccessor.java - Copyright © 2010 David Roden
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

package net.pterodactylus.sone.template;

import net.pterodactylus.sone.data.Profile;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.util.template.Accessor;
import net.pterodactylus.util.template.DataProvider;
import net.pterodactylus.util.template.ReflectionAccessor;

/**
 * {@link Accessor} for {@link Sone}s that adds a couple of properties to Sones.
 * <dl>
 * <dt>niceName</dt>
 * <dd>Will show a combination of first name, middle name, and last name, if
 * available, otherwise the username of the Sone is returned.</dd>
 * <dt>isFriend</dt>
 * <dd>Will return {@code true} if the sone in question is a friend of the
 * currently logged in Sone (as determined by accessing the “currentSone”
 * variable of the given {@link DataProvider}).</dd>
 * <dt>isCurrent</dt>
 * <dd>Will return {@code true} if the sone in question is the currently logged
 * in Sone.</dd>
 * </dl>
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneAccessor extends ReflectionAccessor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object get(DataProvider dataProvider, Object object, String member) {
		Sone sone = (Sone) object;
		if (member.equals("niceName")) {
			return getNiceName(sone);
		} else if (member.equals("isFriend")) {
			Sone currentSone = (Sone) dataProvider.getData("currentSone");
			return currentSone.hasFriend(sone) ? true : null;
		} else if (member.equals("isCurrent")) {
			Sone currentSone = (Sone) dataProvider.getData("currentSone");
			return currentSone.equals(sone);
		} else if (member.equals("isBlocked")) {
			Sone currentSone = (Sone) dataProvider.getData("currentSone");
			return currentSone.isSoneBlocked(sone.getId());
		}
		return super.get(dataProvider, object, member);
	}

	//
	// STATIC METHODS
	//

	/**
	 * Returns the nice name of the given Sone.
	 *
	 * @param sone
	 *            The Sone to get the nice name for
	 * @return The nice name of the Sone
	 */
	public static String getNiceName(Sone sone) {
		Profile profile = sone.getProfile();
		String firstName = profile.getFirstName();
		String middleName = profile.getMiddleName();
		String lastName = profile.getLastName();
		if (firstName == null) {
			if (lastName == null) {
				return sone.getName();
			}
			return lastName;
		}
		return firstName + ((middleName != null) ? " " + middleName : "") + ((lastName != null) ? " " + lastName : "");
	}

}
