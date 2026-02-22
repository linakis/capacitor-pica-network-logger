package com.linakis.capacitorpicanetworklogger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    repository: LogRepository? = null,
    shareText: ((title: String, text: String) -> Unit)? = null,
    onThemeChange: ((Boolean) -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    BoxWithConstraints {
        val screenWidthDp = maxWidth
        val isCompact = screenWidthDp < 600.dp
        val isExpanded = screenWidthDp >= 840.dp
        InspectorScreenContent(
            repository = repository,
            shareText = shareText,
            onThemeChange = onThemeChange,
            onClose = onClose,
            isCompact = isCompact,
            isExpanded = isExpanded
        )
    }
}

// ---------------------------------------------------------------------------
// Main content
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InspectorScreenContent(
    repository: LogRepository?,
    shareText: ((title: String, text: String) -> Unit)?,
    onThemeChange: ((Boolean) -> Unit)?,
    onClose: (() -> Unit)?,
    isCompact: Boolean,
    isExpanded: Boolean
) {
    var page by remember { mutableStateOf(InspectorPage.LIST) }
    var selected by remember { mutableStateOf<LogEntry?>(null) }
    var filter by remember { mutableStateOf("") }
    var showShareSheet by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val isSystemDark = isSystemInDarkTheme()
    var isDark by remember(isSystemDark) { mutableStateOf(isSystemDark) }

    SideEffect { onThemeChange?.invoke(isDark) }

    val contentPadding = if (isCompact) 12.dp else 16.dp
    val outerPadding = if (isExpanded) 24.dp else contentPadding

    val items = remember(repository, refreshKey) {
        repository?.getEntries() ?: emptyList()
    }
    val filtered = items.filter { entry ->
        filter.length < 3 ||
            entry.url.contains(filter, ignoreCase = true) ||
            (entry.host?.contains(filter, ignoreCase = true) == true) ||
            entry.method.contains(filter, ignoreCase = true)
    }

    AppTheme(isDark = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        WindowInsets.safeDrawing
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                            .asPaddingValues()
                    )
            ) {
                TopBar(
                    title = if (isCompact && page == InspectorPage.DETAIL) "Transaction" else "Network Inspector",
                    showBack = isCompact && page == InspectorPage.DETAIL,
                    onBack = { page = InspectorPage.LIST },
                    isDark = isDark,
                    onToggleTheme = { isDark = !isDark },
                    mode = if (isCompact && page == InspectorPage.DETAIL) TopBarMode.DETAIL else TopBarMode.LIST,
                    onDeleteAll = {
                        repository?.clear()
                        selected = null
                        refreshKey += 1
                    },
                    onClose = onClose,
                    onShare = {
                        if (selected != null) {
                            showShareSheet = true
                        }
                    }
                )

                val sel = selected
                if (showShareSheet && sel != null) {
                    ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
                        ShareSheetContent(
                            entry = sel,
                            onDismiss = { showShareSheet = false },
                            shareText = shareText
                        )
                    }
                }

                if (isCompact && page == InspectorPage.DETAIL) {
                    TransactionDetail(
                        entry = selected,
                        shareText = shareText,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = contentPadding)
                    )
                } else {
                    FilterRow(
                        value = filter,
                        onValueChange = { filter = it },
                        onClear = { filter = "" },
                        contentPadding = contentPadding
                    )
                    if (isCompact) {
                        TransactionList(
                            items = filtered,
                            onSelect = { entry ->
                                selected = entry
                                page = InspectorPage.DETAIL
                            },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = outerPadding)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = if (isExpanded) 1200.dp else 9999.dp)
                            ) {
                                TransactionList(
                                    items = filtered,
                                    onSelect = { entry -> selected = entry },
                                    modifier = Modifier
                                        .weight(if (isExpanded) 0.38f else 0.45f)
                                        .fillMaxSize(),
                                    contentPadding = contentPadding
                                )
                                TransactionDetail(
                                    entry = selected,
                                    shareText = shareText,
                                    modifier = Modifier
                                        .weight(if (isExpanded) 0.62f else 0.55f)
                                        .fillMaxSize()
                                        .padding(start = contentPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Theme
// ---------------------------------------------------------------------------

@Composable
private fun AppTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val darkScheme = darkColorScheme(
        primary = Color(0xFF88D5C2),
        onPrimary = Color(0xFF0C1413),
        primaryContainer = Color(0xFF1E3A34),
        onPrimaryContainer = Color(0xFFC5EEE3),
        secondary = Color(0xFFAEC7C1),
        onSecondary = Color(0xFF0F1413),
        secondaryContainer = Color(0xFF253431),
        onSecondaryContainer = Color(0xFFC7D9D4),
        tertiary = Color(0xFFFFC39A),
        onTertiary = Color(0xFF1C130D),
        background = Color(0xFF0F1112),
        onBackground = Color(0xFFE6E8E8),
        surface = Color(0xFF14171A),
        onSurface = Color(0xFFE6E8E8),
        surfaceVariant = Color(0xFF1F2428),
        onSurfaceVariant = Color(0xFFB5BDC3),
        outline = Color(0xFF4A5258),
        outlineVariant = Color(0xFF2C3237)
    )
    val lightScheme = lightColorScheme(
        primary = Color(0xFF2E6D5E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBCECE0),
        onPrimaryContainer = Color(0xFF082018),
        secondary = Color(0xFF4A5C58),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCFE4DE),
        onSecondaryContainer = Color(0xFF0C1F1B),
        tertiary = Color(0xFF7A4A27),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF6F7F7),
        onBackground = Color(0xFF1A1D1E),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1D1E),
        surfaceVariant = Color(0xFFE6ECEA),
        onSurfaceVariant = Color(0xFF4A5552),
        outline = Color(0xFF7A8783),
        outlineVariant = Color(0xFFD1D8D6)
    )
    val typography = androidx.compose.material3.Typography(
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
    )
    val scheme = if (isDark) darkScheme else lightScheme
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

private enum class InspectorPage { LIST, DETAIL }
private enum class TopBarMode { LIST, DETAIL }

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    mode: TopBarMode,
    onDeleteAll: () -> Unit,
    onClose: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleTheme) {
                if (isDark) {
                    Icon(Icons.Filled.LightMode, contentDescription = "Switch to light")
                } else {
                    Icon(Icons.Filled.DarkMode, contentDescription = "Switch to dark")
                }
            }
            if (mode == TopBarMode.LIST) {
                IconButton(onClick = onDeleteAll) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete all")
                }
            } else {
                IconButton(onClick = { onShare?.invoke() }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ---------------------------------------------------------------------------
// Filter
// ---------------------------------------------------------------------------

@Composable
private fun FilterRow(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    contentPadding: androidx.compose.ui.unit.Dp
) {
    Box(modifier = Modifier.padding(contentPadding)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Filter") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Transaction list
// ---------------------------------------------------------------------------

@Composable
private fun TransactionList(
    items: List<LogEntry>,
    onSelect: (LogEntry) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp
) {
    LazyColumn(modifier = modifier.padding(horizontal = contentPadding)) {
        items(items, key = { it.id }) { entry ->
            val path = entry.path ?: runCatching { java.net.URI(entry.url).path }.getOrNull().orEmpty()
            val host = entry.host ?: runCatching { java.net.URI(entry.url).host }.getOrNull().orEmpty()
            Card(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .clickable { onSelect(entry) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Row 1: pills + date/time
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusChip(status = entry.resStatus)
                        MethodChip(method = entry.method)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = formatTime(entry.startTs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Row 2: full-width path (unlimited lines)
                    Text(
                        text = path.ifBlank { entry.url },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    // Row 3: host + duration/size
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = host,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${entry.durationMs ?: 0} ms  •  ${formatSize(entry.resBody?.length ?: 0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transaction detail
// ---------------------------------------------------------------------------

@Composable
private fun TransactionDetail(
    entry: LogEntry?,
    @Suppress("UNUSED_PARAMETER") shareText: ((title: String, text: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (entry == null) {
        Text("No selection", modifier = modifier.padding(12.dp))
        return
    }
    var tab by remember { mutableIntStateOf(0) }
    var bodyQuery by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        OverviewSection(entry)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = bodyQuery,
            onValueChange = { bodyQuery = it },
            label = { Text("Search body") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (bodyQuery.isNotEmpty()) {
                    IconButton(onClick = { bodyQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Request") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Response") })
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (tab) {
            0 -> RequestTab(entry = entry, bodyQuery = bodyQuery)
            1 -> ResponseTab(entry = entry, bodyQuery = bodyQuery)
        }
    }
}

@Composable
private fun OverviewSection(entry: LogEntry) {
    Column {
        KeyValueRow("URL", entry.url)
        KeyValueRow("Status", entry.resStatus?.toString() ?: "-")
        KeyValueRow("Duration", "${entry.durationMs ?: 0} ms")
        KeyValueRow("Size", formatSize(entry.resBody?.length ?: 0))
        KeyValueRow("Started", formatTime(entry.startTs))
        KeyValueRow("Protocol", entry.protocol ?: "-")
        KeyValueRow("SSL", if (entry.ssl) "Yes" else "No")
    }
}

@Composable
private fun RequestTab(entry: LogEntry, bodyQuery: String) {
    val headers = parseHeaders(entry.reqHeadersJson)
    val body = formatBody(entry.reqBody)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HeadersSectionCard(title = "Request Headers", headers = headers)
        SectionCard(title = "Request Body", body = body, highlightQuery = bodyQuery, isBody = true)
    }
}

@Composable
private fun ResponseTab(entry: LogEntry, bodyQuery: String) {
    val headers = parseHeaders(entry.resHeadersJson)
    val body = formatBody(entry.resBody)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HeadersSectionCard(title = "Response Headers", headers = headers)
        SectionCard(title = "Response Body", body = body, highlightQuery = bodyQuery, isBody = true)
    }
}

@Composable
private fun SectionCard(title: String, body: String, highlightQuery: String = "", isBody: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val displayBody = body.ifBlank { "-" }
                val annotated = if (isBody && displayBody != "-") {
                    highlightMatches(
                        text = displayBody,
                        query = highlightQuery,
                        highlightColor = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    AnnotatedString(displayBody)
                }
                Text(annotated, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HeadersSectionCard(title: String, headers: Map<String, String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (headers.isEmpty()) {
                    Text("-", color = MaterialTheme.colorScheme.onSurface)
                } else {
                    val annotated = buildAnnotatedString {
                        headers.entries.forEachIndexed { index, (key, value) ->
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(key)
                            }
                            append(": $value")
                            if (index < headers.size - 1) append("\n")
                        }
                    }
                    Text(annotated, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Share sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheetContent(
    entry: LogEntry,
    onDismiss: () -> Unit,
    shareText: ((title: String, text: String) -> Unit)?
) {
    val path = entry.path ?: runCatching { java.net.URI(entry.url).path }.getOrNull().orEmpty()
    val title = "${entry.method} $path"
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        ShareSheetItem(
            icon = Icons.Filled.Code,
            label = "Share cURL",
            onClick = {
                shareText?.invoke(title, buildCurl(entry))
                onDismiss()
            }
        )
        ShareSheetItem(
            icon = Icons.Filled.Description,
            label = "Share txt",
            onClick = {
                shareText?.invoke(title, buildShareText(entry))
                onDismiss()
            }
        )
        ShareSheetItem(
            icon = Icons.Filled.Inventory,
            label = "Share HAR",
            onClick = {
                shareText?.invoke(title, buildHar(entry))
                onDismiss()
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ShareSheetItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------------------
// Key-value row
// ---------------------------------------------------------------------------

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            key,
            modifier = Modifier.width(86.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------------------------------------------------------------------
// Chips
// ---------------------------------------------------------------------------

@Composable
private fun StatusChip(status: Long?) {
    val label = status?.toString() ?: "-"
    val chipColor = statusColor(status)
    val contentColor = if (chipColor.luminance() < 0.5f) Color.White else Color.Black
    Chip(label = label, background = chipColor, contentColor = contentColor)
}

@Composable
private fun MethodChip(method: String) {
    val label = method.ifBlank { "-" }
    val mc = methodColor(method)
    val contentColor = if (mc.luminance() < 0.5f) Color.White else Color.Black
    Chip(label = label, background = mc, contentColor = contentColor)
}

@Composable
private fun Chip(label: String, background: Color, contentColor: Color) {
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ---------------------------------------------------------------------------
// Formatters
// ---------------------------------------------------------------------------

private fun formatTime(epochMillis: Long): String {
    val date = java.util.Date(epochMillis)
    val formatter = java.text.SimpleDateFormat("dd MMM HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(date)
}

private fun formatSize(bytes: Int): String {
    if (bytes <= 0) return "-"
    val kb = bytes / 1024.0
    return if (kb < 1024) {
        String.format(java.util.Locale.getDefault(), "%.2f KB", kb)
    } else {
        String.format(java.util.Locale.getDefault(), "%.2f MB", kb / 1024.0)
    }
}

private fun formatHeaders(headersJson: String?): String {
    if (headersJson.isNullOrBlank()) return ""
    return try {
        val obj = org.json.JSONObject(headersJson)
        val keys = obj.keys()
        val lines = mutableListOf<String>()
        while (keys.hasNext()) {
            val key = keys.next()
            lines.add("$key: ${obj.optString(key)}")
        }
        lines.joinToString("\n")
    } catch (_: Exception) {
        headersJson
    }
}

private fun formatBody(body: String?): String {
    if (body.isNullOrBlank()) return ""
    return try {
        when {
            body.trim().startsWith("{") -> org.json.JSONObject(body).toString(2)
            body.trim().startsWith("[") -> org.json.JSONArray(body).toString(2)
            else -> body
        }
    } catch (_: Exception) {
        body
    }
}

private fun highlightMatches(
    text: String,
    query: String,
    highlightColor: Color,
    textColor: Color
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.length < 2) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = trimmed.lowercase()
    var start = 0
    return buildAnnotatedString {
        while (start < text.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex = start)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            if (index > start) {
                append(text.substring(start, index))
            }
            withStyle(
                SpanStyle(
                    background = highlightColor,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(text.substring(index, index + lowerQuery.length))
            }
            start = index + lowerQuery.length
        }
    }
}

private fun parseHeaders(headersJson: String?): Map<String, String> {
    if (headersJson.isNullOrBlank()) return emptyMap()
    return try {
        val obj = org.json.JSONObject(headersJson)
        val keys = obj.keys()
        val map = mutableMapOf<String, String>()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.optString(key)
        }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun escapeSingleQuotes(value: String): String {
    return value.replace("'", "'\\''")
}

// ---------------------------------------------------------------------------
// Share builders
// ---------------------------------------------------------------------------

private fun buildCurl(entry: LogEntry): String {
    val method = entry.method.ifBlank { "GET" }
    val parts = mutableListOf("curl -X $method '${entry.url}'")
    parseHeaders(entry.reqHeadersJson).forEach { (key, value) ->
        parts.add("-H '${escapeSingleQuotes("$key: $value")}'")
    }
    entry.reqBody?.let {
        if (it.isNotBlank()) {
            parts.add("--data '${escapeSingleQuotes(it)}'")
        }
    }
    return parts.joinToString(" \\\n  ")
}

private fun buildShareText(entry: LogEntry): String {
    val divider = "----------------------------------------"
    val requestHeaders = formatHeaders(entry.reqHeadersJson)
    val responseHeaders = formatHeaders(entry.resHeadersJson)
    val requestBody = formatBody(entry.reqBody)
    val responseBody = formatBody(entry.resBody)
    return buildString {
        append("URL\n")
        append(entry.url)
        append("\n")
        append(divider)
        append("\nRequest Headers\n")
        append(requestHeaders.ifBlank { "-" })
        append("\n")
        append(divider)
        append("\nRequest Body\n")
        append(requestBody.ifBlank { "-" })
        append("\n")
        append(divider)
        append("\nResponse Headers\n")
        append(responseHeaders.ifBlank { "-" })
        append("\n")
        append(divider)
        append("\nResponse Body\n")
        append(responseBody.ifBlank { "-" })
    }
}

private fun buildHar(entry: LogEntry): String {
    val har = org.json.JSONObject()
    val logObj = org.json.JSONObject()
    logObj.put("version", "1.2")
    logObj.put(
        "creator", org.json.JSONObject()
            .put("name", "capacitor-pica-network-logger")
            .put("version", "0.1.0")
    )
    val entries = org.json.JSONArray()
    val e = org.json.JSONObject()
    e.put("startedDateTime", java.time.Instant.ofEpochMilli(entry.startTs).toString())
    e.put("time", entry.durationMs ?: 0)
    val request = org.json.JSONObject()
        .put("method", entry.method)
        .put("url", entry.url)
        .put("httpVersion", "HTTP/1.1")
        .put("headers", harHeaders(entry.reqHeadersJson))
        .put("queryString", org.json.JSONArray())
        .put("headersSize", -1)
        .put("bodySize", entry.reqBody?.length ?: 0)
        .put(
            "postData", org.json.JSONObject()
                .put("mimeType", "text/plain")
                .put("text", entry.reqBody ?: "")
        )
    e.put("request", request)
    val response = org.json.JSONObject()
        .put("status", entry.resStatus ?: 0)
        .put("statusText", "")
        .put("httpVersion", "HTTP/1.1")
        .put("headers", harHeaders(entry.resHeadersJson))
        .put(
            "content", org.json.JSONObject()
                .put("size", entry.resBody?.length ?: 0)
                .put("mimeType", "text/plain")
                .put("text", entry.resBody ?: "")
        )
        .put("redirectURL", "")
        .put("headersSize", -1)
        .put("bodySize", entry.resBody?.length ?: 0)
    e.put("response", response)
    e.put("cache", org.json.JSONObject())
    e.put(
        "timings", org.json.JSONObject()
            .put("send", 0)
            .put("wait", entry.durationMs ?: 0)
            .put("receive", 0)
    )
    entries.put(e)
    logObj.put("entries", entries)
    har.put("log", logObj)
    return har.toString(2)
}

private fun harHeaders(headersJson: String?): org.json.JSONArray {
    val array = org.json.JSONArray()
    parseHeaders(headersJson).forEach { (key, value) ->
        array.put(org.json.JSONObject().put("name", key).put("value", value))
    }
    return array
}

// ---------------------------------------------------------------------------
// Colors
// ---------------------------------------------------------------------------

private fun statusColor(status: Long?): Color {
    return when (status) {
        null -> Color(0xFF5B6070)
        in 200..299 -> Color(0xFF1E7B5D)
        in 300..399 -> Color(0xFF2A6FB0)
        in 400..499 -> Color(0xFFB06A1A)
        in 500..599 -> Color(0xFFB23939)
        else -> Color(0xFF2E3140)
    }
}

private fun methodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF4B3D9A)
        "POST" -> Color(0xFF7A2F7C)
        "PUT" -> Color(0xFF0E6F83)
        "PATCH" -> Color(0xFF6A5C1A)
        "DELETE" -> Color(0xFF5C2A2A)
        "HEAD" -> Color(0xFF3D4B6A)
        "OPTIONS" -> Color(0xFF4A4A4A)
        else -> Color(0xFF3E4A5A)
    }
}
