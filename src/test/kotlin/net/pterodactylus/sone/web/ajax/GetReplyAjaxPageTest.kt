package net.pterodactylus.sone.web.ajax

import net.pterodactylus.sone.data.PostReply
import net.pterodactylus.sone.data.Sone
import net.pterodactylus.sone.test.getInstance
import net.pterodactylus.sone.test.mock
import net.pterodactylus.sone.test.whenever
import net.pterodactylus.sone.utils.asTemplate
import net.pterodactylus.sone.web.baseInjector
import net.pterodactylus.util.template.ReflectionAccessor
import net.pterodactylus.util.template.TemplateContextFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.Test

/**
 * Unit test for [GetReplyAjaxPage].
 */
class GetReplyAjaxPageTest : JsonPageTest("getReply.ajax", needsFormPassword = false) {

	private val templateContextFactory = TemplateContextFactory().apply {
		addAccessor(Any::class.java, ReflectionAccessor())
	}
	override val page: JsonPage by lazy { GetReplyAjaxPage(webInterface, templateContextFactory, "<%core>\n<%request>\n<%reply.text>\n<%currentSone>".asTemplate()) }

	@Test
	fun `request without reply id results in invalid-reply-id`() {
		assertThatJsonFailed("invalid-reply-id")
	}

	@Test
	fun `request with valid reply id results in reply json`() {
		val sone = mock<Sone>().apply { whenever(id).thenReturn("sone-id") }
		val reply = mock<PostReply>().apply {
			whenever(id).thenReturn("reply-id")
			whenever(this.sone).thenReturn(sone)
			whenever(postId).thenReturn("post-id")
			whenever(time).thenReturn(1000)
			whenever(text).thenReturn("reply text")
		}
		addReply(reply)
		addRequestParameter("reply", "reply-id")
		assertThatJsonIsSuccessful()
		assertThat(json["reply"]!!["id"].asText(), equalTo("reply-id"))
		assertThat(json["reply"]!!["soneId"].asText(), equalTo("sone-id"))
		assertThat(json["reply"]!!["postId"].asText(), equalTo("post-id"))
		assertThat(json["reply"]!!["time"].asLong(), equalTo(1000L))
		assertThat(json["reply"]!!["html"].asText(), equalTo(listOf(
				core.toString(),
				freenetRequest.toString(),
				"reply text",
				currentSone.toString()
		).joinToString("\n")))
	}

	@Test
	fun `page can be created by dependency injection`() {
	    assertThat(baseInjector.getInstance<GetReplyAjaxPage>(), notNullValue())
	}

}
