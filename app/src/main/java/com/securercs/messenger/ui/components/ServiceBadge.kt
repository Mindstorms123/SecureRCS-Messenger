package com.securercs.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServiceBadge(
    service: String,
    backgroundColor: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = Color(backgroundColor).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = service.uppercase(),
            color = Color(backgroundColor),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}
