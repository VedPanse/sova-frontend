package org.sova.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import sova.composeapp.generated.resources.Res
import sova.composeapp.generated.resources.sova_mascot
import sova.composeapp.generated.resources.sova_mascot_eyes_closed
import sova.composeapp.generated.resources.sova_mascot_smiling

enum class SovaMascotMood {
    Calm,
    Peek,
    Smile,
}

@Composable
fun SovaMascotCompanion(
    message: String,
    modifier: Modifier = Modifier,
    mood: SovaMascotMood = SovaMascotMood.Smile,
    title: String = "Sova",
) {
    val transition = rememberInfiniteTransition(label = "sova-companion")
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "sova-companion-bob",
    )
    val peek by transition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(animation = tween(1300), repeatMode = RepeatMode.Reverse),
        label = "sova-companion-peek",
    )
    val image = when (mood) {
        SovaMascotMood.Calm -> Res.drawable.sova_mascot
        SovaMascotMood.Peek -> Res.drawable.sova_mascot_eyes_closed
        SovaMascotMood.Smile -> Res.drawable.sova_mascot_smiling
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = HealthColors.Surface,
        shape = HealthShapes.Card,
        shadowElevation = HealthSpacing.None,
    ) {
        Row(
            modifier = Modifier.padding(HealthSpacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MascotPortrait(
                image = image,
                mood = mood,
                bob = bob,
                peek = peek,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
            ) {
                Text(title, color = HealthColors.TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text(message, color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun MascotPortrait(
    image: DrawableResource,
    mood: SovaMascotMood,
    bob: Float,
    peek: Float,
) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(HealthShapes.Pill)
            .background(HealthColors.AccentSoft),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Crossfade(targetState = image, label = "sova-companion-image") { target ->
            Image(
                painter = painterResource(target),
                contentDescription = "Sova mascot",
                modifier = Modifier
                    .size(72.dp)
                    .offset(y = if (mood == SovaMascotMood.Peek) (14 - peek).dp else bob.dp)
                    .graphicsLayer {
                        rotationZ = if (mood == SovaMascotMood.Peek) -2f else bob * 0.32f
                    },
            )
        }
    }
}
