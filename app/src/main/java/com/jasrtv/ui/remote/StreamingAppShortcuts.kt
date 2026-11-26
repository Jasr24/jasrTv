package com.jasrtv.ui.remote

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jasrtv.R
import com.jasrtv.data.remote.StreamingApp

@Composable
internal fun StreamingAppShortcuts(enabled: Boolean, onLaunch: (StreamingApp) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (app in StreamingApp.entries) {
            val image = when (app) {
                StreamingApp.NETFLIX -> R.drawable.app_netflix
                StreamingApp.CRUNCHYROLL -> R.drawable.app_crunchyroll
                StreamingApp.DISNEY_PLUS -> R.drawable.app_disney_plus
                StreamingApp.PRIME_VIDEO -> R.drawable.app_prime_video
            }
            IconButton(onClick = { onLaunch(app) }, enabled = enabled,
                modifier = Modifier.weight(1f).height(56.dp)) {
                Image(painterResource(image), contentDescription = "Abrir ${app.label}",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(11.dp))
                        .alpha(if (enabled) 1f else 0.4f))
            }
        }
    }
}
