package com.example.myapp // Or your preferred package for admin screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment // Icon for Incident Management
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.example.myapp.ui.theme.MyAppTheme

// Data class for individual dashboard items
data class DashboardItem(
    val title: String,
    val count: Int? = null, // Make count optional for items like navigation
    val icon: ImageVector,
    val color: Color,
    val onClick: (() -> Unit)? = null // Optional onClick for navigation items
)

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AdminDashboardScreenReal(
//    adminViewModel: AdminViewModel = viewModel(),
//    onNavigateToUserManagement: () -> Unit,
//    onNavigateToNotificationManagement: () -> Unit,
//    onNavigateToIncidentManagement: () -> Unit, // New navigation lambda for incidents
//    onLogout: () -> Unit
//) {
//    val analytics by adminViewModel.analytics.collectAsState()
//    val isLoading by adminViewModel.isLoading.collectAsState()
//    val error by adminViewModel.error.collectAsState()
//
//    val dashboardItems = listOf(
//        DashboardItem("Total Users", analytics.totalUsers, Icons.Filled.Group, MaterialTheme.colorScheme.primary),
//        DashboardItem("Student Users", analytics.studentUsers, Icons.Filled.VerifiedUser, Color(0xFF2196F3)),
//        DashboardItem("Security Users", analytics.securityUsers, Icons.Filled.Security, Color(0xFF4CAF50)),
//        DashboardItem("Total SOS Alerts", analytics.totalSosAlerts, Icons.Filled.NotificationsActive, MaterialTheme.colorScheme.secondary),
//        DashboardItem("Pending Alerts", analytics.pendingSosAlerts, Icons.Filled.Shield, Color(0xFFFF9800)),
//        DashboardItem("Resolved Alerts", analytics.resolvedSosAlerts, Icons.Filled.VerifiedUser, Color(0xFF795548)),
//        DashboardItem(
//            title = "Manage Users",
//            icon = Icons.Filled.ManageAccounts,
//            color = MaterialTheme.colorScheme.tertiaryContainer, // Or a distinct color
//            onClick = onNavigateToUserManagement
//        ),
//        DashboardItem(
//            title = "Manage Notifications",
//            icon = Icons.Filled.Notifications,
//            color = Color(0xFF00ACC1), // Cyan
//            onClick = onNavigateToNotificationManagement
//        ),
//        DashboardItem( // New item for Incident Management
//            title = "Manage Incidents",
//            icon = Icons.Filled.Assessment, // Example icon, choose one that fits
//            color = Color(0xFFF06292), // Example color (Pink)
//            onClick = onNavigateToIncidentManagement
//        )
//    )
//
//    MyAppTheme {
//        Scaffold(
//            topBar = {
//                TopAppBar(
//                    title = { Text("Admin Dashboard") },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.primaryContainer,
//                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
//                    ),
//                    actions = {
//                        IconButton(onClick = onLogout) {
//                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
//                        }
//                    }
//                )
//            }
//        ) { paddingValues ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(paddingValues)
//                    .padding(horizontal = 16.dp, vertical = 8.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                if (isLoading && analytics == AdminDashboardAnalytics()) {
//                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
//                } else {
//                    error?.let {
//                        Text(
//                            text = "Error loading analytics: $it",
//                            color = MaterialTheme.colorScheme.error,
//                            modifier = Modifier.padding(bottom = 16.dp),
//                            textAlign = TextAlign.Center
//                        )
//                    }
//
//                    Button(
//                        onClick = { adminViewModel.fetchDashboardAnalytics() },
//                        modifier = Modifier.padding(bottom = 16.dp)
//                    ) {
//                        Text("Refresh Analytics")
//                    }
//
//                    LazyVerticalGrid(
//                        columns = GridCells.Fixed(2), // Adjust number of columns if needed
//                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
//                        verticalArrangement = Arrangement.spacedBy(12.dp),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        items(dashboardItems) { item ->
//                            AnalyticsTile(item = item)
//                        }
//                    }
//                }
//                // Optional: Add Spacer if content is short and you want to push it up
//                if (dashboardItems.size <= 6) { // Adjust this condition based on your layout preference
//                    Spacer(Modifier.weight(1f))
//                }
//            }
//        }
//    }
//}

// AnalyticsTile Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTile(item: DashboardItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable(enabled = item.onClick != null) {
                item.onClick?.invoke()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = if (item.count != null) 0.1f else 0.2f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(if (item.count != null) 36.dp else 48.dp),
                tint = item.color // Ensure icon tint is applied
            )
            Text(
                text = item.title,
                fontSize = if (item.count != null) 14.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = item.color, // Ensure text color is applied
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            item.count?.let {
                Text(
                    text = it.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.color, // Ensure text color is applied
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
