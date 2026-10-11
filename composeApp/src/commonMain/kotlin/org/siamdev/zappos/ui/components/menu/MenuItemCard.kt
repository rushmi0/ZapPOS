/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.menu

import org.siamdev.zappos.ui.components.common.MaterialButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import okio.Path.Companion.toPath
import org.siamdev.zappos.cache.ThumbnailSection
import org.siamdev.zappos.cache.sanitizeCategory
import org.siamdev.zappos.cache.thumbnailCache
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import zappos.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import zappos.composeapp.generated.resources.compose_multiplatform
import org.siamdev.zappos.LocalSettingState
import org.siamdev.zappos.ui.components.common.CurrencyCodeIcon
import org.siamdev.zappos.ui.components.product.ProductListItem
import org.siamdev.zappos.ui.components.product.ProductListItemDefaults
import org.siamdev.zappos.theme.ThemeMode
import org.siamdev.zappos.theme.ZapposTheme


enum class MenuViewMode { LIST, GRID }

@Composable
fun MenuItemCard(
    imageUrl: String,
    name: String,
    priceBaht: String,
    priceSat: String? = null,
    category: String = "",
    isRecommended: Boolean = false,
    isAvailable: Boolean = true,
    count: UInt = 0u,
    id: Int = 0,
    viewMode: MenuViewMode = MenuViewMode.LIST,
    showChevron: Boolean = false,
    onAddClick: () -> Unit = {},
    onReduceClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    // Unavailable items are hidden entirely rather than labelled.
    if (!isAvailable) return

    when (viewMode) {
        MenuViewMode.LIST -> MenuItemCardList(
            imageUrl, id, name, priceBaht, priceSat, category, isRecommended,
            count, showChevron, onAddClick, onClick
        )

        MenuViewMode.GRID -> MenuItemCardGrid(
            imageUrl, id, name, priceBaht, priceSat, category, isRecommended,
            count, onAddClick
        )
    }
}


@Composable
private fun MenuItemCardList(
    imageUrl: String,
    id: Int,
    name: String,
    priceBaht: String,
    priceSat: String?,
    category: String,
    isRecommended: Boolean,
    count: UInt,
    showChevron: Boolean,
    onAddClick: () -> Unit,
    onClick: () -> Unit
) {
    val colors = ProductListItemDefaults.colors()
    ProductListItem(
        title = name,
        subtitle = category.takeIf { it.isNotBlank() },
        supportingContent = { PriceRow(priceBaht = priceBaht, priceSat = priceSat) },
        isRecommended = isRecommended,
        onClick = if (showChevron) onClick else null,
        colors = colors,
        leading = {
            MenuImage(
                imageUrl = imageUrl,
                id = id,
                category = category,
                size = ProductListItemDefaults.ThumbnailSize,
                shape = ProductListItemDefaults.ThumbnailShape
            )
        },
        trailing = {
            Spacer(Modifier.width(8.dp))
            if (showChevron) {
                AddButton(onClick = onAddClick, size = 32.dp)
            } else {
                if (count > 0u) CountBadge(count = count)
                AddButton(onClick = onAddClick, size = 32.dp)
            }
        }
    )
}


@Composable
private fun MenuItemCardGrid(
    imageUrl: String,
    id: Int,
    name: String,
    priceBaht: String,
    priceSat: String?,
    category: String,
    isRecommended: Boolean,
    count: UInt,
    onAddClick: () -> Unit
) {
    val colors = ProductListItemDefaults.colors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(ProductListItemDefaults.Shape)
            .background(colors.containerColor)
    ) {
        MenuImage(
            imageUrl = imageUrl,
            id = id,
            category = category,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
            shape = RectangleShape
        )

        Column(Modifier.padding(ProductListItemDefaults.ContentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, false)
                )
                if (isRecommended) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = colors.accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (category.isNotBlank()) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    PriceRow(priceBaht = priceBaht, priceSat = priceSat)
                }
                if (count > 0u) CountBadge(count = count)
                AddButton(onClick = onAddClick, size = 32.dp)
            }
        }
    }
}


@Composable
private fun MenuImage(
    imageUrl: String,
    id: Int,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = ProductListItemDefaults.ThumbnailShape,
    contentScale: ContentScale = ContentScale.Crop
) {

    val source: Any = remember(id, category, imageUrl) {
        if (id != 0) {
            val cache = thumbnailCache
            val cat = sanitizeCategory(category)
            val hash = cache?.getHash(ThumbnailSection.PRODUCTS, cat, id.toString())
            if (cache != null && hash != null && cache.exists(
                    ThumbnailSection.PRODUCTS,
                    cat,
                    hash
                )
            ) {
                cache.filePath(ThumbnailSection.PRODUCTS, cat, hash).toPath()
            } else imageUrl
        } else imageUrl
    }
    val baseModifier = if (size != null) modifier.size(size) else modifier
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(source)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = null,
        modifier = baseModifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = contentScale,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
        error = painterResource(Res.drawable.compose_multiplatform)
    )
}

@Composable
private fun PriceRow(priceBaht: String, priceSat: String?) {
    val setting = LocalSettingState.current
    val showSecondary = setting.currency.showSecondary
    val primaryCode = setting.currency.primaryCode
    val secondaryCode = setting.currency.secondaryCode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CurrencyCodeIcon(
                code = primaryCode,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = priceBaht,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (priceSat != null && showSecondary) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                CurrencyCodeIcon(
                    code = secondaryCode,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = priceSat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

    }
}

@Composable
private fun CountBadge(count: UInt) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = "×$count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun AddButton(
    onClick: () -> Unit,
    size: Dp
) {
    MaterialButton(
        modifier = Modifier.size(size),
        iconCenter = Icons.Default.Add,
        iconColor = Color.White,
        buttonColor = Color(0xFF070E1E),
        onClick = onClick
    )
}


// LIST — count + category + recommended
@Preview(showBackground = true)
@Composable
fun MenuItemCardListPreview() {
    ZapposTheme(themeMode = ThemeMode.LIGHT) {
        MenuItemCard(
            imageUrl = "https://images.pexels.com/photos/350478/pexels-photo-350478.jpeg",
            name = "Mocha",
            priceBaht = "70.00",
            priceSat = "17,500",
            category = "Beverages · Coffee",
            isRecommended = true,
            count = 2u,
            viewMode = MenuViewMode.LIST
        )
    }
}

// LIST — chevron mode (ProductListDetailScreen usage)
@Preview(showBackground = true)
@Composable
fun MenuItemCardChevronPreview() {
    ZapposTheme(themeMode = ThemeMode.LIGHT) {
        MenuItemCard(
            imageUrl = "https://images.pexels.com/photos/2611811/pexels-photo-2611811.jpeg",
            name = "Matcha Latte",
            priceBaht = "100.00",
            category = "Beverages · Tea",
            isRecommended = true,
            showChevron = true,
            viewMode = MenuViewMode.LIST
        )
    }
}

// GRID — recommended, with count
@Preview(showBackground = true)
@Composable
fun MenuItemCardGridPreview() {
    ZapposTheme(themeMode = ThemeMode.LIGHT) {
        MenuItemCard(
            imageUrl = "https://images.pexels.com/photos/18635175/pexels-photo-18635175.jpeg",
            name = "Matcha Coffee",
            priceBaht = "100.00",
            priceSat = "26,000",
            category = "Matcha",
            isRecommended = true,
            count = 1u,
            viewMode = MenuViewMode.GRID
        )
    }
}
