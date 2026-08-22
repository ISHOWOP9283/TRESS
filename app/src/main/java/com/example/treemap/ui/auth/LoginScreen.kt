package com.example.treemap.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treemap.ui.theme.MangroveDeepTeal
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.ui.theme.StatusThriving

@Composable
fun LoginScreen(
    onLogin: (emailOrUser: String, pass: String) -> Unit,
    onDirectEmailAccess: (email: String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var emailOrUsername by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isDirectAccessMode by remember { mutableStateOf(false) }

    // Dynamic Role Detection
    val detectedRole = remember(emailOrUsername) {
        val trimmed = emailOrUsername.trim().lowercase()
        when {
            trimmed.isBlank() -> null
            trimmed == "admin" || trimmed.startsWith("admin@") -> "Admin Account"
            trimmed.contains("@") -> "Community Volunteer"
            else -> "Field User"
        }
    }

    val isAdminRole = remember(emailOrUsername) {
        val trimmed = emailOrUsername.trim().lowercase()
        trimmed == "admin" || trimmed.startsWith("admin@")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MangroveDeepTeal,
                        Color(0xFF071F1B),
                        Color(0xFF03100E)
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo & Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MangroveTealPrimary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MangroveTealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forest,
                        contentDescription = "Mangrove Community Mapper",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Community Mapper",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            )

            Text(
                text = "Mangrove Monitoring & Coastal Protection",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.75f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDirectAccessMode) "Instant Email Access" else "Sign In",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        // Role Detection Badge
                        if (detectedRole != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAdminRole) Color(0xFFFFE082) else MangroveTealPrimary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAdminRole) Icons.Default.AdminPanelSettings else Icons.Default.VolunteerActivism,
                                        contentDescription = null,
                                        tint = if (isAdminRole) Color(0xFFE65100) else MangroveTealPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = detectedRole,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdminRole) Color(0xFFE65100) else MangroveTealPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isDirectAccessMode)
                            "Enter any volunteer or Google email to access field tools immediately"
                        else
                            "Log in with username 'admin' & 'admin', or your volunteer account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Error Banner
                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Email / Username Input
                    OutlinedTextField(
                        value = emailOrUsername,
                        onValueChange = { emailOrUsername = it },
                        label = { Text("Username or Email") },
                        placeholder = { Text(if (isDirectAccessMode) "volunteer@example.com" else "admin") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isDirectAccessMode) Icons.Default.AlternateEmail else Icons.Default.Person,
                                contentDescription = null,
                                tint = MangroveTealPrimary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_input_email"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MangroveTealPrimary,
                            focusedLabelColor = MangroveTealPrimary
                        )
                    )

                    if (!isDirectAccessMode) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            placeholder = { Text("admin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MangroveTealPrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onLogin(emailOrUsername, password) }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_input_password"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MangroveTealPrimary,
                                focusedLabelColor = MangroveTealPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            if (isDirectAccessMode) {
                                onDirectEmailAccess(emailOrUsername)
                            } else {
                                onLogin(emailOrUsername, password)
                            }
                        },
                        enabled = emailOrUsername.isNotBlank() && (isDirectAccessMode || password.isNotBlank()),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MangroveTealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text(
                            text = if (isDirectAccessMode) "Continue to Field Map" else "Log In to Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle between password and direct email access
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                isDirectAccessMode = !isDirectAccessMode
                                if (isDirectAccessMode && emailOrUsername == "admin") {
                                    emailOrUsername = "volunteer@community.org"
                                } else if (!isDirectAccessMode && emailOrUsername == "volunteer@community.org") {
                                    emailOrUsername = "admin"
                                }
                            }
                        ) {
                            Text(
                                text = if (isDirectAccessMode) "← Back to Password Login" else "Use Direct Volunteer / Google Email Access →",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MangroveTealPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Demo Credentials Selector
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.10f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "QUICK DEMO ACCOUNTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAccountChip(
                            title = "Admin",
                            subtitle = "admin / admin",
                            isAdmin = true,
                            onClick = {
                                isDirectAccessMode = false
                                emailOrUsername = "admin"
                                password = "admin"
                                onLogin("admin", "admin")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickAccountChip(
                            title = "Manthan SM",
                            subtitle = "manthansm@gmail.com",
                            isAdmin = false,
                            onClick = {
                                isDirectAccessMode = false
                                emailOrUsername = "manthansm@gmail.com"
                                password = "user@123"
                                onLogin("manthansm@gmail.com", "user@123")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAccountChip(
                            title = "Gaurav HP",
                            subtitle = "gauravhp@gmail.com",
                            isAdmin = false,
                            onClick = {
                                isDirectAccessMode = false
                                emailOrUsername = "gauravhp@gmail.com"
                                password = "user@123"
                                onLogin("gauravhp@gmail.com", "user@123")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickAccountChip(
                            title = "Volunteer Alex",
                            subtitle = "alex.rivera@volunteer.org",
                            isAdmin = false,
                            onClick = {
                                isDirectAccessMode = false
                                emailOrUsername = "alex.rivera@volunteer.org"
                                password = "user@123"
                                onLogin("alex.rivera@volunteer.org", "user@123")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccountChip(
    title: String,
    subtitle: String,
    isAdmin: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isAdmin) Color(0xFF1E3A34) else Color(0xFF142E28),
        border = BorderStroke(1.dp, if (isAdmin) MangroveTealPrimary else Color.White.copy(alpha = 0.2f)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isAdmin) Color(0xFFFFB74D) else Color(0xFF80CBC4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                ),
                maxLines = 1
            )
        }
    }
}
