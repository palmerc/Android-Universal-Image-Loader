package com.nostra13.universalimageloader.core;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.cache.memory.MemoryCacheAware;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.MemoryCacheKeyUtil;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

/**
 * Singleton for image loading and displaying at {@link ImageView ImageViews}<br />
 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be called before any other method.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class ImageLoader {

	public static final String TAG = ImageLoader.class.getSimpleName();

	private static final String ERROR_WRONG_ARGUMENTS = "Wrong arguments were passed to displayImage() method (ImageView reference are required)";
	private static final String ERROR_NOT_INIT = "ImageLoader must be init with configuration before using";
	private static final String ERROR_INIT_CONFIG_WITH_NULL = "ImageLoader configuration can not be initialized with null";
	private static final String LOG_LOAD_IMAGE_FROM_MEMORY_CACHE = "Load image from memory cache [%s]";

	private ImageLoaderConfiguration configuration;
	private ImageLoadingListener emptyListener;

	private Map<ImageView, String> cacheKeyForImageView = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
	private Map<ImageView, LoadAndDisplayImageTask> cacheKeyForRunningTask = Collections.synchronizedMap(new WeakHashMap<ImageView, LoadAndDisplayImageTask>());



	private final static ImageLoader INSTANCE = new ImageLoader();
	private ImageLoader() {
	}

	/** Returns singleton class instance */
	public static ImageLoader getInstance() {
		return INSTANCE;
	}

	/**
	 * Initializes ImageLoader's singleton instance with configuration. Method should be called <b>once</b> (each
	 * following call will have no effect)<br />
	 * 
	 * @param configuration
	 *            {@linkplain ImageLoaderConfiguration ImageLoader configuration}
	 * @throws IllegalArgumentException
	 *             if <b>configuration</b> parameter is null
	 */
	public synchronized void init(ImageLoaderConfiguration configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
		}
		if (this.configuration == null) {
			this.configuration = configuration;
			emptyListener = new SimpleImageLoadingListener();
		}
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView when it's turn. <br/>
	 * Default {@linkplain DisplayImageOptions display image options} from {@linkplain ImageLoaderConfiguration
	 * configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png", "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @throws RuntimeException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't called before
	 */
	public void displayImage(String uri, ImageView imageView) {
		displayImage(uri, imageView, null, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png", "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param options
	 *            {@linkplain DisplayImageOptions Display image options} for image displaying. If <b>null</b> - default
	 *            display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions) from
	 *            configuration} will be used.
	 * @throws RuntimeException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't called before
	 */
	public void displayImage(String uri, ImageView imageView, DisplayImageOptions options) {
		displayImage(uri, imageView, options, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView when it's turn.<br />
	 * Default {@linkplain DisplayImageOptions display image options} from {@linkplain ImageLoaderConfiguration
	 * configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png", "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading process. Listener fires events only if
	 *            there is no image for loading in memory cache. If there is image for loading in memory cache then
	 *            image is displayed at ImageView but listener does not fire any event. Listener fires events on UI
	 *            thread.
	 * @throws RuntimeException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't called before
	 */
	public void displayImage(String uri, ImageView imageView, ImageLoadingListener listener) {
		displayImage(uri, imageView, null, listener);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png", "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param options
	 *            {@linkplain DisplayImageOptions Display image options} for image displaying. If <b>null</b> - default
	 *            display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions) from
	 *            configuration} will be used.
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading process. Listener fires events only if
	 *            there is no image for loading in memory cache. If there is image for loading in memory cache then
	 *            image is displayed at ImageView but listener does not fire any event. Listener fires events on UI
	 *            thread.
	 * @throws RuntimeException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't called before
	 */
	public void displayImage(String uri, ImageView imageView, DisplayImageOptions options, ImageLoadingListener listener) {		
		if (imageView != null) {			
			displayTaskIsCancelled(imageView);
		}
		
		Log.d(getClass().getCanonicalName(), "Outstanding tasks: " + cacheKeyForRunningTask.size());
		
		if (configuration == null) {
			throw new RuntimeException(ERROR_NOT_INIT);
		}
		if (imageView == null) {
			Log.w(TAG, ERROR_WRONG_ARGUMENTS);
			return;
		}
		if (listener == null) {
			listener = emptyListener;
		}
		if (options == null) {
			options = configuration.defaultDisplayImageOptions;
		}

		if (uri == null || uri.length() == 0) {
			cacheKeyForImageView.remove(imageView);
			if (options.isShowImageForEmptyUri()) {
				imageView.setImageResource(options.getImageForEmptyUri());
			} else {
				imageView.setImageBitmap(null);
			}
			return;
		}

		ImageSize targetSize = getImageSizeScaleTo(imageView);
		String memoryCacheKey = MemoryCacheKeyUtil.generateKey(uri, targetSize);
		cacheKeyForImageView.put(imageView, memoryCacheKey);

		Bitmap bmp = configuration.memoryCache.get(memoryCacheKey);
		if (bmp != null && !bmp.isRecycled()) {
			if (configuration.loggingEnabled) Log.i(TAG, String.format(LOG_LOAD_IMAGE_FROM_MEMORY_CACHE, memoryCacheKey));
			listener.onLoadingStarted();
			imageView.setImageBitmap(bmp);
			listener.onLoadingComplete(bmp);
		} else {
			listener.onLoadingStarted();

			if (options.isShowStubImage()) {
				imageView.setImageResource(options.getStubImage());
			} else {
				if (options.isResetViewBeforeLoading()) {
					imageView.setImageBitmap(null);
				}
			}

			ImageLoadingInfo imageLoadingInfo = new ImageLoadingInfo(uri, imageView, targetSize, options, listener);
			imageLoadingInfo.configuration = configuration;

			LoadAndDisplayImageTask displayImageTask = new LoadAndDisplayImageTask();
			cacheKeyForRunningTask.put(imageView, displayImageTask);

			displayImageTask.execute(imageLoadingInfo);
		}
	}

	/** Returns memory cache */
	public MemoryCacheAware<String, Bitmap> getMemoryCache() {
		return configuration.memoryCache;
	}

	/**
	 * Clear memory cache.<br />
	 * Do nothing if {@link #init(ImageLoaderConfiguration)} method wasn't called before.
	 */
	public void clearMemoryCache() {
		if (configuration != null) {
			configuration.memoryCache.clear();
		}
	}

	/** Returns disc cache */
	public DiscCacheAware getDiscCache() {
		return configuration.discCache;
	}

	/**
	 * Clear disc cache.<br />
	 * Do nothing if {@link #init(ImageLoaderConfiguration)} method wasn't called before.
	 */
	public void clearDiscCache() {
		if (configuration != null) {
			configuration.discCache.clear();
		}
	}

	/** Returns URI of image which is loading at this moment into passed {@link ImageView} */
	public String getLoadingUriForView(ImageView imageView) {
		return cacheKeyForImageView.get(imageView);
	}
	
	public void displayTaskIsCancelled(ImageView imageView) {
		if (imageView != null && cacheKeyForRunningTask.containsKey(imageView)) {
			LoadAndDisplayImageTask task = cacheKeyForRunningTask.get(imageView);
			task.cancel(true);
			
			displayTaskDidComplete(imageView);
			Log.d(getClass().getCanonicalName(), "Task is cancelled.");
		}
	}
	
	public void displayTaskDidComplete(ImageView imageView) {
		if (imageView != null) {
			cacheKeyForRunningTask.remove(imageView);
		}
	}

	/** Stops all running display image tasks, discards all other scheduled tasks */
	public void stop() {
	}

	/**
	 * Defines image size for loading at memory (for memory economy) by {@link ImageView} parameters.<br />
	 * Size computing algorithm:<br />
	 * 1) Get <b>maxWidth</b> and <b>maxHeight</b>. If both of them are not set then go to step #2.<br />
	 * 2) Get <b>layout_width</b> and <b>layout_height</b>. If both of them haven't exact value then go to step #3.</br>
	 * 3) Get device screen dimensions.
	 */
	private ImageSize getImageSizeScaleTo(ImageView imageView) {
		int width = -1;
		int height = -1;

		// Check maxWidth and maxHeight parameters
		try {
			Field maxWidthField = ImageView.class.getDeclaredField("mMaxWidth");
			Field maxHeightField = ImageView.class.getDeclaredField("mMaxHeight");
			maxWidthField.setAccessible(true);
			maxHeightField.setAccessible(true);
			int maxWidth = (Integer) maxWidthField.get(imageView);
			int maxHeight = (Integer) maxHeightField.get(imageView);

			if (maxWidth >= 0 && maxWidth < Integer.MAX_VALUE) {
				width = maxWidth;
			}
			if (maxHeight >= 0 && maxHeight < Integer.MAX_VALUE) {
				height = maxHeight;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}

		if (width <= 0 && height <= 0) {
			// Get layout width and height parameters
			LayoutParams params = imageView.getLayoutParams();
			width = params.width;
			height = params.height;
		}

		if (width <= 0 && height <= 0) {
			// Get device screen dimensions
			width = configuration.maxImageWidthForMemoryCache;
			height = configuration.maxImageHeightForMemoryCache;

			// Consider device screen orientation
			int screenOrientation = imageView.getContext().getResources().getConfiguration().orientation;
			if ((screenOrientation == Configuration.ORIENTATION_PORTRAIT && width > height)
					|| (screenOrientation == Configuration.ORIENTATION_LANDSCAPE && width < height)) {
				int tmp = width;
				width = height;
				height = tmp;
			}
		}
		return new ImageSize(width, height);
	}
}
