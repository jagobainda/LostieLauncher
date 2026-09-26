package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.FaqCard
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.FaqsViewModel

@Composable
fun FaqsScreen(modifier: Modifier = Modifier, viewModel: FaqsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(state.searchText) }
    val search = { text: String ->
        query = text
        viewModel.setSearchText(text)
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(LauncherSpacing.Screen),
    ) {
        FaqsSearchBar(query = query, onQueryChange = search, onClear = { search("") })
        if (state.hasNoResults) {
            EmptyState(
                icon = R.drawable.ic_faqs_search_x,
                message = LocalStrings.current.faqsNoResults,
                compact = true,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.rows, key = { it.index }) { row ->
                    FaqCard(
                        question = row.entry.question,
                        answer = row.entry.answer,
                        highlight = state.searchText,
                        expanded = row.isExpanded,
                        onToggle = { viewModel.toggleFaq(row.index) },
                        onOpenLink = viewModel::openLink,
                        modifier = Modifier.padding(bottom = LauncherSpacing.MediumLarge),
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqsSearchBar(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val colors = LocalLauncherColors.current
    val strings = LocalStrings.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(bottom = LauncherSpacing.Card)
            .fillMaxWidth()
            .height(LauncherSizes.SearchBarHeight)
            .background(colors.tertiaryBg, RoundedCornerShape(LauncherRadii.Medium))
            .padding(horizontal = LauncherSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_faqs_search),
            contentDescription = null,
            tint = colors.secondaryFgDim,
            modifier = Modifier.size(LauncherSizes.SearchIcon),
        )
        Spacer(Modifier.width(LauncherSpacing.MediumLarge))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.secondaryFg, fontSize = LauncherType.BodySize),
                cursorBrush = SolidColor(colors.secondaryFg),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = strings.faqsSearchPlaceholder },
            )
            if (query.isEmpty() && !focused) {
                Text(strings.faqsSearchPlaceholder, color = colors.secondaryFgDim, fontSize = LauncherType.BodySize)
            }
        }
        if (query.isNotEmpty()) FaqsClearButton(onClear)
    }
}

@Composable
private fun FaqsClearButton(onClick: () -> Unit) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val active = hovered || pressed
    Box(
        Modifier
            .size(LauncherSizes.SearchClearButton)
            .then(
                if (active) {
                    Modifier.background(
                        colors.overlayLight,
                        RoundedCornerShape(LauncherRadii.Small),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_faqs_clear),
            contentDescription = null,
            tint = if (active) colors.secondaryFg else colors.secondaryFgDim,
            modifier = Modifier.size(LauncherSizes.SearchClearIcon),
        )
    }
}
