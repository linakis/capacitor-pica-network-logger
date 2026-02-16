package com.linakis.capacitorpicanetworklogger.kmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    repository: InspectorRepository? = null,
    shareText: ((title: String, text: String) -> Unit)? = null,
    saveText: ((fileName: String, text: String) -> Unit)? = null,
    colorSchemeProvider: ((Boolean) -> ColorScheme?)? = null,
    onThemeChange: ((Boolean) -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isCompact = screenWidth < 600
    val isExpanded = screenWidth >= 840
    var page by remember { mutableStateOf(InspectorPage.LIST) }
    var selected by remember { mutableStateOf<LogDetail?>(null) }
    var filter by remember { mutableStateOf("") }
    var showShareSheet by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val isSystemDark = isSystemInDarkTheme()
    var isDark by remember(isSystemDark) { mutableStateOf(isSystemDark) }

    SideEffect {
        onThemeChange?.invoke(isDark)
    }


    val contentPadding = if (isCompact) 12.dp else 16.dp
    val outerPadding = if (isExpanded) 24.dp else contentPadding

    val items = remember(repository, refreshKey) {
        repository?.getLogs()?.map { it.toLogItem() } ?: emptyList()
    }
    val filtered = items.filter { item ->
        filter.length < 3 ||
            item.url.contains(filter, ignoreCase = true) ||
            item.host.contains(filter, ignoreCase = true) ||
            item.method.contains(filter, ignoreCase = true)
    }

    AppTheme(isDark = isDark, colorSchemeProvider = colorSchemeProvider) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues())
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
                val selectedDetail = selected
                if (showShareSheet && selectedDetail != null) {
                    ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
                        ShareSheetContent(
                            detail = selectedDetail,
                            onDismiss = { showShareSheet = false },
                            shareText = shareText
                        )
                    }
                }
                if (isCompact && page == InspectorPage.DETAIL) {
                    TransactionDetail(
                        selected = selected,
                        shareText = shareText,
                        modifier = Modifier.fillMaxSize().padding(horizontal = contentPadding)
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
                            onSelect = { id ->
                                selected = repository?.getLog(id)?.toLogDetail()
                                page = InspectorPage.DETAIL
                            },
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = outerPadding)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = if (isExpanded) 1200.dp else 9999.dp)
                            ) {
                                TransactionList(
                                    items = filtered,
                                    onSelect = { id ->
                                        selected = repository?.getLog(id)?.toLogDetail()
                                    },
                                    modifier = Modifier.weight(if (isExpanded) 0.38f else 0.45f).fillMaxSize(),
                                    contentPadding = contentPadding
                                )
                                TransactionDetail(
                                    selected = selected,
                                    shareText = shareText,
                                    modifier = Modifier.weight(if (isExpanded) 0.62f else 0.55f).fillMaxSize().padding(start = contentPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTheme(
    isDark: Boolean,
    colorSchemeProvider: ((Boolean) -> ColorScheme?)?,
    content: @Composable () -> Unit
) {
    val darkScheme = androidx.compose.material3.darkColorScheme(
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
    val lightScheme = androidx.compose.material3.lightColorScheme(
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
    val scheme = colorSchemeProvider?.invoke(isDark) ?: if (isDark) darkScheme else lightScheme
    androidx.compose.material3.MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

private enum class InspectorPage { LIST, DETAIL }

private enum class TopBarMode { LIST, DETAIL }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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

@Composable
private fun TransactionList(
    items: List<LogItem>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp
) {
    LazyColumn(modifier = modifier.padding(horizontal = contentPadding)) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .clickable { onSelect(item.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(status = item.status)
                        MethodChip(method = item.method)
                        Text(item.path, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(item.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(formatTime(item.startTs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.durationMs ?: 0} ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatSize(item.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDetail(
    selected: LogDetail?,
    shareText: ((title: String, text: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (selected == null) {
        Text("No selection", modifier = modifier.padding(12.dp))
        return
    }
    var tab by remember { mutableStateOf(0) }
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        OverviewSection(selected)
        Spacer(modifier = Modifier.height(12.dp))
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
            0 -> RequestTab(detail = selected)
            1 -> ResponseTab(detail = selected)
        }
    }
}

@Composable
private fun OverviewSection(detail: LogDetail) {
    Column {
        KeyValueRow("URL", detail.url)
        KeyValueRow("Status", detail.status?.toString() ?: "-")
        KeyValueRow("Duration", "${detail.durationMs ?: 0} ms")
        KeyValueRow("Size", formatSize(detail.resBody?.length ?: 0))
        KeyValueRow("Started", formatTime(detail.startTs))
        KeyValueRow("Protocol", detail.protocol ?: "-")
        KeyValueRow("SSL", if (detail.ssl) "Yes" else "No")
    }
}

@Composable
private fun RequestTab(
    detail: LogDetail
) {
    val headers = formatHeaders(detail.reqHeadersJson)
    val body = formatBody(detail.reqBody)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Request Headers", body = headers)
        SectionCard(title = "Request Body", body = body)
    }
}

@Composable
private fun ResponseTab(
    detail: LogDetail
) {
    val headers = formatHeaders(detail.resHeadersJson)
    val body = formatBody(detail.resBody)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Response Headers", body = headers)
        SectionCard(title = "Response Body", body = body)
    }
}

@Composable
private fun SectionCard(title: String, body: String) {
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
                Text(body.ifBlank { "-" }, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}



@Composable
private fun ShareSheetContent(
    detail: LogDetail,
    onDismiss: () -> Unit,
    shareText: ((title: String, text: String) -> Unit)?
) {
    val title = "${detail.method} ${detail.path}"
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        ShareSheetItem(
            icon = Icons.Filled.Code,
            label = "Share cURL",
            onClick = {
                shareText?.invoke(title, detail.toCurl())
                onDismiss()
            }
        )
        ShareSheetItem(
            icon = Icons.Filled.Description,
            label = "Share txt",
            onClick = {
                shareText?.invoke(title, detail.toShareText())
                onDismiss()
            }
        )
        ShareSheetItem(
            icon = Icons.Filled.Inventory,
            label = "Share HAR",
            onClick = {
                shareText?.invoke(title, detail.toHar())
                onDismiss()
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ShareSheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
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
    val methodColor = methodColor(method)
    val contentColor = if (methodColor.luminance() < 0.5f) Color.White else Color.Black
    Chip(
        label = label,
        background = methodColor,
        contentColor = contentColor
    )
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

private fun formatTime(epochMillis: Long): String {
    val seconds = epochMillis / 1000
    val millis = epochMillis % 1000
    val instant = kotlinx.datetime.Instant.fromEpochSeconds(seconds, millis.toInt() * 1_000_000)
    return instant.toString().substringAfter('T').substringBefore('Z')
}

private fun formatSize(bytes: Int): String {
    if (bytes <= 0) return "-"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.2f KB".format(kb) else "%.2f MB".format(kb / 1024.0)
}

private fun formatHeaders(headersJson: String?): String {
    if (headersJson.isNullOrBlank()) return ""
    return try {
        val obj = org.json.JSONObject(headersJson)
        val keys = obj.keys()
        val lines = mutableListOf<String>()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key).toString()
            lines.add("$key: $value")
        }
        lines.joinToString("\n")
    } catch (_: Exception) {
        headersJson
    }
}

private fun formatBody(body: String?): String {
    if (body.isNullOrBlank()) return ""
    return try {
        val obj = org.json.JSONObject(body)
        obj.toString(2)
    } catch (_: Exception) {
        body
    }
}

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
