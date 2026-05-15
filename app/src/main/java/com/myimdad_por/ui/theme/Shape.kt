package com.myimdad_por.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Shapes used across the app.
 * Centralized here to keep the visual language consistent with AppDimens.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.Radius.small),
    small = RoundedCornerShape(AppDimens.Radius.medium),
    medium = RoundedCornerShape(AppDimens.Radius.large),
    large = RoundedCornerShape(AppDimens.Radius.extraLarge),
    extraLarge = RoundedCornerShape(AppDimens.Radius.round)
)

/**
 * Common reusable shape tokens.
 */
object AppShapeTokens {

    val none = RoundedCornerShape(AppDimens.Radius.none)

    val tiny = RoundedCornerShape(AppDimens.Radius.small)
    val small = RoundedCornerShape(AppDimens.Radius.medium)
    val medium = RoundedCornerShape(AppDimens.Radius.large)
    val large = RoundedCornerShape(AppDimens.Radius.extraLarge)
    val extraLarge = RoundedCornerShape(AppDimens.Radius.round)

    val card = RoundedCornerShape(AppDimens.Radius.extraLarge)
    val elevatedCard = RoundedCornerShape(AppDimens.Radius.extraLarge)
    val filledCard = RoundedCornerShape(AppDimens.Radius.large)

    val button = RoundedCornerShape(AppDimens.Radius.large)
    val buttonLarge = RoundedCornerShape(AppDimens.Radius.extraLarge)
    val buttonPill = RoundedCornerShape(AppDimens.Radius.round)

    val chip = RoundedCornerShape(AppDimens.Radius.round)
    val badge = RoundedCornerShape(AppDimens.Radius.round)
    val avatar = RoundedCornerShape(AppDimens.Radius.round)

    val textField = RoundedCornerShape(AppDimens.Radius.extraLarge)
    val searchBar = RoundedCornerShape(AppDimens.Radius.extraLarge)
    val dialog = RoundedCornerShape(AppDimens.Radius.extraLarge)

    val bottomSheet = RoundedCornerShape(
        topStart = AppDimens.Radius.extraLarge,
        topEnd = AppDimens.Radius.extraLarge
    )

    val topBar = RoundedCornerShape(
        bottomStart = AppDimens.Radius.extraLarge,
        bottomEnd = AppDimens.Radius.extraLarge
    )

    val bottomBar = RoundedCornerShape(
        topStart = AppDimens.Radius.extraLarge,
        topEnd = AppDimens.Radius.extraLarge
    )

    val sheetHandle = RoundedCornerShape(AppDimens.Radius.round)
    val floatingActionButton = RoundedCornerShape(AppDimens.Radius.extraLarge)

    val chatBubbleIncoming = RoundedCornerShape(
        topStart = AppDimens.Radius.large,
        topEnd = AppDimens.Radius.large,
        bottomEnd = AppDimens.Radius.large,
        bottomStart = AppDimens.Radius.medium
    )

    val chatBubbleOutgoing = RoundedCornerShape(
        topStart = AppDimens.Radius.large,
        topEnd = AppDimens.Radius.large,
        bottomStart = AppDimens.Radius.large,
        bottomEnd = AppDimens.Radius.medium
    )
}