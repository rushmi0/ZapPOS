/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.siamdev.zappos.data.source.MasterEvent
import org.siamdev.zappos.ui.components.menu.DefaultProductCategories
import org.siamdev.zappos.utils.formatPrice
import org.siamdev.zappos.theme.ThemeMode
import org.siamdev.zappos.theme.ZapposTheme

/** Colors used by [ProductListItem]. Create via [ProductListItemDefaults.colors]. */
@Immutable
data class ProductListItemColors(
    val containerColor: Color,
    val selectedContainerColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val supportingColor: Color,
    val accentColor: Color,
    val trailingIconColor: Color,
)

/** Default values for [ProductListItem] and [ProductThumbnail]. */
object ProductListItemDefaults {
    val Shape: Shape = RoundedCornerShape(10.dp)
    val ThumbnailShape: Shape = RoundedCornerShape(10.dp)
    val ThumbnailSize: Dp = 48.dp
    val ContentPadding = PaddingValues(10.dp)

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        selectedContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
        titleColor: Color = MaterialTheme.colorScheme.onSurface,
        subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        supportingColor: Color = MaterialTheme.colorScheme.onSurface,
        accentColor: Color = MaterialTheme.colorScheme.primary,
        trailingIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    ) = ProductListItemColors(
        containerColor = containerColor,
        selectedContainerColor = selectedContainerColor,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        supportingColor = supportingColor,
        accentColor = accentColor,
        trailingIconColor = trailingIconColor,
    )

    /** Default trailing chevron. */
    @Composable
    fun Chevron(tint: Color = colors().trailingIconColor) {
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Square product image that falls back to a tinted [placeholderIcon] when [imageUrl] is blank. */
@Composable
fun ProductThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Default.ShoppingBag,
    size: Dp = ProductListItemDefaults.ThumbnailSize,
    shape: Shape = ProductListItemDefaults.ThumbnailShape,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val base = modifier.size(size).clip(shape)
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
            contentDescription = null,
            modifier = base.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = base.background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                placeholderIcon,
                contentDescription = null,
                modifier = Modifier.size(size / 2),
                tint = accentColor,
            )
        }
    }
}

/**
 * Generic product row: leading thumbnail, title (with optional recommended star),
 * subtitle, supporting text, and a trailing slot.
 *
 * [supportingContent] replaces [supportingText] when rich content (e.g. a price row) is needed.
 * Every slot can be replaced, so the row can be reused for any list (menu, stock, order…).
 * Pass `onClick = null` for a non-interactive row and `trailing = null` to hide the chevron.
 */
@Composable
fun ProductListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supportingText: String? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    imageUrl: String? = null,
    placeholderIcon: ImageVector = Icons.Default.ShoppingBag,
    isSelected: Boolean = false,
    isRecommended: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: Shape = ProductListItemDefaults.Shape,
    colors: ProductListItemColors = ProductListItemDefaults.colors(),
    contentPadding: PaddingValues = ProductListItemDefaults.ContentPadding,
    leading: (@Composable () -> Unit)? = {
        ProductThumbnail(
            imageUrl = imageUrl,
            placeholderIcon = placeholderIcon,
            accentColor = colors.accentColor,
        )
    },
    trailing: (@Composable RowScope.() -> Unit)? = {
        ProductListItemDefaults.Chevron(colors.trailingIconColor)
    },
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isSelected) colors.selectedContainerColor else colors.containerColor)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isRecommended) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = colors.accentColor,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (supportingContent != null) {
                supportingContent()
            } else if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.supportingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailing?.invoke(this)
    }
}

/** Convenience overload that maps a [MasterEvent] to [ProductListItem]. */
@Composable
fun ProductListItem(
    product: MasterEvent,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: Shape = ProductListItemDefaults.Shape,
    colors: ProductListItemColors = ProductListItemDefaults.colors(),
    contentPadding: PaddingValues = ProductListItemDefaults.ContentPadding,
    supportingText: String? = "฿ ${product.price.formatPrice()} / ${product.unit}",
    trailing: (@Composable RowScope.() -> Unit)? = {
        ProductListItemDefaults.Chevron(colors.trailingIconColor)
    },
) {
    val catEntry = DefaultProductCategories.find { it.id == product.category }
    val categoryName = catEntry?.name ?: product.category
    val subName = catEntry?.subCategories?.find { it.id == product.subCategory }?.name

    ProductListItem(
        title = product.name,
        modifier = modifier,
        subtitle = if (subName != null) "$categoryName · $subName" else categoryName,
        supportingText = supportingText,
        imageUrl = product.imageUrl,
        placeholderIcon = catEntry?.icon ?: Icons.Default.ShoppingBag,
        isSelected = isSelected,
        isRecommended = product.isRecommended,
        onClick = onClick,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        trailing = trailing,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProductListItemPreview() {
    ZapposTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(12.dp)) {
            ProductListItem(
                title = "Iced Latte",
                subtitle = "Drinks · Coffee",
                supportingText = "฿ 65.00 / cup",
                isRecommended = true,
                onClick = {},
            )
            Spacer(Modifier.size(6.dp))
            ProductListItem(
                title = "Croissant",
                subtitle = "Bakery",
                supportingText = "฿ 45.00 / pcs",
                isSelected = true,
                onClick = {},
                trailing = {
                    Text("x2", style = MaterialTheme.typography.labelLarge)
                },
            )
        }
    }
}
