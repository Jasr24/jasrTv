package com.jasrtv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import com.jasrtv.R

@Composable
internal fun AppBrand(onChooseTheme: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = "Cambiar color del control", onClick = onChooseTheme),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.jasrtv_icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
        )
        Text(buildAnnotatedString {
            append("jasr")
            withStyle(SpanStyle(color = accent)) { append("Tv") }
        }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
