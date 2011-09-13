/*
 * Sone - UnbookmarkAjaxPage.java - Copyright © 2011 David Roden
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

package net.pterodactylus.sone.web.ajax;

import net.pterodactylus.sone.web.WebInterface;
import net.pterodactylus.sone.web.page.FreenetRequest;
import net.pterodactylus.util.json.JsonObject;

/**
 * AJAX page that lets the user unbookmark a post.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class UnbookmarkAjaxPage extends JsonPage {

	/**
	 * Creates a new unbookmark AJAX page.
	 *
	 * @param webInterface
	 *            The Sone web interface
	 */
	public UnbookmarkAjaxPage(WebInterface webInterface) {
		super("unbookmark.ajax", webInterface);
	}

	//
	// JSONPAGE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JsonObject createJsonObject(FreenetRequest request) {
		String id = request.getHttpRequest().getParam("post", null);
		if ((id == null) || (id.length() == 0)) {
			return createErrorJsonObject("invalid-post-id");
		}
		webInterface.getCore().unbookmarkPost(id);
		return createSuccessJsonObject();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean requiresLogin() {
		return false;
	}

}
