package com.supermanzer.manzertracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.launch

// Scrolling wheel of numbers; whichever number rests in the centre band is the selection.
// initialValue only sets the starting position — after that the scroll position is the state.
@Composable
fun WheelNumberPicker(
    initialValue: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItems: Int = 5,
    itemHeight: Dp = 44.dp
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialValue.coerceIn(range) - range.first
    )
    val scope = rememberCoroutineScope()
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val centeredIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                ?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState, range) {
        snapshotFlow { centeredIndex }.collect { currentOnValueChange(range.first + it) }
    }

    Box(modifier = modifier.height(itemHeight * visibleItems)) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            // Padding of half the wheel lets the first and last numbers reach the centre.
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2)),
            modifier = Modifier.fillMaxSize()
        ) {
            items(range.count()) { index ->
                val distance = abs(index - centeredIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(if (distance == 0) 1f else if (distance == 1) 0.6f else 0.3f)
                        .clickable { scope.launch { listState.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (range.first + index).toString(),
                        style = if (distance == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                        color = if (distance == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).offset(y = -itemHeight / 2))
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).offset(y = itemHeight / 2))
    }
}
