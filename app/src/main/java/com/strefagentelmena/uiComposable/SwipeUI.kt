package com.strefagentelmena.uiComposable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import kotlinx.coroutines.async

val swipeUI = SwipeUI()
class SwipeUI {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeToDelete(dismissState: DismissState) {
        val progress = dismissState.progress

        val colorDelete =  colorsUI.amaranthPurple

        Box(
            Modifier
                .fillMaxSize()
                .background(colorDelete)
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete",
                modifier = Modifier
                    .size(35.dp)
            )
        }
    }
}