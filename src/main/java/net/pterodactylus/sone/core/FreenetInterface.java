/*
 * Sone - FreenetInterface.java - Copyright © 2010–2013 David Roden
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

package net.pterodactylus.sone.core;

import static freenet.client.FetchException.PERMANENT_REDIRECT;
import static freenet.node.RequestStarter.INTERACTIVE_PRIORITY_CLASS;
import static freenet.node.RequestStarter.PREFETCH_PRIORITY_CLASS;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static net.pterodactylus.sone.data.Sone.TO_FREENET_URI;
import static net.pterodactylus.util.logging.Logging.getLogger;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.event.ImageInsertAbortedEvent;
import net.pterodactylus.sone.core.event.ImageInsertFailedEvent;
import net.pterodactylus.sone.core.event.ImageInsertFinishedEvent;
import net.pterodactylus.sone.core.event.ImageInsertStartedEvent;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.TemporaryImage;
import net.pterodactylus.util.logging.Logging;

import com.db4o.ObjectContainer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import freenet.client.ClientMetadata;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.HighLevelSimpleClient;
import freenet.client.HighLevelSimpleClientImpl;
import freenet.client.InsertBlock;
import freenet.client.InsertContext;
import freenet.client.InsertException;
import freenet.client.async.BaseClientPutter;
import freenet.client.async.ClientContext;
import freenet.client.async.ClientPutCallback;
import freenet.client.async.ClientPutter;
import freenet.client.async.USKCallback;
import freenet.client.async.USKManager;
import freenet.keys.FreenetURI;
import freenet.keys.InsertableClientSSK;
import freenet.keys.USK;
import freenet.node.Node;
import freenet.node.RequestClient;
import freenet.node.RequestStarter;
import freenet.support.api.Bucket;
import freenet.support.io.ArrayBucket;

/**
 * Contains all necessary functionality for interacting with the Freenet node.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class FreenetInterface {

	/** The logger. */
	private static final Logger logger = getLogger(FreenetInterface.class);

	/** The event bus. */
	private final EventBus eventBus;

	/** The node to interact with. */
	private final Node node;

	/** The high-level client to use for requests. */
	private final HighLevelSimpleClient client;
	private final RequestClient requestClient;

	/** The USK callbacks. */
	private final Map<String, USKCallback> soneUskCallbacks = new HashMap<String, USKCallback>();

	/** The not-Sone-related USK callbacks. */
	private final Map<FreenetURI, USKCallback> uriUskCallbacks = synchronizedMap(new HashMap<FreenetURI, USKCallback>());
	private USKManager uskManager;

	/**
	 * Creates a new Freenet interface.
	 *
	 * @param eventBus
	 *            The event bus
	 * @param node
	 *            The node to interact with
	 */
	@Inject
	public FreenetInterface(EventBus eventBus, Node node) {
		this.eventBus = eventBus;
		this.node = node;
		this.client = node.clientCore.makeClient(INTERACTIVE_PRIORITY_CLASS, false, true);
		this.requestClient = (HighLevelSimpleClientImpl) client;
		this.uskManager = node.clientCore.uskManager;
	}

	@VisibleForTesting
	public FreenetInterface(EventBus eventBus, Node node, HighLevelSimpleClient highLevelSimpleClient, RequestClient requestClient, USKManager uskManager) {
		this.eventBus = eventBus;
		this.node = node;
		this.client = highLevelSimpleClient;
		this.requestClient = requestClient;
		this.uskManager = uskManager;
	}

	//
	// ACTIONS
	//

	/**
	 * Fetches the given URI.
	 *
	 * @param uri
	 *            The URI to fetch
	 * @return The result of the fetch, or {@code null} if an error occured
	 */
	public Fetched fetchUri(FreenetURI uri) {
		FetchResult fetchResult = null;
		FreenetURI currentUri = new FreenetURI(uri);
		while (true) {
			try {
				fetchResult = client.fetch(currentUri);
				return new Fetched(currentUri, fetchResult);
			} catch (FetchException fe1) {
				if (fe1.getMode() == PERMANENT_REDIRECT) {
					currentUri = fe1.newURI;
					continue;
				}
				logger.log(WARNING, format("Could not fetch “%s”!", uri), fe1);
				return null;
			}
		}
	}

	/**
	 * Creates a key pair.
	 *
	 * @return The request key at index 0, the insert key at index 1
	 */
	public String[] generateKeyPair() {
		FreenetURI[] keyPair = client.generateKeyPair("");
		return new String[] { keyPair[1].toString(), keyPair[0].toString() };
	}

	/**
	 * Inserts the image data of the given {@link TemporaryImage} and returns
	 * the given insert token that can be used to add listeners or cancel the
	 * insert.
	 *
	 * @param temporaryImage
	 *            The temporary image data
	 * @param image
	 *            The image
	 * @param insertToken
	 *            The insert token
	 * @throws SoneException
	 *             if the insert could not be started
	 */
	public void insertImage(TemporaryImage temporaryImage, Image image, InsertToken insertToken) throws SoneException {
		String filenameHint = image.getId() + "." + temporaryImage.getMimeType().substring(temporaryImage.getMimeType().lastIndexOf("/") + 1);
		InsertableClientSSK key = InsertableClientSSK.createRandom(node.random, "");
		FreenetURI targetUri = key.getInsertURI().setDocName(filenameHint);
		InsertContext insertContext = client.getInsertContext(true);
		Bucket bucket = new ArrayBucket(temporaryImage.getImageData());
		ClientMetadata metadata = new ClientMetadata(temporaryImage.getMimeType());
		InsertBlock insertBlock = new InsertBlock(bucket, metadata, targetUri);
		try {
			ClientPutter clientPutter = client.insert(insertBlock, false, null, false, insertContext, insertToken, INTERACTIVE_PRIORITY_CLASS);
			insertToken.setClientPutter(clientPutter);
		} catch (InsertException ie1) {
			throw new SoneInsertException("Could not start image insert.", ie1);
		}
	}

	/**
	 * Inserts a directory into Freenet.
	 *
	 * @param insertUri
	 *            The insert URI
	 * @param manifestEntries
	 *            The directory entries
	 * @param defaultFile
	 *            The name of the default file
	 * @return The generated URI
	 * @throws SoneException
	 *             if an insert error occurs
	 */
	public FreenetURI insertDirectory(FreenetURI insertUri, HashMap<String, Object> manifestEntries, String defaultFile) throws SoneException {
		try {
			return client.insertManifest(insertUri, manifestEntries, defaultFile);
		} catch (InsertException ie1) {
			throw new SoneException(ie1);
		}
	}

	/**
	 * Registers the USK for the given Sone and notifies the given
	 * {@link SoneDownloader} if an update was found.
	 *
	 * @param sone
	 *            The Sone to watch
	 * @param soneDownloader
	 *            The Sone download to notify on updates
	 */
	public void registerUsk(final Sone sone, final SoneDownloader soneDownloader) {
		try {
			logger.log(FINE, format("Registering Sone “%s” for USK updates at %s…", sone, TO_FREENET_URI.apply(sone).setMetaString(new String[]{"sone.xml"})));
			USKCallback uskCallback = new NewEditionFound(sone, soneDownloader);
			soneUskCallbacks.put(sone.getId(), uskCallback);
			boolean runBackgroundFetch = soneWasUpdatedInTheLastWeek(sone);
			uskManager.subscribe(USK.create(TO_FREENET_URI.apply(sone)), uskCallback, runBackgroundFetch, requestClient);
		} catch (MalformedURLException mue1) {
			logger.log(WARNING, format("Could not subscribe USK “%s”!", TO_FREENET_URI.apply(sone)), mue1);
		}
	}

	private boolean soneWasUpdatedInTheLastWeek(Sone sone) {
		return (System.currentTimeMillis() - sone.getTime()) < DAYS.toMillis(7);
	}

	/**
	 * Unsubscribes the request URI of the given Sone.
	 *
	 * @param sone
	 *            The Sone to unregister
	 */
	public void unregisterUsk(Sone sone) {
		USKCallback uskCallback = soneUskCallbacks.remove(sone.getId());
		if (uskCallback == null) {
			return;
		}
		try {
			logger.log(FINEST, format("Unsubscribing from USK for %s…", sone));
			uskManager.unsubscribe(USK.create(TO_FREENET_URI.apply(sone)), uskCallback);
		} catch (MalformedURLException mue1) {
			logger.log(FINE, format("Could not unsubscribe USK “%s”!", TO_FREENET_URI.apply(sone)), mue1);
		}
	}

	/**
	 * Registers an arbitrary URI and calls the given callback if a new edition
	 * is found.
	 *
	 * @param uri
	 *            The URI to watch
	 * @param callback
	 *            The callback to call
	 */
	public void registerUsk(FreenetURI uri, final Callback callback) {
		USKCallback uskCallback = new CallbackWrapper(callback);
		try {
			uskManager.subscribe(USK.create(uri), uskCallback, true, requestClient);
			uriUskCallbacks.put(uri, uskCallback);
		} catch (MalformedURLException mue1) {
			logger.log(WARNING, format("Could not subscribe to USK: %s", uri), mue1);
		}
	}

	/**
	 * Unregisters the USK watcher for the given URI.
	 *
	 * @param uri
	 *            The URI to unregister the USK watcher for
	 */
	public void unregisterUsk(FreenetURI uri) {
		USKCallback uskCallback = uriUskCallbacks.remove(uri);
		if (uskCallback == null) {
			logger.log(INFO, format("Could not unregister unknown USK: %s", uri));
			return;
		}
		try {
			uskManager.unsubscribe(USK.create(uri), uskCallback);
		} catch (MalformedURLException mue1) {
			logger.log(INFO, format("Could not unregister invalid USK: %s", uri), mue1);
		}
	}

	/**
	 * Container for a fetched URI and the {@link FetchResult}.
	 *
	 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
	 */
	public static class Fetched {

		/** The fetched URI. */
		private final FreenetURI freenetUri;

		/** The fetch result. */
		private final FetchResult fetchResult;

		/**
		 * Creates a new fetched URI.
		 *
		 * @param freenetUri
		 *            The URI that was fetched
		 * @param fetchResult
		 *            The fetch result
		 */
		public Fetched(FreenetURI freenetUri, FetchResult fetchResult) {
			this.freenetUri = freenetUri;
			this.fetchResult = fetchResult;
		}

		//
		// ACCESSORS
		//

		/**
		 * Returns the fetched URI.
		 *
		 * @return The fetched URI
		 */
		public FreenetURI getFreenetUri() {
			return freenetUri;
		}

		/**
		 * Returns the fetch result.
		 *
		 * @return The fetch result
		 */
		public FetchResult getFetchResult() {
			return fetchResult;
		}

	}

	/**
	 * Callback for USK watcher events.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	public static interface Callback {

		/**
		 * Notifies a listener that a new edition was found for a URI.
		 *
		 * @param uri
		 *            The URI that a new edition was found for
		 * @param edition
		 *            The found edition
		 * @param newKnownGood
		 *            Whether the found edition was actually fetched
		 * @param newSlot
		 *            Whether the found edition is higher than all previously
		 *            found editions
		 */
		public void editionFound(FreenetURI uri, long edition, boolean newKnownGood, boolean newSlot);

	}

	private static class NewEditionFound implements USKCallback {

		private final Sone sone;
		private final SoneDownloader soneDownloader;

		public NewEditionFound(Sone sone, SoneDownloader soneDownloader) {
			this.sone = sone;
			this.soneDownloader = soneDownloader;
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void onFoundEdition(long edition, USK key, ObjectContainer objectContainer, ClientContext clientContext, boolean metadata, short codec, byte[] data, boolean newKnownGood, boolean newSlotToo) {
			logger.log(FINE, format("Found USK update for Sone “%s” at %s, new known good: %s, new slot too: %s.", sone, key, newKnownGood, newSlotToo));
			if (edition > sone.getLatestEdition()) {
				sone.modify().setLatestEdition(edition).update();
				new Thread(new Runnable() {

					@Override
					public void run() {
						soneDownloader.fetchSone(sone);
					}
				}, format("Sone Downloader for %s", sone.getId())).start();
			}
		}

		@Override
		public short getPollingPriorityProgress() {
			return INTERACTIVE_PRIORITY_CLASS;
		}

		@Override
		public short getPollingPriorityNormal() {
			return INTERACTIVE_PRIORITY_CLASS;
		}
	}

	private static class CallbackWrapper implements USKCallback {

		private final Callback callback;

		public CallbackWrapper(Callback callback) {
			this.callback = callback;
		}

		@Override
		public void onFoundEdition(long edition, USK key, ObjectContainer objectContainer, ClientContext clientContext, boolean metadata, short codec, byte[] data, boolean newKnownGood, boolean newSlotToo) {
			callback.editionFound(key.getURI(), edition, newKnownGood, newSlotToo);
		}

		@Override
		public short getPollingPriorityNormal() {
			return PREFETCH_PRIORITY_CLASS;
		}

		@Override
		public short getPollingPriorityProgress() {
			return INTERACTIVE_PRIORITY_CLASS;
		}

	}

	/**
	 * Insert token that can cancel a running insert and sends events.
	 *
	 * @see ImageInsertAbortedEvent
	 * @see ImageInsertStartedEvent
	 * @see ImageInsertFailedEvent
	 * @see ImageInsertFinishedEvent
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	public class InsertToken implements ClientPutCallback {

		/** The image being inserted. */
		private final Image image;

		/** The client putter. */
		private ClientPutter clientPutter;

		/** The final URI. */
		private volatile FreenetURI resultingUri;

		/**
		 * Creates a new insert token for the given image.
		 *
		 * @param image
		 *            The image being inserted
		 */
		public InsertToken(Image image) {
			this.image = image;
		}

		//
		// ACCESSORS
		//

		/**
		 * Sets the client putter that is inserting the image. This will also
		 * signal all registered listeners that the image has started.
		 *
		 * @param clientPutter
		 *            The client putter
		 */
		@SuppressWarnings("synthetic-access")
		public void setClientPutter(ClientPutter clientPutter) {
			this.clientPutter = clientPutter;
			eventBus.post(new ImageInsertStartedEvent(image));
		}

		//
		// ACTIONS
		//

		/**
		 * Cancels the running insert.
		 */
		@SuppressWarnings("synthetic-access")
		public void cancel() {
			clientPutter.cancel(null, node.clientCore.clientContext);
			eventBus.post(new ImageInsertAbortedEvent(image));
		}

		//
		// INTERFACE ClientPutCallback
		//

		@Override
		public void onMajorProgress(ObjectContainer objectContainer) {
			/* ignore, we don’t care. */
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void onFailure(InsertException insertException, BaseClientPutter clientPutter, ObjectContainer objectContainer) {
			if ((insertException != null) && ("Cancelled by user".equals(insertException.getMessage()))) {
				eventBus.post(new ImageInsertAbortedEvent(image));
			} else {
				eventBus.post(new ImageInsertFailedEvent(image, insertException));
			}
		}

		@Override
		public void onFetchable(BaseClientPutter clientPutter, ObjectContainer objectContainer) {
			/* ignore, we don’t care. */
		}

		@Override
		public void onGeneratedMetadata(Bucket metadata, BaseClientPutter clientPutter, ObjectContainer objectContainer) {
			/* ignore, we don’t care. */
		}

		@Override
		public void onGeneratedURI(FreenetURI generatedUri, BaseClientPutter clientPutter, ObjectContainer objectContainer) {
			resultingUri = generatedUri;
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void onSuccess(BaseClientPutter clientPutter, ObjectContainer objectContainer) {
			eventBus.post(new ImageInsertFinishedEvent(image, resultingUri));
		}

	}

}
