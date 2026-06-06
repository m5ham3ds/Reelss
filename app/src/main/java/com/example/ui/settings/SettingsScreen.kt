package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val pexelsKey by settingsManager.pexelsApiKey.collectAsState(initial = "")
    val pixabayKey by settingsManager.pixabayApiKey.collectAsState(initial = "")
    val isDark by settingsManager.themeMode.collectAsState(initial = true)
    val showTrans by settingsManager.showTranslation.collectAsState(initial = true)
    val language by settingsManager.language.collectAsState(initial = "ar")

    val isArabic = language == "ar"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "الإعدادات" else "Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = if (isArabic) "المظهر واللغة" else "Appearance & Language",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "الوضع الداكن" else "Dark Mode", modifier = Modifier.weight(1f))
                Switch(checked = isDark, onCheckedChange = { scope.launch { settingsManager.setThemeMode(it) } })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "ترجمة الآيات" else "Show Translation", modifier = Modifier.weight(1f))
                Switch(checked = showTrans, onCheckedChange = { scope.launch { settingsManager.setShowTranslation(it) } })
            }

            var langExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = langExpanded,
                onExpandedChange = { langExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (isArabic) "العربية" else "English",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isArabic) "اللغة" else "Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = langExpanded,
                    onDismissRequest = { langExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("العربية") },
                        onClick = {
                            scope.launch { settingsManager.setLanguage("ar") }
                            langExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("English") },
                        onClick = {
                            scope.launch { settingsManager.setLanguage("en") }
                            langExpanded = false
                        }
                    )
                }
            }
            
            HorizontalDivider()

            Text(
                text = if (isArabic) "مفاتيح API (للفيديوهات السينمائية)" else "API Keys (Cinematic Videos)",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = pexelsKey,
                onValueChange = { scope.launch { settingsManager.savePexelsKey(it) } },
                label = { Text("Pexels API Key") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pixabayKey,
                onValueChange = { scope.launch { settingsManager.savePixabayKey(it) } },
                label = { Text("Pixabay API Key") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
