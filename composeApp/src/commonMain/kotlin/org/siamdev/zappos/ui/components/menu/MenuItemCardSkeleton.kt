/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.menu

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.components.product.ProductListItem
import org.siamdev.zappos.ui.components.product.ProductListItemDefaults
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun shimmerBrush(): Brush {
    // Tints of onSurface stay visible on the surfaceVariant card in both light and dark themes.
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    val shimmerColors = listOf(base, highlight, base)
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
private fun SkeletonLine(brush: Brush, widthFraction: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(brush)
    )
}

/** Loading placeholder that mirrors the list layout of [MenuItemCard] (a [ProductListItem] row). */
@Composable
fun MenuItemCardListSkeleton() {
    val brush = shimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ProductListItemDefaults.Shape)
            .background(ProductListItemDefaults.colors().containerColor)
            .padding(ProductListItemDefaults.ContentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(ProductListItemDefaults.ThumbnailSize)
                .clip(ProductListItemDefaults.ThumbnailShape)
                .background(brush)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            SkeletonLine(brush, widthFraction = 0.6f, height = 14.dp) // name
            Spacer(Modifier.height(5.dp))
            SkeletonLine(brush, widthFraction = 0.35f, height = 10.dp) // category
            Spacer(Modifier.height(5.dp))
            SkeletonLine(brush, widthFraction = 0.3f, height = 12.dp) // price
        }

        Spacer(Modifier.width(8.dp))

        // Add button placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
    }
}

/** Loading placeholder that mirrors the grid layout of [MenuItemCard]. */
@Composable
fun MenuItemCardGridSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(ProductListItemDefaults.Shape)
            .background(ProductListItemDefaults.colors().containerColor)
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .background(brush)
        )
        Column(modifier = Modifier.padding(ProductListItemDefaults.ContentPadding)) {
            SkeletonLine(brush, widthFraction = 0.7f, height = 14.dp) // name
            Spacer(Modifier.height(5.dp))
            SkeletonLine(brush, widthFraction = 0.45f, height = 10.dp) // category
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    SkeletonLine(brush, widthFraction = 0.5f, height = 12.dp) // price
                }
                // Add button placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
            }
        }
    }
}

// List skeleton
@Composable
fun MenuListSkeleton(count: Int = 6) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(count) {
            MenuItemCardListSkeleton()
        }
    }
}

// Grid skeleton
@Composable
fun MenuGridSkeleton(count: Int = 6) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gridColumns = if (maxWidth < 500.dp) GridCells.Fixed(2)
                          else GridCells.Adaptive(minSize = 250.dp)
        LazyVerticalGrid(
            columns = gridColumns,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(count) {
                MenuItemCardGridSkeleton()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411)
@Composable
fun MenuItemCardListSkeletonPreview() {
    Surface {
        MenuItemCardListSkeleton()
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
fun MenuItemCardGridSkeletonPreview() {
    Surface {
        MenuItemCardGridSkeleton()
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 600)
@Composable
fun MenuListSkeletonPreview() {
    Surface {
        MenuListSkeleton(count = 4)
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 600)
@Composable
fun MenuGridSkeletonPreview() {
    Surface {
        MenuGridSkeleton(count = 6)
    }
}