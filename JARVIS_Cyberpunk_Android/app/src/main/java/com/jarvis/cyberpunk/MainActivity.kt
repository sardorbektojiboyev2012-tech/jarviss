package com.jarvis.cyberpunk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BG = Color(0xFF080611)
private val PANEL = Color(0xFF171128)
private val PURPLE = Color(0xFFB45CFF)
private val CYAN = Color(0xFF5DEBFF)
private val WHITE = Color(0xFFF5F0FF)
private val MUTED = Color(0xFFAAA1C4)
private data class ChatLine(val user: Boolean, val text: String)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale("uz", "UZ") }
        setContent { JarvisScreen { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "jarvis") } }
    }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable
private fun JarvisScreen(speak: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jarvis_local", Context.MODE_PRIVATE) }
    var input by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(loadHistory(prefs)) }
    var notes by remember { mutableStateOf(prefs.getString("notes", "") ?: "") }
    var noteDraft by remember { mutableStateOf("") }
    var showNotes by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var voiceReply by remember { mutableStateOf(prefs.getBoolean("voice", true)) }
    var lang by remember { mutableStateOf(prefs.getString("lang", "uz") ?: "uz") }
    var status by remember { mutableStateOf("LOCAL CORE • READY") }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(0.72f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow")
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        input = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: input
    }

    fun send(raw: String) {
        val command = raw.trim()
        if (command.isBlank()) return
        val reply = answer(context, command, notes, lang)
        history = (history + listOf(ChatLine(true, command), ChatLine(false, reply.removePrefix("WEB_SEARCH:")))).takeLast(80)
        saveHistory(prefs, history)
        if (reply.startsWith("WEB_SEARCH:")) {
            status = "ONLINE ACTION • BROWSER"
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(command)}"))) }
        } else status = "LOCAL CORE • READY"
        if (voiceReply) speak(reply.removePrefix("WEB_SEARCH:"))
        input = ""
    }

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = PANEL, primary = PURPLE, secondary = CYAN, onBackground = WHITE, onSurface = WHITE)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21103B), BG, Color(0xFF080D1A))))) {
            Column(Modifier.fillMaxSize().padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("J.A.R.V.I.S", color = WHITE, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("CYBERPUNK INTELLIGENCE", color = PURPLE, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Sozlamalar", tint = CYAN) }
                    IconButton(onClick = { history = emptyList(); saveHistory(prefs, history); status = "NEW SESSION" }) { Icon(Icons.Default.Delete, "Chatni tozalash", tint = MUTED) }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(CYAN))
                    Spacer(Modifier.width(7.dp)); Text(status, color = CYAN, fontSize = 10.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.weight(1f)); Text("${lang.uppercase()} • HYBRID", color = MUTED, fontSize = 10.sp)
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(Brush.linearGradient(listOf(Color(0xFF2A1244), Color(0xFF101A2A)))).border(1.dp, PURPLE.copy(alpha=.7f), RoundedCornerShape(25.dp)).padding(vertical=16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(110.dp).alpha(glow).border(2.dp, PURPLE, CircleShape).padding(8.dp).border(1.dp, CYAN, CircleShape).padding(8.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF8C29D1), Color(0xFF211944), BG))), contentAlignment = Alignment.Center) {
                        Text("J", color = WHITE, fontSize = 47.sp, fontWeight = FontWeight.ExtraLight)
                    }
                    Spacer(Modifier.height(10.dp)); Text("SYSTEM ONLINE", color = CYAN, fontSize = 12.sp, letterSpacing = 2.sp)
                    Text("Shaxsiy yordamchingiz", color = MUTED, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    Quick("◷", "Vaqt", Modifier.weight(1f)) { send("soat nechchi") }
                    Quick("✎", "Eslatma", Modifier.weight(1f)) { showNotes = true }
                    Quick("⌕", "Qidiruv", Modifier.weight(1f)) { input = "internetdan qidir: " }
                    Quick("?", "Yordam", Modifier.weight(1f)) { send("yordam") }
                }
                Spacer(Modifier.height(12.dp)); Text("CONVERSATION LOG", color = PURPLE, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                if (history.isEmpty()) {
                    Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x77130F22)).border(1.dp, Color(0x554C3673), RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Xush kelibsiz.", color = WHITE, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp)); Text("Buyruq yozing yoki mikrofonni bosing. Mahalliy buyruqlar internetsiz ham ishlaydi.", color = MUTED, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x77130F22)).border(1.dp, Color(0x554C3673), RoundedCornerShape(20.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(history) { Bubble(it) } }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(PANEL).border(1.dp, PURPLE.copy(alpha=.8f), RoundedCornerShape(22.dp)).padding(horizontal=8.dp, vertical=5.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when(lang) { "en" -> "en-US"; "ru" -> "ru-RU"; else -> "uz-UZ" })
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS uchun gapiring")
                        }
                        runCatching { speech.launch(intent) }.onFailure { status = "SPEECH SERVICE UNAVAILABLE" }
                    }) { Icon(Icons.Default.Mic, "Ovozli kiritish", tint = CYAN) }
                    BasicTextField(input, { input = it }, Modifier.weight(1f).padding(horizontal=7.dp), textStyle = androidx.compose.ui.text.TextStyle(color = WHITE, fontSize = 14.sp), cursorBrush = Brush.horizontalGradient(listOf(PURPLE, CYAN)), decorationBox = { inner -> if (input.isEmpty()) Text("JARVISga buyruq yozing...", color = MUTED, fontSize = 13.sp); inner() })
                    IconButton(onClick = { send(input) }) { Icon(Icons.Default.Send, "Yuborish", tint = PURPLE) }
                }
                Spacer(Modifier.height(5.dp)); Text("Chat va eslatmalar qurilmada saqlanadi", color = MUTED, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        if (showSettings) AlertDialog(onDismissRequest = { showSettings = false }, containerColor = PANEL, title = { Text("JARVIS sozlamalari", color = WHITE) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Javobni ovozda o‘qish", Modifier.weight(1f), color = WHITE); Switch(voiceReply, { voiceReply = it; prefs.edit().putBoolean("voice", it).apply() }) }
                Text("Interfeys tili", color = WHITE)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("uz" to "O‘zbek", "en" to "English", "ru" to "Русский").forEach { (code, label) -> FilterChip(selected = lang == code, onClick = { lang = code; prefs.edit().putString("lang", code).apply() }, label = { Text(label) }) }
                }
                Text("Umumiy AI suhbat uchun keyinchalik haqiqiy AI API/serveri kerak. Bu versiya mahalliy buyruqlar va brauzer qidiruvini ishlatadi.", color = MUTED, fontSize = 12.sp)
            }
        }, confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Yopish", color = CYAN) } })
        if (showNotes) AlertDialog(onDismissRequest = { showNotes = false }, containerColor = PANEL, title = { Text("Mahalliy eslatmalar", color = WHITE) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(noteDraft, { noteDraft = it }, label = { Text("Yangi eslatma") }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PURPLE, unfocusedBorderColor = MUTED, focusedTextColor = WHITE, unfocusedTextColor = WHITE))
                if (notes.isNotBlank()) Text("Saqlangan eslatmalar:\n$notes", color = MUTED, fontSize = 12.sp)
            }
        }, confirmButton = { TextButton(onClick = { if (noteDraft.isNotBlank()) { notes = (notes + "\n• " + noteDraft).trim(); prefs.edit().putString("notes", notes).apply(); noteDraft = "" }; showNotes = false }) { Text("Saqlash", color = CYAN) } }, dismissButton = { TextButton(onClick = { showNotes = false }) { Text("Bekor", color = MUTED) } })
    }
}

@Composable private fun Quick(icon: String, title: String, modifier: Modifier = Modifier, action: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xAA1A1430)).border(1.dp, Color(0x664F3B76), RoundedCornerShape(14.dp)).clickable { action() }.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = CYAN, fontSize = 19.sp); Spacer(Modifier.height(3.dp)); Text(title, color = WHITE, fontSize = 10.sp)
    }
}

@Composable private fun Bubble(line: ChatLine) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.user) Arrangement.End else Arrangement.Start) {
        Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(15.dp)).background(if (line.user) Color(0xFF382052) else Color(0xFF151D2C)).border(1.dp, if (line.user) PURPLE.copy(alpha=.6f) else CYAN.copy(alpha=.3f), RoundedCornerShape(15.dp)).padding(11.dp)) {
            Text(if (line.user) "SIZ" else "JARVIS", color = if (line.user) PURPLE else CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp)); Text(line.text, color = WHITE, fontSize = 13.sp)
        }
    }
}

private fun answer(context: Context, raw: String, notes: String, lang: String): String {
    val s = raw.trim().lowercase(Locale.ROOT)
    if (s.startsWith("internetdan qidir") || s.startsWith("qidir:")) return "WEB_SEARCH: Qidiruv brauzerda ochildi."
    if (s in listOf("salom", "hello", "привет") || s.startsWith("assalomu alaykum")) return when(lang) { "en" -> "Hello! I'm JARVIS. I can run local commands and open web search."; "ru" -> "Здравствуйте! Я JARVIS. Я выполняю локальные команды и открываю поиск."; else -> "Salom! Men JARVISman. Mahalliy buyruqlarni bajaraman va internet qidiruvini ochaman." }
    if (s.contains("soat") || s.contains("vaqt")) return "Hozir: " + SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault()).format(Date())
    if (s.contains("sana") || s.contains("bugun")) return "Bugungi sana: " + SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("uz")).format(Date())
    if (s.contains("eslatma") || s.contains("note")) return if (notes.isBlank()) "Hozircha eslatma yo‘q. Eslatma tugmasidan saqlang." else "Saqlangan eslatmalar:\n$notes"
    if (s == "youtube" || s.contains("och youtube")) { openUrl(context, "https://www.youtube.com"); return "YouTube ochish buyrug‘i yuborildi." }
    if (s == "google" || s.contains("och google")) { openUrl(context, "https://www.google.com"); return "Google ochish buyrug‘i yuborildi." }
    if (s.startsWith("och ")) {
        val name = s.removePrefix("och ").trim()
        val pkg = when { name.contains("telegram") -> "org.telegram.messenger"; name.contains("whatsapp") || name.contains("vatsap") -> "com.whatsapp"; name.contains("instagram") -> "com.instagram.android"; name.contains("youtube") -> "com.google.android.youtube"; name.contains("chrome") -> "com.android.chrome"; else -> null }
        if (pkg != null) { val launch = context.packageManager.getLaunchIntentForPackage(pkg); if (launch != null) { runCatching { context.startActivity(launch) }; return "$name ilovasini ochish buyrug‘i yuborildi." }; return "$name ilovasi topilmadi yoki telefonda o‘rnatilmagan." }
    }
    if (s.contains("yordam") || s == "help") return "Hozir ishlaydigan funksiyalar:\n• Vaqt va sana\n• Mahalliy eslatmalar\n• Chat tarixini saqlash/tozalash\n• Ovozdan matn kiritish (speech service bo‘lsa)\n• Javobni ovozda o‘qish\n• Google/YouTube ochish\n• Brauzerda internet qidiruvi\n\nErkin AI javoblari uchun haqiqiy AI backend ulanishi kerak."
    return when(lang) { "en" -> "This version supports offline commands, notes, time/date, speech input and web search. General AI answers need a real AI backend."; "ru" -> "Эта версия поддерживает офлайн-команды, заметки, время/дату, голосовой ввод и веб-поиск. Для ответов ИИ нужен настоящий AI-сервер."; else -> "Bu versiyada mahalliy buyruqlar, eslatmalar, vaqt/sana, ovozli kiritish va internet qidiruvi bor. Erkin AI javoblari uchun haqiqiy AI serveri kerak — mavjud bo‘lmagan funksiyani bor deb ko‘rsatmayman." }
}
private fun openUrl(context: Context, url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
private fun loadHistory(prefs: android.content.SharedPreferences): List<ChatLine> = runCatching {
    val a = JSONArray(prefs.getString("history", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); ChatLine(o.optBoolean("user"), o.optString("text")) }
}.getOrDefault(emptyList())
private fun saveHistory(prefs: android.content.SharedPreferences, history: List<ChatLine>) {
    val a = JSONArray(); history.forEach { a.put(JSONObject().put("user", it.user).put("text", it.text)) }; prefs.edit().putString("history", a.toString()).apply()
}
