package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadUserManagementTab(
    onNavigateToCreateUser: () -> Unit,
    onNavigateToViewAllUsers: () -> Unit,
    onNavigateToRoleManagement: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // iOS-style header - Title at top only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "User Management",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Large icon below the title - Three person silhouettes
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                // Three person silhouettes - one solid, two outlined
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // First user (solid blue)
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Second user (lighter blue outline)
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Third user (lighter blue outline)
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle under the icon - Bold, centered
            Text(
                text = "User Management",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Description - Medium-light gray, smaller, centered
            Text(
                text = "Manage users, roles, and permissions",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Management options cards with increased spacing
            UserManagementCard(
                icon = Icons.Default.PersonAdd,
                iconColor = Color(0xFF1976D2),
                iconBackgroundColor = Color(0xFFE3F2FD),
                title = "Create New User",
                description = "Add a new user to the system with appropriate role",
                onClick = onNavigateToCreateUser
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            UserManagementCard(
                icon = Icons.Default.Groups,
                iconColor = Color(0xFF4CAF50),
                iconBackgroundColor = Color(0xFFE8F5E9),
                title = "View All Users",
                description = "Browse and manage existing users",
                onClick = onNavigateToViewAllUsers
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            UserManagementCard(
                icon = Icons.Default.VerifiedUser,
                iconColor = Color(0xFF9C27B0),
                iconBackgroundColor = Color(0xFFF3E5F5),
                title = "Role Management",
                description = "Configure user roles and permissions",
                onClick = onNavigateToRoleManagement
            )
        }
    }
}

@Composable
private fun UserManagementCard(
    icon: ImageVector,
    iconColor: Color,
    iconBackgroundColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x40000000)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with color background circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Thin, soft gray arrow icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
