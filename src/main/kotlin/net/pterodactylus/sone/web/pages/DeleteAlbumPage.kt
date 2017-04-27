package net.pterodactylus.sone.web.pages

import net.pterodactylus.sone.utils.isPOST
import net.pterodactylus.sone.web.pages.SoneTemplatePage
import net.pterodactylus.sone.web.WebInterface
import net.pterodactylus.sone.web.page.FreenetRequest
import net.pterodactylus.util.template.Template
import net.pterodactylus.util.template.TemplateContext

/**
 * Page that lets the user delete an {@link Album}.
 */
class DeleteAlbumPage(template: Template, webInterface: WebInterface):
		SoneTemplatePage("deleteAlbum.html", template, "Page.DeleteAlbum.Title", webInterface, true) {

	override fun handleRequest(request: FreenetRequest, templateContext: TemplateContext) {
		if (request.isPOST) {
			val album = webInterface.core.getAlbum(request.httpRequest.getPartAsStringFailsafe("album", 36)) ?: throw RedirectException("invalid.html")
			if (!album.sone.isLocal) {
				throw RedirectException("noPermission.html")
			}
			if (request.httpRequest.getPartAsStringFailsafe("abortDelete", 4) == "true") {
				throw RedirectException("imageBrowser.html?album=${album.id}")
			}
			webInterface.core.deleteAlbum(album)
			throw RedirectException(if (album.parent.isRoot) "imageBrowser.html?sone=${album.sone.id}" else "imageBrowser.html?album=${album.parent.id}")
		}
		val album = webInterface.core.getAlbum(request.httpRequest.getParam("album"))
		templateContext["album"] = album ?: throw RedirectException("invalid.html")
	}

}
