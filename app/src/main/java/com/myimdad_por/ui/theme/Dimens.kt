package com.myimdad_por.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for spacing, sizing, radius, elevation and typography.
 * Keep all UI measurements centralized here to ensure consistency across the app.
 */
object AppDimens {

    object Spacing {
        val none: Dp = 0.dp
        val extraSmall: Dp = 4.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val normal: Dp = 16.dp
        val large: Dp = 20.dp
        val extraLarge: Dp = 24.dp
        val huge: Dp = 32.dp
        val xLarge: Dp = 40.dp
        val xxLarge: Dp = 48.dp
    }

    object Radius {
        val none: Dp = 0.dp
        val small: Dp = 8.dp
        val medium: Dp = 12.dp
        val large: Dp = 16.dp
        val extraLarge: Dp = 20.dp
        val round: Dp = 999.dp
    }

    object Elevation {
        val none: Dp = 0.dp
        val low: Dp = 2.dp
        val medium: Dp = 4.dp
        val high: Dp = 8.dp
        val veryHigh: Dp = 12.dp
    }

    object Icon {
        val tiny: Dp = 14.dp
        val small: Dp = 18.dp
        val normal: Dp = 24.dp
        val large: Dp = 28.dp
        val extraLarge: Dp = 32.dp
        val avatar: Dp = 40.dp
        val fab: Dp = 56.dp
    }

    object Component {
        val chipHeight: Dp = 32.dp
        val buttonHeight: Dp = 48.dp
        val textFieldHeight: Dp = 56.dp
        val topBarHeight: Dp = 64.dp
        val bottomBarHeight: Dp = 72.dp

        val avatarSmall: Dp = 32.dp
        val avatarMedium: Dp = 40.dp
        val avatarLarge: Dp = 56.dp

        val cardMinHeight: Dp = 72.dp
        val dialogMaxWidth: Dp = 320.dp
        val dividerThickness: Dp = 1.dp
        val borderThickness: Dp = 1.dp
    }

    object Text {
        val caption = 12.sp
        val bodySmall = 13.sp
        val body = 14.sp
        val bodyLarge = 16.sp
        val titleSmall = 18.sp
        val title = 20.sp
        val headline = 24.sp
        val display = 30.sp
    }

    object Layout {
        val screenPadding: Dp = 16.dp
        val sectionPadding: Dp = 20.dp
        val listItemVerticalPadding: Dp = 12.dp
        val listItemHorizontalPadding: Dp = 16.dp
        val contentMaxWidth: Dp = 480.dp
        val imageHeight: Dp = 180.dp
        val emptyStateIconSize: Dp = 72.dp
    }

    object Chat {
        val bubbleMaxWidth: Dp = 280.dp
        val bubblePadding: Dp = 12.dp
        val messageSpacing: Dp = 8.dp
    }
}