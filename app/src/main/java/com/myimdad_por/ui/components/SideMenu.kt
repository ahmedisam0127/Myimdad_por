package com.myimdad_por.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.navigation.ScreenRoutes
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.DividerColor
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.IconColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor

private enum class MenuSection {
    MAIN,
    OPERATIONS,
    SYSTEM
}

private data class SideMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val section: MenuSection
)

@Composable
fun SideMenu(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    drawerShape: Shape = MaterialTheme.shapes.extraLarge
) {
    val items = remember {
        listOf(
            SideMenuItem(
                route = ScreenRoutes.Dashboard,
                title = "لوحة التحكم",
                icon = Icons.Filled.Dashboard,
                section = MenuSection.MAIN
            ),
            SideMenuItem(
                route = ScreenRoutes.Customers,
                title = "العملاء",
                icon = Icons.Filled.People,
                section = MenuSection.MAIN
            ),
            SideMenuItem(
                route = ScreenRoutes.Inventory,
                title = "المخزون",
                icon = Icons.Filled.Inventory,
                section = MenuSection.MAIN
            ),
            SideMenuItem(
                route = ScreenRoutes.Sales,
                title = "المبيعات",
                icon = Icons.Filled.ReceiptLong,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Purchases,
                title = "المشتريات",
                icon = Icons.Filled.ShoppingCart,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Payments,
                title = "المدفوعات",
                icon = Icons.Filled.AccountBalanceWallet,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Returns,
                title = "المرتجعات",
                icon = Icons.Filled.AssignmentReturn,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Expenses,
                title = "المصروفات",
                icon = Icons.Filled.Receipt,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Accounting,
                title = "المحاسبة",
                icon = Icons.Filled.AccountBalance,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Suppliers,
                title = "الموردون",
                icon = Icons.Filled.LocalShipping,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Reports,
                title = "التقارير",
                icon = Icons.Filled.BarChart,
                section = MenuSection.OPERATIONS
            ),
            SideMenuItem(
                route = ScreenRoutes.Subscription,
                title = "الاشتراكات",
                icon = Icons.Filled.WorkspacePremium,
                section = MenuSection.SYSTEM
            ),
            SideMenuItem(
                route = ScreenRoutes.Security,
                title = "الحماية والأمان",
                icon = Icons.Filled.Security,
                section = MenuSection.SYSTEM
            ),
            SideMenuItem(
                route = ScreenRoutes.Settings,
                title = "الإعدادات",
                icon = Icons.Filled.Settings,
                section = MenuSection.SYSTEM
            )
        )
    }

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 360.dp),
        drawerShape = drawerShape,
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.Layout.screenPadding,
                    vertical = AppDimens.Layout.sectionPadding
                )
        ) {
            DrawerHeader()

            Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

            MenuSectionHeader(title = "الرئيسية")
            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            items.filter { it.section == MenuSection.MAIN }.forEach { item ->
                SideMenuRow(
                    title = item.title,
                    icon = item.icon,
                    selected = isRouteSelected(currentRoute, item.route),
                    onClick = { onNavigate(item.route) }
                )
                Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            MenuSectionHeader(title = "العمليات")
            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            items.filter { it.section == MenuSection.OPERATIONS }.forEach { item ->
                SideMenuRow(
                    title = item.title,
                    icon = item.icon,
                    selected = isRouteSelected(currentRoute, item.route),
                    onClick = { onNavigate(item.route) }
                )
                Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            MenuSectionHeader(title = "النظام")
            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            items.filter { it.section == MenuSection.SYSTEM }.forEach { item ->
                SideMenuRow(
                    title = item.title,
                    icon = item.icon,
                    selected = isRouteSelected(currentRoute, item.route),
                    onClick = { onNavigate(item.route) }
                )
                Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))

            Divider(
                color = DividerColor,
                thickness = AppDimens.Component.dividerThickness,
                modifier = Modifier.padding(vertical = AppDimens.Spacing.small)
            )

            LogoutRow(onClick = onLogout)
        }
    }
}

@Composable
private fun DrawerHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = AppDimens.Elevation.low
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BrandPrimaryTint,
                            MaterialTheme.colorScheme.primaryContainer,
                            BrandPrimarySoft
                        )
                    )
                )
                .padding(AppDimens.Layout.sectionPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = BrandPrimary,
                    tonalElevation = 0.dp,
                    shadowElevation = AppDimens.Elevation.low,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "MP",
                            style = MaterialTheme.typography.titleMedium,
                            color = WhiteColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "إمداد بور",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))

                    Text(
                        text = "تنقل سريع، أوضح، وأكثر احترافية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Layout.listItemHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondaryColor,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(DividerColor)
        )
    }
}

@Composable
private fun SideMenuRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "side_menu_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "side_menu_content"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) BrandPrimary else IconColor,
        label = "side_menu_icon"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) BrandPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = MaterialTheme.shapes.large,
        shadowElevation = if (selected) AppDimens.Elevation.low else AppDimens.Elevation.none,
        tonalElevation = if (selected) AppDimens.Elevation.low else AppDimens.Elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.Component.buttonHeight)
                .padding(
                    horizontal = AppDimens.Layout.listItemHorizontalPadding,
                    vertical = AppDimens.Layout.listItemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (selected) BrandPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 18.dp)
                        .background(
                            color = BrandPrimary,
                            shape = MaterialTheme.shapes.large
                        )
                )
            }
        }
    }
}

@Composable
private fun LogoutRow(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = ErrorContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = AppDimens.Elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.Component.buttonHeight)
                .padding(
                    horizontal = AppDimens.Layout.listItemHorizontalPadding,
                    vertical = AppDimens.Layout.listItemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = null,
                tint = ErrorColor
            )

            Text(
                text = "تسجيل الخروج",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = ErrorColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun isRouteSelected(currentRoute: String?, targetRoute: String): Boolean {
    if (currentRoute.isNullOrBlank()) return false
    return currentRoute == targetRoute || currentRoute.startsWith("$targetRoute/")
}