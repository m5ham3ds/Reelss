package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.settings.SettingsManager
import com.example.ui.ReelState
import com.example.ui.ReelViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

val SURAH_NAMES = listOf(
    "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم", "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس", "التكوير", "الإنفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات", "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر", "المسد", "الإخلاص", "الفلق", "الناس"
)

val SURAH_COUNTS = mapOf(1 to 7, 2 to 286, 3 to 200, 4 to 176, 5 to 120, 6 to 165, 7 to 206, 8 to 75, 9 to 129, 10 to 109, 11 to 123, 12 to 111, 13 to 43, 14 to 52, 15 to 99, 16 to 128, 17 to 111, 18 to 110, 19 to 98, 20 to 135, 21 to 112, 22 to 78, 23 to 118, 24 to 64, 25 to 77, 26 to 227, 27 to 93, 28 to 88, 29 to 69, 30 to 60, 31 to 34, 32 to 30, 33 to 73, 34 to 54, 35 to 45, 36 to 83, 37 to 182, 38 to 88, 39 to 75, 40 to 85, 41 to 54, 42 to 53, 43 to 89, 44 to 59, 45 to 37, 46 to 35, 47 to 38, 48 to 29, 49 to 18, 50 to 45, 51 to 60, 52 to 49, 53 to 62, 54 to 55, 55 to 78, 56 to 96, 57 to 29, 58 to 22, 59 to 24, 60 to 13, 61 to 14, 62 to 11, 63 to 11, 64 to 18, 65 to 12, 66 to 12, 67 to 30, 68 to 52, 69 to 52, 70 to 44, 71 to 28, 72 to 28, 73 to 20, 74 to 56, 75 to 40, 76 to 31, 77 to 50, 78 to 40, 79 to 46, 80 to 42, 81 to 29, 82 to 19, 83 to 36, 84 to 25, 85 to 22, 86 to 17, 87 to 19, 88 to 26, 89 to 30, 90 to 20, 91 to 15, 92 to 21, 93 to 11, 94 to 8, 95 to 8, 96 to 19, 97 to 5, 98 to 8, 99 to 8, 100 to 11, 101 to 11, 102 to 8, 103 to 3, 104 to 9, 105 to 5, 106 to 4, 107 to 7, 108 to 3, 109 to 6, 110 to 3, 111 to 5, 112 to 4, 113 to 5, 114 to 6)

val RECITERS = listOf(
    "AbdulSamad_64kbps_QuranExplorer.Com" to "الشيخ عبدالباسط عبدالصمد",
    "Abdul_Basit_Murattal_64kbps" to "الشيخ عبدالباسط عبدالصمد مرتل",
    "Abdurrahmaan_As-Sudais_64kbps" to "الشيخ عبدالرحمن السديس",
    "Maher_AlMuaiqly_64kbps" to "الشيخ ماهر المعيقلي",
    "Alafasy_64kbps" to "الشيخ مشاري العفاسي",
    "Husary_64kbps" to "الشيخ محمود خليل الحصري",
    "Hudhaify_64kbps" to "الشيخ عبدالله الحذيفي"
)

class MainActivity : ComponentActivity() {
    private val viewModel: ReelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val isDark by settingsManager.themeMode.collectAsState(initial = true)
            val language by settingsManager.language.collectAsState(initial = "ar")
            val isArabic = language == "ar"

            MyApplicationTheme(darkTheme = isDark) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                isArabic = isArabic,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ReelViewModel, isArabic: Boolean, onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var selectedSurahIdx by remember { mutableIntStateOf(0) }
    var startAyahText by remember { mutableStateOf("1") }
    var endAyahText by remember { mutableStateOf("5") }
    var selectedReciterIdx by remember { mutableIntStateOf(4) } // Default Alafasy

    var surahExpanded by remember { mutableStateOf(false) }
    var reciterExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "صانع ريلز القرآن" else "Quran Reels Maker", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Surah Dropdown
            ExposedDropdownMenuBox(
                expanded = surahExpanded,
                onExpandedChange = { surahExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${selectedSurahIdx + 1} - ${SURAH_NAMES[selectedSurahIdx]}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isArabic) "اختر السورة" else "Select Surah") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = surahExpanded) },
                    modifier = Modifier.fillMaxWidth().testTag("surah_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = surahExpanded,
                    onDismissRequest = { surahExpanded = false }
                ) {
                    SURAH_NAMES.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text("${index + 1} - $name") },
                            onClick = {
                                selectedSurahIdx = index
                                surahExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startAyahText,
                    onValueChange = { startAyahText = it },
                    label = { Text(if (isArabic) "من آية" else "From Ayah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("start_ayah")
                )
                OutlinedTextField(
                    value = endAyahText,
                    onValueChange = { endAyahText = it },
                    label = { Text(if (isArabic) "إلى آية" else "To Ayah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("end_ayah")
                )
            }

            // Reciter Dropdown
            ExposedDropdownMenuBox(
                expanded = reciterExpanded,
                onExpandedChange = { reciterExpanded = it }
            ) {
                OutlinedTextField(
                    value = RECITERS[selectedReciterIdx].second,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isArabic) "القارئ" else "Reciter") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterExpanded) },
                    modifier = Modifier.fillMaxWidth().testTag("reciter_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = reciterExpanded,
                    onDismissRequest = { reciterExpanded = false }
                ) {
                    RECITERS.forEachIndexed { index, reciter ->
                        DropdownMenuItem(
                            text = { Text(reciter.second) },
                            onClick = {
                                selectedReciterIdx = index
                                reciterExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state is ReelState.Idle || state is ReelState.Error || state is ReelState.Success) {
                Button(
                    onClick = {
                        val start = startAyahText.toIntOrNull() ?: 1
                        val end = endAyahText.toIntOrNull() ?: start
                        val maxAyahs = SURAH_COUNTS[selectedSurahIdx + 1] ?: 1
                        
                        val cStart = start.coerceIn(1, maxAyahs)
                        val cEnd = end.coerceIn(cStart, maxAyahs)
                        
                        viewModel.generate(
                            context = context,
                            surah = selectedSurahIdx + 1,
                            startAyah = cStart,
                            endAyah = cEnd,
                            reciterId = RECITERS[selectedReciterIdx].first
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("generate_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isArabic) "إنشاء المقطع (Reel)" else "Generate Reel", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            when (state) {
                is ReelState.Error -> {
                    Text(
                        text = (state as ReelState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ReelState.Loading -> {
                    val loadingState = state as ReelState.Loading
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { loadingState.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(loadingState.message, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                is ReelState.Success -> {
                    val uri = (state as ReelState.Success).uri
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(if (isArabic) "تم بنجاح!" else "Success!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // ExoPlayer video preview
                            var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
                            DisposableEffect(uri) {
                                val player = ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(uri))
                                    prepare()
                                }
                                exoPlayer = player
                                onDispose { player.release() }
                            }
                            
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = true
                                    }
                                },
                                modifier = Modifier
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, if (isArabic) "مشاركة المقطع" else "Share Reel"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isArabic) "مشاركة" else "Share")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}


