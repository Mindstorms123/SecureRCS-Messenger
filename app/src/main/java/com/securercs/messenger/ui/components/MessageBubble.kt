package com.securercs.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securercs.messenger.data.model.MessageStatus
import com.securercs.messenger.ui.theme.BubbleIncoming
import com.securercs.messenger.ui.theme.BubbleOutgoing

@Composable
fun MessageBubble(
    content: String,
    timestamp: String,
    isOutgoing: Boolean,
    status: MessageStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .background(
                    color = if (isOutgoing) BubbleOutgoing else BubbleIncoming,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isOutgoing) 12.dp else 0.dp,
                        bottomEnd = if (isOutgoing) 0.dp else 12.dp,
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = content,
                color = Color.White,
                fontSize = 15.sp,
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = timestamp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                if (isOutgoing) {
                    when (status) {
                        MessageStatus.SENDING -> {}
                        MessageStatus.SENT -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Gesendet",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        MessageStatus.DELIVERED -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Zugestellt",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        MessageStatus.READ -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Gelesen",
                                tint = Color(0xFF00BFFF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
