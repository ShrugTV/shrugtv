package com.shrug.shrugtv.ui.tv.home

import android.content.Context
import com.shrug.shrugtv.ui.tv.TvColors
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Image from the network via the already present Glide (disk cache), without a new library.
 *
 * Since :
 * - A small memory cache of its own ([ImageCache]) delivers already loaded images in the same frame,
 *   without a detour through a background thread. Before, every card showed the placeholder for at least
 *   one frame when it reappeared (row back, page reloaded).
 * - When the URL changes (hero), the old image stays until the new one is there; with [crossfade]
 *   it cross-fades in 150 ms. Before, it jumped to the placeholder on every change.
 * - Loading via a Glide target instead of `submit().get()`: when the card leaves the screen the request
 *   is cancelled instead of blocking an IO thread until the end.
 * Until the first image is there, a surface in [placeholder] is shown.
 *
 * Since : every URL gets a time key ([ImageTick]), in the memory cache and as Glide
 * signature. Twitch serves new live preview images under the same URL every 5 min; without the key the
 * first loaded image stayed in Glide's disk cache, even across app restarts. When the minute tick
 * advances, affected images recompose and reload; the old one stays until the new one is there.
 */
@Composable
fun NetImage(
    url: String?,
    modifier: Modifier = Modifier,
    width: Int = 800,
    height: Int = 450,
    placeholder: Color = TvColors.Placeholder,
    contentScale: ContentScale = ContentScale.Crop,
    crossfade: Boolean = false,
) {
    val context = LocalContext.current
    val cacheKey = ImageCache.cacheKey(url, width, height)
    var image by remember { mutableStateOf(cacheKey?.let { ImageCache.fetch(it) }) }
    LaunchedEffect(cacheKey) {
        if (cacheKey == null) {
            image = null
            return@LaunchedEffect
        }
        ImageCache.fetch(cacheKey)?.let { image = it; return@LaunchedEffect }
        image = ImageCache.load(context.applicationContext, url!!, width, height) // null: placeholder instead of the old image
    }
    if (crossfade && com.shrug.shrugtv.ui.tv.TvStyle.animations) {
        Crossfade(image, animationSpec = tween(150), label = "netzbild", modifier = modifier) { b ->
            ImageOrSurface(b, Modifier.fillMaxSize(), placeholder, contentScale)
        }
    } else {
        ImageOrSurface(image, modifier, placeholder, contentScale)
    }
}

@Composable
private fun ImageOrSurface(b: Bitmap?, modifier: Modifier, placeholder: Color, contentScale: ContentScale) {
    if (b != null) {
        Image(bitmap = b.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(placeholder))
    }
}

/** Memory cache and preloading for [NetImage]. */
object ImageCache {
    // 32 MB: about 70 card images (440x248) and a few hero images (1280x720 = 3.7 MB)
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val region = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Current minute for live preview images. Read in the composable so cards and hero reload when it
     * changes; a single loop for the whole app.
     */
    val liveTick = mutableLongStateOf(ImageTick.live(System.currentTimeMillis()))

    init {
        region.launch {
            try {
                ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val now = ImageTick.live(System.currentTimeMillis())
                    if (now != liveTick.longValue) liveTick.longValue = now
                    while (true) {
                        delay(15_000)
                        val t = ImageTick.live(System.currentTimeMillis())
                        if (t != liveTick.longValue) liveTick.longValue = t
                    }
                }
            } catch (_: Throwable) {
                // Unit-Test-Umgebung ohne Android-Hauptthread
            }
        }
    }

    /** Time key of a URL; for live preview images it reads [liveTick] (snapshot state). */
    fun signature(url: String): String =
        if (ImageTick.isLivePreview(url)) "L" + liveTick.longValue else "T" + ImageTick.tag(System.currentTimeMillis())

    fun cacheKey(url: String?, width: Int, height: Int): String? =
        if (url.isNullOrEmpty()) null else "$width x $height ${signature(url)} $url"

    fun fetch(cacheKey: String): Bitmap? = cache.get(cacheKey)

    /** Loads into the cache; cancellable via the coroutine. Null on error. */
    suspend fun load(context: Context, url: String, width: Int, height: Int): Bitmap? {
        val k = cacheKey(url, width, height) ?: return null
        cache.get(k)?.let { return it }
        val signature = signature(url)
        val glide = Glide.with(context)
        return suspendCancellableCoroutine { cont ->
            val target = object : CustomTarget<Bitmap>(width, height) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    cache.put(k, resource)
                    if (cont.isActive) cont.resume(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (cont.isActive) cont.resume(null)
                }

                // Glide only releases the image when we clear the target ourselves (on cancel, before
                // loading); a finished image stays valid in the cache.
                override fun onLoadCleared(placeholder: Drawable?) {}
            }
            glide.asBitmap().format(DecodeFormat.PREFER_RGB_565).load(url).signature(ObjectKey(signature)).into(target)
            cont.invokeOnCancellation { region.launch { glide.clear(target) } }
        }
    }

    /** Preload images, e.g. the next cards of a row. No effect if already there. */
    fun preload(context: Context, urls: List<String?>, width: Int, height: Int) {
        val app = context.applicationContext
        for (u in urls) {
            val k = cacheKey(u, width, height) ?: continue
            if (cache.get(k) != null) continue
            region.launch { load(app, u!!, width, height) }
        }
    }
}

/**
 * Time keys for image URLs, without Android dependency for unit tests. Twitch
 * renews live preview images (`/previews-ttv/`) per channel about every 5 min, at its own times (age
 * jumps back after ~300 s, measured 2026-10-28; a URL suffix does not bypass the CDN). So the app asks
 * again every minute: a new image is there at most 1 min after Twitch, with fixed 5 min windows it was
 * up to 5. Avatars, box art and banners once a day, as in the original (dailyCacheKey).
 */
object ImageTick {
    const val LIVE_MS = 60_000L
    const val TAG_MS = 24 * 60 * 60_000L

    fun isLivePreview(url: String): Boolean = "/previews-ttv/" in url
    fun live(nowMs: Long): Long = nowMs / LIVE_MS
    fun tag(nowMs: Long): Long = nowMs / TAG_MS
}
