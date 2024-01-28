package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadList
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Destination
@Composable
fun ForumSearchPostPage(
    forumName: String,
    forumId: Long,
    navigator: DestinationsNavigator,
    viewModel: ForumSearchPostViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val currentKeyword by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::keyword,
        initial = ""
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::error,
        initial = null
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::data,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isLoadingMore,
        initial = false
    )
    val currentSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::sortType,
        initial = ForumSearchPostSortType.NEWEST
    )
    val currentFilterType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::filterType,
        initial = ForumSearchPostFilterType.ALL
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::hasMore,
        initial = true
    )
    val isEmpty by remember {
        derivedStateOf { data.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }
    val isKeywordEmpty by remember {
        derivedStateOf { currentKeyword.isEmpty() }
    }
    var inputKeyword by remember { mutableStateOf("") }

    fun refresh() {
        viewModel.send(
            ForumSearchPostUiIntent.Refresh(
                currentKeyword,
                forumName,
                forumId,
                currentSortType,
                currentFilterType
            )
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = ::refresh
    )
    val lazyListState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val sortTypeMapping = remember {
        mapOf(
            ForumSearchPostSortType.NEWEST to context.getString(R.string.title_search_post_sort_by_time),
            ForumSearchPostSortType.RELATIVE to context.getString(R.string.title_search_post_sort_by_relevant),
        )
    }
    val filterTypeMapping = remember {
        mapOf(
            ForumSearchPostFilterType.ALL to context.getString(R.string.title_search_filter_all),
            ForumSearchPostFilterType.ONLY_THREAD to context.getString(R.string.title_search_filter_only_thread),
        )
    }

    MyScaffold(
        topBar = {
            TopAppBarContainer(
                topBar = {
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .background(ExtendedTheme.colors.topBar)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SearchBox(
                            keyword = inputKeyword,
                            onKeywordChange = { inputKeyword = it },
                            modifier = Modifier.fillMaxSize(),
                            onKeywordSubmit = {
                                focusRequester.freeFocus()
                                keyboardController?.hide()
                                viewModel.send(
                                    ForumSearchPostUiIntent.Refresh(
                                        it,
                                        forumName,
                                        forumId,
                                        currentSortType,
                                        currentFilterType
                                    )
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(
                                        id = R.string.hint_search_in_ba,
                                        forumName
                                    ),
                                    color = ExtendedTheme.colors.onTopBarSurface.copy(alpha = ContentAlpha.medium)
                                )
                            },
                            prependIcon = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = rememberRipple(bounded = false, 24.dp),
                                            role = Role.Button,
                                            onClick = { navigator.navigateUp() }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(id = R.string.button_back)
                                    )
                                }
                            },
                            focusRequester = focusRequester,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProvideNavigator(navigator = navigator) {
                if (!isKeywordEmpty) {
                    StateScreen(
                        modifier = Modifier.fillMaxSize(),
                        isEmpty = isEmpty,
                        isError = isError,
                        isLoading = isRefreshing,
                        onReload = ::refresh,
                        errorScreen = {
                            error?.item?.let {
                                ErrorScreen(error = it)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pullRefresh(pullRefreshState)
                        ) {
                            LoadMoreLayout(
                                isLoading = isLoadingMore,
                                onLoadMore = {
                                    viewModel.send(
                                        ForumSearchPostUiIntent.LoadMore(
                                            currentKeyword,
                                            forumName,
                                            forumId,
                                            currentPage,
                                            currentSortType,
                                            currentFilterType
                                        )
                                    )
                                },
                                loadEnd = !hasMore,
                                lazyListState = lazyListState,
                            ) {
                                SearchThreadList(
                                    data = data,
                                    lazyListState = lazyListState,
                                    onItemClick = {
                                        navigator.navigate(
                                            ThreadPageDestination(
                                                threadId = it.tid.toLong()
                                            )
                                        )
                                    },
                                    onItemUserClick = {
                                        navigator.navigate(UserProfilePageDestination(it.userId.toLong()))
                                    },
                                    onItemForumClick = {
                                        navigator.navigate(
                                            ForumPageDestination(
                                                it.forumName
                                            )
                                        )
                                    },
                                    hideForum = true,
                                ) {
                                    stickyHeader(key = "Sort&Filter") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(ExtendedTheme.colors.background)
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {}
                                                )
                                        ) {
                                            val menuState = rememberMenuState()

                                            val rotate by animateFloatAsState(
                                                targetValue = if (menuState.expanded) 180f else 0f,
                                                label = "ArrowIndicatorRotate"
                                            )

                                            ClickMenu(
                                                menuContent = {
                                                    ListSinglePicker(
                                                        itemTitles = sortTypeMapping.values.toImmutableList(),
                                                        itemValues = sortTypeMapping.keys.toImmutableList(),
                                                        selectedPosition = sortTypeMapping.keys.indexOf(
                                                            currentSortType
                                                        ),
                                                        onItemSelected = { _, _, newSortType, changed ->
                                                            if (changed) {
                                                                viewModel.send(
                                                                    ForumSearchPostUiIntent.Refresh(
                                                                        currentKeyword,
                                                                        forumName,
                                                                        forumId,
                                                                        newSortType,
                                                                        currentFilterType
                                                                    )
                                                                )
                                                            }
                                                            dismiss()
                                                        }
                                                    )
                                                },
                                                menuState = menuState,
                                                indication = null
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
//                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = sortTypeMapping[currentSortType]
                                                            ?: "",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Rounded.ArrowDropDown,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .rotate(rotate)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(IntrinsicSize.Min)
                                            ) {
                                                filterTypeMapping.keys.map<Int, @Composable () -> Unit> { type ->
                                                    {
                                                        Text(
                                                            text = filterTypeMapping[type] ?: "",
                                                            fontSize = 13.sp,
                                                            fontWeight = if (type == currentFilterType) {
                                                                FontWeight.Bold
                                                            } else {
                                                                FontWeight.Normal
                                                            },
                                                            modifier = Modifier.clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null,
                                                                role = Role.RadioButton,
                                                                onClick = {
                                                                    if (type != currentFilterType) {
                                                                        viewModel.send(
                                                                            ForumSearchPostUiIntent.Refresh(
                                                                                currentKeyword,
                                                                                forumName,
                                                                                forumId,
                                                                                currentSortType,
                                                                                type
                                                                            )
                                                                        )
                                                                    }
                                                                }
                                                            )
                                                        )
                                                    }
                                                }.forEachIndexed { index, composable ->
                                                    composable()
                                                    if (index < filterTypeMapping.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(
                                                                horizontal = 8.dp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            PullRefreshIndicator(
                                refreshing = isRefreshing,
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter),
                                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                                contentColor = ExtendedTheme.colors.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}