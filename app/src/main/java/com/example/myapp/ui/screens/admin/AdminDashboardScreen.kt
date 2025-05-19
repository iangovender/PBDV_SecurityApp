package com.example.myapp.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapp.AdminDashboardAnalytics
import com.example.myapp.AdminViewModel
import com.example.myapp.ui.theme.MyAppTheme
import com.google.firebase.firestore.PropertyName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel = viewModel(),
    onNavigateToUserManagement: () -> Unit,
    onNavigateToNotificationManagement: () -> Unit,
    onNavigateToIncidentManagement: () -> Unit,
    onLogout: () -> Unit
) {
    val analytics by adminViewModel.analytics.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val error by adminViewModel.error.collectAsState()

    val dashboardItems = listOf(
//        DashboardItem("Total Users", analytics.totalUsers, Icons.Filled.Group,
//            MaterialTheme.colorScheme.primary),
//        DashboardItem("Students", analytics.studentUsers, Icons.Filled.School,
//            Color(0xFF2196F3)),
//        DashboardItem("Security", analytics.securityUsers, Icons.Filled.Security,
//            Color(0xFF4CAF50)),
//        DashboardItem("SOS Alerts", analytics.totalSosAlerts, Icons.Filled.NotificationsActive,
//            MaterialTheme.colorScheme.secondary),
//        DashboardItem("Pending", analytics.pendingSosAlerts, Icons.Filled.Warning,
//            Color(0xFFFF9800)),
//        DashboardItem("Resolved", analytics.resolvedSosAlerts, Icons.Filled.CheckCircle,
//            Color(0xFF388E3C)),
        DashboardItem(
            title = "User Management",
            icon = Icons.Filled.ManageAccounts,
            color = MaterialTheme.colorScheme.tertiary,
            onClick = onNavigateToUserManagement
        ),
        DashboardItem(
            title = "Notifications",
            icon = Icons.Filled.Notifications,
            color = Color(0xFF00ACC1),
            onClick = onNavigateToNotificationManagement
        ),
        DashboardItem(
            title = "Incidents",
            icon = Icons.Filled.Assignment,
            color = Color(0xFFAB47BC),
            onClick = onNavigateToIncidentManagement
        ),
//        DashboardItem(
//            title = "Reports",
//            icon = Icons.Filled.Analytics,
//            color = Color(0xFFFF7043),
//            onClick = { /* TODO: Add navigation */ }
//        )
    )

    MyAppTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Admin Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Section
                if (isLoading && analytics == AdminDashboardAnalytics()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                    }
                } else {
//                    error?.let {
//                        AlertCard(
//                            message = "Error loading analytics: $it",
//                            onRetry = { adminViewModel.fetchDashboardAnalytics() }
//                        )
//                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Refresh Button
                    OutlinedButton(
                        onClick = { adminViewModel.fetchDashboardAnalytics() },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh Data")
                    }

                    // Dashboard Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dashboardItems) { item ->
                            AnalyticsTile(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onRetry,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTile(item: DashboardItem) {
    val isActionItem = item.onClick != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isActionItem) 120.dp else 140.dp)
            .clickable(enabled = isActionItem) { item.onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActionItem) 2.dp else 4.dp,
            pressedElevation = if (isActionItem) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActionItem)
                item.color.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = if (isActionItem) item.color else MaterialTheme.colorScheme.primary
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isActionItem) item.color else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                item.count?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = item.color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

data class DashboardItem(
    val title: String,
    val count: Int? = null,
    val icon: ImageVector,
    val color: Color,
    val onClick: (() -> Unit)? = null
)