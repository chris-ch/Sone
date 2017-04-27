package net.pterodactylus.sone.web.pages

import net.pterodactylus.sone.data.Album
import net.pterodactylus.sone.data.Image
import net.pterodactylus.sone.data.Sone
import net.pterodactylus.sone.test.mock
import net.pterodactylus.sone.test.whenever
import net.pterodactylus.sone.web.pages.WebPageTest
import net.pterodactylus.sone.web.pages.DeleteImagePage
import net.pterodactylus.util.web.Method.GET
import net.pterodactylus.util.web.Method.POST
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify

/**
 * Unit test for [DeleteImagePage].
 */
class DeleteImagePageTest: WebPageTest() {

	private val page = DeleteImagePage(template, webInterface)
	private val image = mock<Image>()
	private val sone = mock<Sone>()

	override fun getPage() = page

	@Before
	fun setupImage() {
		val album = mock<Album>()
		whenever(album.id).thenReturn("album-id")
		whenever(image.id).thenReturn("image-id")
		whenever(image.sone).thenReturn(sone)
		whenever(image.album).thenReturn(album)
		whenever(sone.isLocal).thenReturn(true)
	}

	@Test
	fun `page returns correct path`() {
		assertThat(page.path, equalTo("deleteImage.html"))
	}

	@Test
	fun `page requires login`() {
		assertThat(page.requiresLogin(), equalTo(true))
	}

	@Test
	fun `get request with invalid image redirects to invalid page`() {
		request("", GET)
		verifyRedirect("invalid.html")
	}

	@Test
	fun `get request with image from non-local sone redirects to no permissions page`() {
		request("", GET)
		whenever(sone.isLocal).thenReturn(false)
		addImage("image-id", image)
		addHttpRequestParameter("image", "image-id")
		verifyRedirect("noPermission.html")
	}

	@Test
	fun `get request with image from local sone sets image in template context`() {
		request("", GET)
		addImage("image-id", image)
		addHttpRequestParameter("image", "image-id")
		page.processTemplate(freenetRequest, templateContext)
		assertThat(templateContext["image"], equalTo<Any>(image))
	}

	@Test
	fun `post request with abort delete flag set redirects to image browser`() {
		request("", POST)
		addImage("image-id", image)
		addHttpRequestPart("image", "image-id")
		addHttpRequestPart("abortDelete", "true")
		verifyRedirect("imageBrowser.html?image=image-id")
	}

	@Test
	fun `post request deletes image and redirects to image browser`() {
		request("", POST)
		addImage("image-id", image)
		addHttpRequestPart("image", "image-id")
		verifyRedirect("imageBrowser.html?album=album-id") {
			verify(webInterface.core).deleteImage(image)
		}
	}

}
