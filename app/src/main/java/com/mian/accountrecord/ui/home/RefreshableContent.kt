package com.mian.accountrecord.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable pull-to-refresh wrapper using Material 3 PullToRefreshBox.
 *
 * Wraps [content] in a PullToRefreshBox that shows a circular loading
 * indicator when the user pulls down, and calls [onRefresh] to reload data.
 *
 * Requirements: 7.24, 7.25, 7.26
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        content()
    }
}
