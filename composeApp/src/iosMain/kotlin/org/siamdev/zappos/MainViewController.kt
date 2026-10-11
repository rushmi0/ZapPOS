/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos

import androidx.compose.ui.window.ComposeUIViewController
import org.siamdev.module.db.AppDatabase
import org.siamdev.module.db.appDatabase
import org.siamdev.zappos.cache.IosThumbnailCache
import org.siamdev.zappos.cache.thumbnailCache

private val database: AppDatabase = appDatabase().also {
    thumbnailCache = IosThumbnailCache()
}

fun MainViewController() = ComposeUIViewController {
    val platform = getPlatform()
    App(platform = platform)
}

fun initializeDatabase(): AppDatabase = appDatabase()