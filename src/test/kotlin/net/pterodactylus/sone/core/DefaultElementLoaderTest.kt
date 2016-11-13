package net.pterodactylus.sone.core

import com.google.common.base.Ticker
import com.google.common.io.ByteStreams
import freenet.keys.FreenetURI
import net.pterodactylus.sone.core.FreenetInterface.BackgroundFetchCallback
import net.pterodactylus.sone.test.capture
import net.pterodactylus.sone.test.mock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Unit test for [DefaultElementLoaderTest].
 */
class DefaultElementLoaderTest {

	companion object {
		private const val IMAGE_ID = "KSK@gpl.png"
		private val freenetURI = FreenetURI(IMAGE_ID)
	}

	private val freenetInterface = mock<FreenetInterface>()
	private val ticker = mock<Ticker>()
	private val elementLoader = DefaultElementLoader(freenetInterface, ticker)
	private val callback = capture<BackgroundFetchCallback>()

	@Test
	fun `image loader starts request for link that is not known`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), any<BackgroundFetchCallback>())
	}

	@Test
	fun `element loader only starts request once`() {
		elementLoader.loadElement(IMAGE_ID)
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), any<BackgroundFetchCallback>())
	}

	@Test
	fun `element loader returns loading element on first call`() {
		assertThat(elementLoader.loadElement(IMAGE_ID).loading, `is`(true))
	}

	@Test
	fun `element loader does not cancel on image mime type`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		assertThat(callback.value.cancelForMimeType(freenetURI, "image/png"), `is`(false))
	}

	@Test
	fun `element loader does  cancel on audio mime type`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		assertThat(callback.value.cancelForMimeType(freenetURI, "audio/mpeg"), `is`(true))
	}

	@Test
	fun `element loader does not cancel on video mime type`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		assertThat(callback.value.cancelForMimeType(freenetURI, "video/mkv"), `is`(true))
	}

	@Test
	fun `element loader does not cancel on text mime type`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		assertThat(callback.value.cancelForMimeType(freenetURI, "text/plain"), `is`(true))
	}

	@Test
	fun `image loader can load image`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		callback.value.loaded(freenetURI, "image/png", read("/static/images/unknown-image-0.png"))
		val linkedElement = elementLoader.loadElement(IMAGE_ID)
		assertThat(linkedElement, `is`(LinkedElement(IMAGE_ID)))
	}

	@Test
	fun `image is not loaded again after it failed`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		callback.value.failed(freenetURI)
		assertThat(elementLoader.loadElement(IMAGE_ID).failed, `is`(true))
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
	}

	@Test
	fun `image is loaded again after failure cache is expired`() {
		elementLoader.loadElement(IMAGE_ID)
		verify(freenetInterface).startFetch(eq(freenetURI), callback.capture())
		callback.value.failed(freenetURI)
		`when`(ticker.read()).thenReturn(TimeUnit.MINUTES.toNanos(31))
		val linkedElement = elementLoader.loadElement(IMAGE_ID)
		assertThat(linkedElement.failed, `is`(false))
		assertThat(linkedElement.loading, `is`(true))
		verify(freenetInterface, times(2)).startFetch(eq(freenetURI), callback.capture())
	}

	private fun read(resource: String): ByteArray =
			javaClass.getResourceAsStream(resource)?.use { input ->
				ByteArrayOutputStream().use {
					ByteStreams.copy(input, it)
					it
				}.toByteArray()
			} ?: ByteArray(0)

}