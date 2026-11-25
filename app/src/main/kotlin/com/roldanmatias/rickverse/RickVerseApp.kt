package com.roldanmatias.rickverse

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath

/** Hilt entry point. Also configures Coil's process-wide image loader. */
@HiltAndroidApp
class RickVerseApp :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            // Avatars are small and repeat across screens: a modest memory cache plus a disk cache is plenty.
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build() }
            .diskCache {
                DiskCache
                    .Builder()
                    .directory(context.cacheDir.resolve("images").toOkioPath())
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }.build()

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.20
        const val DISK_CACHE_BYTES = 64L * 1024 * 1024
    }
}
