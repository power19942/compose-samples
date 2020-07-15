/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.compose.jetchat.conversation

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.onCommit
import androidx.compose.setValue
import androidx.ui.core.Alignment
import androidx.ui.core.ContentScale
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.clip
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.rawDragGestureFilter
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Image
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.Text
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.clickable
import androidx.ui.foundation.drawBorder
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope.gravity
import androidx.ui.layout.RowScope.weight
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.height
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.layout.relativePaddingFrom
import androidx.ui.material.Divider
import androidx.ui.material.EmphasisAmbient
import androidx.ui.material.MaterialTheme
import androidx.ui.material.ProvideEmphasis
import androidx.ui.material.Surface
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.outlined.Info
import androidx.ui.material.icons.outlined.Search
import androidx.ui.res.imageResource
import androidx.ui.res.stringResource
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.semantics.accessibilityLabel
import androidx.ui.text.LastBaseline
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import com.example.compose.jetchat.R
import com.example.compose.jetchat.components.JetchatAppBar
import com.example.compose.jetchat.data.exampleUiState
import com.example.compose.jetchat.theme.JetchatTheme
import com.example.compose.jetchat.theme.elevatedSurface

/**
 * Entry point for a conversation screen.
 *
 * @param uiState [ConversationUiState] that contains messages to display
 * @param navigateToProfile User action when navigation to a profile is requested
 * @param modifier [Modifier] to apply to this layout node
 * @param onNavIconPressed Sends an event up when the user clicks on the menu
 */
@Composable
fun ConversationContent(
    uiState: ConversationUiState,
    navigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavIconPressed: () -> Unit = { }
) {
    val authorMe = stringResource(R.string.author_me)
    val timeNow = stringResource(id = R.string.now)
    Surface(modifier = modifier) {
        Stack(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Messages(
                    messages = uiState.messages,
                    navigateToProfile = navigateToProfile,
                    modifier = Modifier.weight(1f)
                )
                UserInput(
                    onMessageSent = { content ->
                        uiState.addMessage(
                            Message(authorMe, content, timeNow)
                        )
                    }
                )
            }
            // Channel name bar floats above the messages
            ChannelNameBar(
                channelName = uiState.channelName,
                channelMembers = uiState.channelMembers,
                modifier = Modifier.gravity(Alignment.TopCenter),
                onNavIconPressed = onNavIconPressed
            )
        }
    }
}

@Composable
fun ChannelNameBar(
    channelName: String,
    channelMembers: Int,
    modifier: Modifier = Modifier,
    onNavIconPressed: () -> Unit = { }
) {
    JetchatAppBar(
        modifier = modifier,
        onNavIconPressed = onNavIconPressed,
        title = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
                    .gravity(Alignment.CenterVertically),
                horizontalGravity = Alignment.CenterHorizontally
            ) {
                // Channel name
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.subtitle1
                )
                // Number of members
                ProvideEmphasis(emphasis = EmphasisAmbient.current.medium) { // Broken - b/159017896
                    Text(
                        text = stringResource(R.string.members, channelMembers),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        },
        actions = {
            ProvideEmphasis(emphasis = EmphasisAmbient.current.medium) {
                // Search icon
                Icon(
                    asset = Icons.Outlined.Search,
                    modifier = Modifier
                        .clickable(onClick = {}) // TODO: Show not implemented dialog.
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .preferredHeight(24.dp)
                )
                // Info icon
                Icon(
                    asset = Icons.Outlined.Info,
                    modifier = Modifier
                        .clickable(onClick = {}) // TODO: Show not implemented dialog.
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .preferredHeight(24.dp)
                )
            }
        }
    )
}

@Composable
fun Messages(
    messages: List<Message>,
    navigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Scroll is reversed so a value of 0 is the bottom
    val scrollerPosition = ScrollerPosition(isReversed = true)

    // Used to tell between manual and programmatic scrolling
    var userScrolled by savedInstanceState { false }

    onCommit(userScrolled, scrollerPosition) {
        // Scroll to last message
        if (!userScrolled && // Don't scroll if the user triggered the scrolling
            !scrollerPosition.atBottom() // Don't scroll if already at the bottom
        ) {
            // Scroll smoothly after the first scroll
            scrollerPosition.smoothScrollTo(BottomScrollerPosition)
        }
    }

    Stack(modifier = modifier) {
        // Not remembering this is fine as it's cheaper to recreate
        val dragObserver = object : DragObserver {
            override fun onStart(downPosition: Offset) {
                userScrolled = true
            }
        }
        val a11yLabel = stringResource(R.string.conversation_desc)
        VerticalScroller(
            scrollerPosition = scrollerPosition,
            // Using [rawDragGestureFilter] so [DragObserver.onStart] is called immediately,
            modifier = Modifier
                .semantics { accessibilityLabel = a11yLabel }
                .rawDragGestureFilter(dragObserver = dragObserver)
                .fillMaxWidth()
        ) {
            val authorMe = stringResource(id = R.string.author_me)
            Column {
                Spacer(modifier = Modifier.preferredHeight(64.dp))
                messages.forEachIndexed { index, content ->
                    val prevAuthor = messages.getOrNull(index - 1)?.author
                    val nextAuthor = messages.getOrNull(index + 1)?.author
                    val isFirstMessageByAuthor = prevAuthor != content.author
                    val isLastMessageByAuthor = nextAuthor != content.author

                    // Hardcode day dividers for simplicity
                    if (index == 0) {
                        DayHeader("20 Aug")
                    } else if (index == 4) {
                        DayHeader("Today")
                    }

                    Message(
                        onAuthorClick = { navigateToProfile(content.author) },
                        msg = content,
                        isUserMe = content.author == authorMe,
                        isFirstMessageByAuthor = isFirstMessageByAuthor,
                        isLastMessageByAuthor = isLastMessageByAuthor
                    )
                }
            }
        }
        // Jump to bottom button shows up when user scrolls past a threshold.
        // Convert to pixels:
        val jumpThreshold = with(DensityAmbient.current) {
            JumpToBottomThreshold.toPx()
        }

        // Apply the threshold:
        val jumpToBottomButtonEnabled = scrollerPosition.value > jumpThreshold

        JumpToBottom(
            // Only show if the scroller is not at the bottom
            enabled = jumpToBottomButtonEnabled,
            onClicked = {
                // Reset the userScrolled flag, which is preventing the auto scroll
                userScrolled = false
            },
            modifier = Modifier.gravity(Alignment.BottomCenter)
        )
    }
}

@Composable
fun Message(
    onAuthorClick: () -> Unit,
    msg: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean
) {
    // TODO: get image from msg.author
    val image = if (isUserMe) {
        imageResource(id = R.drawable.ali)
    } else {
        imageResource(id = R.drawable.someone_else)
    }
    val borderColor = if (isUserMe) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondary
    }

    val spaceBetweenAuthors = if (isFirstMessageByAuthor) Modifier.padding(top = 4.dp) else Modifier
    Row(modifier = spaceBetweenAuthors) {
        if (isFirstMessageByAuthor) {
            // Avatar
            Image(
                modifier = Modifier
                    .clickable(onClick = onAuthorClick)
                    .padding(horizontal = 16.dp)
                    .preferredSize(42.dp)
                    // TODO: border behavior will change in b/158160576
                    .drawBorder(1.5.dp, borderColor, CircleShape)
                    .drawBorder(3.dp, MaterialTheme.colors.surface, CircleShape)
                    .clip(CircleShape)
                    .gravity(Alignment.Top),
                asset = image,
                contentScale = ContentScale.Crop
            )
        } else {
            // Space under avatar
            Spacer(modifier = Modifier.preferredWidth(74.dp))
        }
        AuthorAndTextMessage(
            msg = msg,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    msg: Message,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isFirstMessageByAuthor) {
            AuthorNameTimestamp(msg)
        }
        ChatItemBubble(msg, isLastMessageByAuthor)
        if (isLastMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.preferredHeight(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.preferredHeight(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(msg: Message) {
    Row {
        ProvideEmphasis(emphasis = EmphasisAmbient.current.high) {
            Text(
                text = msg.author,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
                    .alignWithSiblings(LastBaseline)
                    .relativePaddingFrom(LastBaseline, after = 8.dp) // Space to 1st bubble
            )
        }
        Spacer(modifier = Modifier.preferredWidth(8.dp))
        ProvideEmphasis(emphasis = EmphasisAmbient.current.medium) {
            Text(
                text = msg.timestamp,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.alignWithSiblings(LastBaseline)
            )
        }
    }
}

private val ChatBubbleShape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp)
private val LastChatBubbleShape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp)

@Composable
fun DayHeader(dayString: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).preferredHeight(16.dp)) {
        DayHeaderLine()
        ProvideEmphasis(emphasis = EmphasisAmbient.current.medium) {
            Text(
                text = dayString,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.overline
            )
        }
        DayHeaderLine()
    }
}

@Composable
private fun DayHeaderLine() {
    Divider(
        modifier = Modifier.weight(1f).gravity(Alignment.CenterVertically),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
fun ChatItemBubble(
    message: Message,
    lastMessageByAuthor: Boolean
) {
    val backgroundBubbleColor =
        if (MaterialTheme.colors.isLight) {
            Color(0xFFF5F5F5)
        } else {
            MaterialTheme.colors.elevatedSurface(2.dp)
        }

    val bubbleShape = if (lastMessageByAuthor) LastChatBubbleShape else ChatBubbleShape
    Column {
        Surface(color = backgroundBubbleColor, shape = bubbleShape) {
            ProvideEmphasis(emphasis = EmphasisAmbient.current.high) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        message.image?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(color = backgroundBubbleColor, shape = bubbleShape) {
                Image(
                    asset = imageResource(it),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.preferredSize(160.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ConversationPreview() {
    JetchatTheme {
        ConversationContent(
            uiState = exampleUiState,
            navigateToProfile = { }
        )
    }
}

@Preview
@Composable
fun channelBarPrev() {
    JetchatTheme {
        ChannelNameBar(channelName = "composers", channelMembers = 52)
    }
}

@Preview
@Composable
fun DayHeaderPrev() {
    DayHeader("Aug 6")
}

private val JumpToBottomThreshold = 56.dp
private val BottomScrollerPosition = 0f

private fun ScrollerPosition.atBottom(): Boolean = value == BottomScrollerPosition
