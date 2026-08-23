package com.example.treemap.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun LoginScreen(
    onLogin: (emailOrUser: String, pass: String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localValidationMsg by remember { mutableStateOf<String?>(null) }

    val warmCanvasColor = Color(0xFFF9F6F0)
    val cardSurfaceColor = Color(0xFFFFFFFF)
    val brandTeal = Color(0xFF144A42)
    val warmAccentOrange = Color(0xFFE89A3C)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(warmCanvasColor)
            .imePadding()
    ) {
        val isWide = maxWidth > 700.dp

        if (isWide) {
            // Tablet / Landscape Split View
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 1100.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Column: Form
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(40.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            LoginFormContent(
                                emailOrUsername = emailOrUsername,
                                onEmailChange = { emailOrUsername = it },
                                password = password,
                                onPasswordChange = { password = it },
                                rememberMe = rememberMe,
                                onRememberMeChange = { rememberMe = it },
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                onForgotPasswordClick = { showForgotPasswordDialog = true },
                                onSubmit = {
                                    if (emailOrUsername.isBlank() || password.isBlank()) {
                                        localValidationMsg = "Please enter both username/email and password."
                                    } else {
                                        localValidationMsg = null
                                        onLogin(emailOrUsername.trim(), password)
                                    }
                                },
                                errorMessage = localValidationMsg ?: errorMessage,
                                brandTeal = brandTeal
                            )
                        }

                        // Right Column: Nature Botanical Illustration
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                                .background(Color(0xFF0D2823))
                        ) {
                            MangroveBotanicalArt(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            // Mobile Portrait View: Clean Stack
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Illustration Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .background(Color(0xFF0D2823))
                        ) {
                            MangroveBotanicalArt(
                                modifier = Modifier.fillMaxSize()
                            )

                            // Top Brand Logo Overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.treemap.ui.components.MapTreeLogoBadge(
                                    size = 38.dp,
                                    showText = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "MapTree",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Project Tomorrow",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Form Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            LoginFormContent(
                                emailOrUsername = emailOrUsername,
                                onEmailChange = { emailOrUsername = it },
                                password = password,
                                onPasswordChange = { password = it },
                                rememberMe = rememberMe,
                                onRememberMeChange = { rememberMe = it },
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                onForgotPasswordClick = { showForgotPasswordDialog = true },
                                onSubmit = {
                                    if (emailOrUsername.isBlank() || password.isBlank()) {
                                        localValidationMsg = "Please enter both username/email and password."
                                    } else {
                                        localValidationMsg = null
                                        onLogin(emailOrUsername.trim(), password)
                                    }
                                },
                                errorMessage = localValidationMsg ?: errorMessage,
                                brandTeal = brandTeal
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = brandTeal,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Account Access & Recovery",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To access the system:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "• Administrator Login: Use username 'admin' with password 'admin'.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Team & Volunteer Accounts: All accounts are managed exclusively by the Administrator. Contact your system administrator to provision or update your login credentials.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showForgotPasswordDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = brandTeal)
                ) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun LoginFormContent(
    emailOrUsername: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmit: () -> Unit,
    errorMessage: String?,
    brandTeal: Color
) {
    // Header Branding
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = brandTeal.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Forest,
                    contentDescription = null,
                    tint = brandTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Mangrove",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = brandTeal,
                letterSpacing = 0.3.sp
            )
        )
    }

    // Title & Subtitle matching image.png
    Text(
        text = "Welcome Back!",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color(0xFF132A26)
        )
    )

    Text(
        text = "Please Log in to your account.",
        style = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF6B7E7A)
        ),
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
    )

    // Error Alert if any
    AnimatedVisibility(
        visible = !errorMessage.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFFEBEE),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    // Input: Username or Email (Empty box with no confusing placeholder text)
    OutlinedTextField(
        value = emailOrUsername,
        onValueChange = onEmailChange,
        label = { Text("Email Address or Username") },
        placeholder = null,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.AlternateEmail,
                contentDescription = null
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = Color(0xFF132A26),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_email"),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF132A26),
            unfocusedTextColor = Color(0xFF132A26),
            focusedContainerColor = Color(0xFFFAFCFA),
            unfocusedContainerColor = Color(0xFFFFFFFF),
            focusedBorderColor = brandTeal,
            unfocusedBorderColor = Color(0xFFCAD7D3),
            focusedLabelColor = brandTeal,
            unfocusedLabelColor = Color(0xFF5A6F6A),
            cursorColor = brandTeal,
            focusedLeadingIconColor = brandTeal,
            unfocusedLeadingIconColor = Color(0xFF7A8D89)
        )
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Input: Password (Empty box with no confusing placeholder text, visible dark characters)
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        placeholder = null,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisible) {
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
            onDone = { onSubmit() }
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = Color(0xFF132A26),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_input_password"),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF132A26),
            unfocusedTextColor = Color(0xFF132A26),
            focusedContainerColor = Color(0xFFFAFCFA),
            unfocusedContainerColor = Color(0xFFFFFFFF),
            focusedBorderColor = brandTeal,
            unfocusedBorderColor = Color(0xFFCAD7D3),
            focusedLabelColor = brandTeal,
            unfocusedLabelColor = Color(0xFF5A6F6A),
            cursorColor = brandTeal,
            focusedLeadingIconColor = brandTeal,
            unfocusedLeadingIconColor = Color(0xFF7A8D89),
            focusedTrailingIconColor = brandTeal,
            unfocusedTrailingIconColor = Color(0xFF7A8D89)
        )
    )

    // Remember Me & Forgot Password Row (Matching image.png)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onRememberMeChange(!rememberMe) }
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = onRememberMeChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = brandTeal,
                    uncheckedColor = Color(0xFF9EABA7)
                )
            )
            Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF4A5C58),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        TextButton(
            onClick = onForgotPasswordClick,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFE55757),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Primary Action: Full-width Clean Login Button (Admin creates accounts, removed create account button)
    Button(
        onClick = onSubmit,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = brandTeal
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("login_submit_button")
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Subtle Footer Terms (Matching image.png)
    Text(
        text = "By signing in you agree to our terms and coastal mangrove data protection policy.",
        style = MaterialTheme.typography.labelSmall.copy(
            color = Color(0xFF94A5A1),
            fontSize = 11.sp,
            lineHeight = 14.sp
        ),
        textAlign = TextAlign.Start
    )
}

/**
 * Botanical Vector Artwork featuring lush mangrove leaves, branch, and colorful kingfisher bird,
 * matching the visual aesthetic of the user's reference mockup.
 */
@Composable
private fun MangroveBotanicalArt(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Rich deep jungle backdrop gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F322B),
                    Color(0xFF09221D),
                    Color(0xFF041310)
                )
            )
        )

        // 2. Warm ambient sunlight halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x35E8A93C),
                    Color(0x1226B888),
                    Color.Transparent
                ),
                center = Offset(w * 0.7f, h * 0.35f),
                radius = w * 0.65f
            )
        )

        // 3. Layered Botanical Leaves in Background
        drawBotanicalFoliage(w, h)

        // 4. Perched Tree Branch (Textured wood)
        val branchPath = Path().apply {
            moveTo(0f, h * 0.68f)
            cubicTo(
                w * 0.35f, h * 0.72f,
                w * 0.55f, h * 0.62f,
                w * 0.85f, h * 0.58f
            )
            cubicTo(
                w * 0.95f, h * 0.57f,
                w * 1.05f, h * 0.55f,
                w * 1.15f, h * 0.54f
            )
        }

        // Branch Shadow
        drawPath(
            path = branchPath,
            color = Color(0xFF2C1E14),
            style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round)
        )
        // Branch Main Wood
        drawPath(
            path = branchPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6B4B32),
                    Color(0xFF8D6847),
                    Color(0xFF4A3322)
                ),
                start = Offset(0f, h * 0.7f),
                end = Offset(w, h * 0.55f)
            ),
            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
        )

        // 5. Perched Kingfisher / Mangrove Bird Illustration
        drawTropicalBird(w, h)

        // 6. Foreground Hanging Leaves Overlay
        drawForegroundVines(w, h)

        // 7. Interactive Slider / Navigation Pill Indicator Accent (like image.png)
        drawCircle(
            color = Color(0xFFF1A934),
            radius = 16.dp.toPx(),
            center = Offset(w * 0.05f, h * 0.52f)
        )
        // Play / Arrow indicator inside the circle
        val playPath = Path().apply {
            val cx = w * 0.05f
            val cy = h * 0.52f
            moveTo(cx - 3.dp.toPx(), cy - 5.dp.toPx())
            lineTo(cx + 5.dp.toPx(), cy)
            lineTo(cx - 3.dp.toPx(), cy + 5.dp.toPx())
            close()
        }
        drawPath(path = playPath, color = Color.White)
    }
}

private fun DrawScope.drawBotanicalFoliage(w: Float, h: Float) {
    val leafColors = listOf(
        Color(0xFF175344),
        Color(0xFF1E6B57),
        Color(0xFF2B856E),
        Color(0xFF0F3B30),
        Color(0xFF3BA68B)
    )

    // Background leaf clusters
    val leafPositions = listOf(
        Triple(w * 0.15f, h * 0.25f, 45f),
        Triple(w * 0.35f, h * 0.15f, -30f),
        Triple(w * 0.75f, h * 0.20f, 60f),
        Triple(w * 0.88f, h * 0.40f, -15f),
        Triple(w * 0.20f, h * 0.80f, 25f),
        Triple(w * 0.70f, h * 0.85f, -40f),
        Triple(w * 0.90f, h * 0.75f, 50f)
    )

    leafPositions.forEachIndexed { i, (lx, ly, rot) ->
        val leafPath = Path().apply {
            moveTo(lx, ly)
            cubicTo(
                lx + 40.dp.toPx(), ly - 30.dp.toPx(),
                lx + 70.dp.toPx(), ly + 10.dp.toPx(),
                lx + 80.dp.toPx(), ly + 50.dp.toPx()
            )
            cubicTo(
                lx + 40.dp.toPx(), ly + 70.dp.toPx(),
                lx + 10.dp.toPx(), ly + 40.dp.toPx(),
                lx, ly
            )
            close()
        }
        drawPath(
            path = leafPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    leafColors[i % leafColors.size],
                    leafColors[(i + 1) % leafColors.size]
                )
            )
        )
    }
}

private fun DrawScope.drawTropicalBird(w: Float, h: Float) {
    val bx = w * 0.65f
    val by = h * 0.42f

    // Bird Tail Feathers
    val tailPath = Path().apply {
        moveTo(bx - 10.dp.toPx(), by + 40.dp.toPx())
        lineTo(bx - 25.dp.toPx(), by + 110.dp.toPx())
        lineTo(bx - 5.dp.toPx(), by + 120.dp.toPx())
        lineTo(bx + 15.dp.toPx(), by + 45.dp.toPx())
        close()
    }
    drawPath(
        path = tailPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF287C68),
                Color(0xFF165244),
                Color(0xFF0F382E)
            )
        )
    )

    // Bird Body (Fluffy Emerald & Teal plumage)
    val bodyPath = Path().apply {
        moveTo(bx, by - 20.dp.toPx())
        cubicTo(
            bx + 45.dp.toPx(), by - 10.dp.toPx(),
            bx + 40.dp.toPx(), by + 50.dp.toPx(),
            bx + 10.dp.toPx(), by + 65.dp.toPx()
        )
        cubicTo(
            bx - 20.dp.toPx(), by + 65.dp.toPx(),
            bx - 35.dp.toPx(), by + 20.dp.toPx(),
            bx, by - 20.dp.toPx()
        )
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4AC49F),
                Color(0xFF269376),
                Color(0xFF16604D)
            ),
            center = Offset(bx + 5.dp.toPx(), by + 15.dp.toPx()),
            radius = 60.dp.toPx()
        )
    )

    // Breast Feathers / Warm Peach Accents (like tropical kingfisher)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF2A45C),
                Color(0xFFD97724),
                Color.Transparent
            )
        ),
        radius = 18.dp.toPx(),
        center = Offset(bx + 16.dp.toPx(), by + 22.dp.toPx())
    )

    // Bird Head & Crest
    val headPath = Path().apply {
        moveTo(bx - 5.dp.toPx(), by - 20.dp.toPx())
        cubicTo(
            bx - 15.dp.toPx(), by - 45.dp.toPx(),
            bx + 15.dp.toPx(), by - 55.dp.toPx(),
            bx + 25.dp.toPx(), by - 30.dp.toPx()
        )
        cubicTo(
            bx + 28.dp.toPx(), by - 20.dp.toPx(),
            bx + 15.dp.toPx(), by - 15.dp.toPx(),
            bx - 5.dp.toPx(), by - 20.dp.toPx()
        )
        close()
    }
    drawPath(
        path = headPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF5FE3B8),
                Color(0xFF33AA86),
                Color(0xFF1E6B56)
            )
        )
    )

    // Crest Spikes / Feathers
    val crestPath = Path().apply {
        moveTo(bx - 8.dp.toPx(), by - 40.dp.toPx())
        lineTo(bx - 18.dp.toPx(), by - 56.dp.toPx())
        lineTo(bx - 2.dp.toPx(), by - 48.dp.toPx())
        lineTo(bx + 6.dp.toPx(), by - 60.dp.toPx())
        lineTo(bx + 14.dp.toPx(), by - 46.dp.toPx())
    }
    drawPath(
        path = crestPath,
        color = Color(0xFF67E9BD),
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    )

    // Bird Beak
    val beakPath = Path().apply {
        moveTo(bx + 22.dp.toPx(), by - 28.dp.toPx())
        lineTo(bx + 52.dp.toPx(), by - 22.dp.toPx())
        lineTo(bx + 20.dp.toPx(), by - 18.dp.toPx())
        close()
    }
    drawPath(
        path = beakPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A3B32),
                Color(0xFF211915)
            )
        )
    )

    // Eye
    drawCircle(
        color = Color(0xFF151515),
        radius = 4.dp.toPx(),
        center = Offset(bx + 14.dp.toPx(), by - 26.dp.toPx())
    )
    drawCircle(
        color = Color.White,
        radius = 1.5.dp.toPx(),
        center = Offset(bx + 15.dp.toPx(), by - 27.dp.toPx())
    )

    // Bird Feet / Claws gripping the branch
    drawCircle(
        color = Color(0xFF3B2A1E),
        radius = 3.dp.toPx(),
        center = Offset(bx, by + 65.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF3B2A1E),
        radius = 3.dp.toPx(),
        center = Offset(bx + 12.dp.toPx(), by + 63.dp.toPx())
    )
}

private fun DrawScope.drawForegroundVines(w: Float, h: Float) {
    // Elegant translucent leaf silhouettes giving depth
    val vineColors = listOf(
        Color(0x334FE0B0),
        Color(0x2232B88C)
    )

    val p1 = Path().apply {
        moveTo(w * 0.45f, h * 0.35f)
        cubicTo(
            w * 0.40f, h * 0.50f,
            w * 0.48f, h * 0.65f,
            w * 0.46f, h * 0.75f
        )
    }
    drawPath(
        path = p1,
        color = vineColors[0],
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )
}
