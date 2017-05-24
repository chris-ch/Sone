package net.pterodactylus.sone.web.pages

import net.pterodactylus.sone.utils.isPOST
import net.pterodactylus.sone.utils.parameters
import net.pterodactylus.sone.web.WebInterface
import net.pterodactylus.sone.web.page.FreenetRequest
import net.pterodactylus.util.template.Template
import net.pterodactylus.util.template.TemplateContext

/**
 * Page that lets the user like [net.pterodactylus.sone.data.Post]s and [net.pterodactylus.sone.data.Reply]s.
 */
class LikePage(template: Template, webInterface: WebInterface)
	: SoneTemplatePage("like.html", template, "Page.Like.Title", webInterface, true) {

	override fun handleRequest(request: FreenetRequest, templateContext: TemplateContext) {
		if (request.isPOST) {
			getCurrentSone(request.toadletContext)!!.let { currentSone ->
				request.parameters["type", 16]?.also { type ->
					when(type) {
						"post" -> currentSone.addLikedPostId(request.parameters["post", 36]!!)
						"reply" -> currentSone.addLikedReplyId(request.parameters["reply", 36]!!)
					}
				}
				throw RedirectException(request.parameters["returnPage", 256]!!)
			}
		}
	}

}