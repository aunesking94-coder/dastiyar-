package com.dastiyar.app.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dastiyar.app.data.Habit
import com.dastiyar.app.data.Memory
import com.dastiyar.app.data.Profile
import com.dastiyar.app.data.Settings
import com.dastiyar.app.data.Slot
import com.dastiyar.app.data.Task
import com.dastiyar.app.util.Dates
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/* ================= مشترک ================= */

@Composable
fun TimeDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var hh by remember { mutableStateOf(Dates.timeToMin(initial) / 60) }
    var mm by remember { mutableStateOf(Dates.timeToMin(initial) % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ساعت") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = hh.toString().padStart(2, '0'),
                    onValueChange = { hh = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("ساعت") },
                    modifier = Modifier.width(90.dp)
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = mm.toString().padStart(2, '0'),
                    onValueChange = { mm = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("دقیقه") },
                    modifier = Modifier.width(90.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(Dates.minToTime(hh * 60 + mm)) }) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun EmptyHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ================= امروز ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: MainViewModel) {
    val slots by vm.todaySlots.collectAsState()
    val tomorrow by vm.tomorrowSlots.collectAsState()
    val profile by vm.profile.collectAsState()
    val overdue by vm.overdue.collectAsState()
    var showWakeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${Dates.faDate(Dates.today())}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    val done = slots.filter { it.done || it.kind != "task" }.distinctBy { it.time }.size
                    val total = slots.distinctBy { it.time }.size
                    Text(
                        "برنامهٔ امروز: ${Dates.faDigits("$done")}/${Dates.faDigits("$total")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (overdue.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("کارهای عقب‌مانده", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                        overdue.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${t.title} (${Dates.faDateShort(t.date)})")
                            }
                        }
                    }
                }
            }
        }

        item {
            val wakeSlot = slots.firstOrNull { it.title == "بیدار شدن" }
            val wakeDone = wakeSlot?.done == true
            val canLogWake = wakeSlot != null && !wakeDone && Dates.nowMinutes() >= Dates.timeToMin(wakeSlot.time) - 30
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.buildToday() }, modifier = Modifier.weight(1f)) { Text("بازسازی امروز") }
                OutlinedButton(onClick = { vm.buildTomorrowOnDemand() }, modifier = Modifier.weight(1f)) {
                    Text("ساخت فردا (${Dates.faDigits(tomorrow.size.toString())})")
                }
            }
            if (canLogWake) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showWakeDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.WbSunny, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت بیدار شدی")
                }
            }
        }

        if (slots.isEmpty()) {
            item { EmptyHint("برنامهٔ امروز ساخته نشده. دکمهٔ «بازسازی امروز» را بزن یا در چت به دستیار بگو امروز چه کارهایی داری.") }
        }

        items(slots, key = { it.id }) { slot ->
            SlotRow(slot = slot, onToggle = { vm.toggleSlot(slot) })
        }
    }

    if (showWakeDialog) {
        TimeDialog(
            initial = Dates.nowTime(),
            onDismiss = { showWakeDialog = false },
            onConfirm = { t ->
                showWakeDialog = false
                vm.logWake(t)
            }
        )
    }
}

@Composable
private fun SlotRow(slot: Slot, onToggle: () -> Unit) {
    val color = when {
        slot.done -> MaterialTheme.colorScheme.surfaceVariant
        slot.kind == "task" && slot.priority == "high" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = slot.done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(slot.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${Dates.faTime(slot.time)} — ${Dates.faTime(slot.endTime)}" +
                        (if (slot.note.isNotBlank()) "  (${slot.note})" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (slot.kind == "task" && slot.priority == "high") {
                Text("مهم", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/* ================= کارها ================= */

@Composable
fun TasksScreen(vm: MainViewModel) {
    val today by vm.todayTasks.collectAsState()
    val tomorrow by vm.tomorrowTasks.collectAsState()
    val overdue by vm.overdue.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "افزودن کار")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (overdue.isNotEmpty()) {
                item { SectionTitle("عقب‌مانده (${Dates.faDigits(overdue.size.toString())})") }
                items(overdue, key = { it.id }) { t -> TaskRow(t, vm) }
            }
            item { SectionTitle("امروز (${Dates.faDateShort(Dates.today())})") }
            if (today.isEmpty()) item { EmptyHint("کاری ثبت نشده.") }
            items(today, key = { it.id }) { t -> TaskRow(t, vm) }
            item { SectionTitle("فردا") }
            if (tomorrow.isEmpty()) item { EmptyHint("کاری ثبت نشده.") }
            items(tomorrow, key = { it.id }) { t -> TaskRow(t, vm) }
        }
    }

    if (showAdd) AddTaskDialog(vm) { showAdd = false }
}

@Composable
private fun TaskRow(t: Task, vm: MainViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (t.priority == "high") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = t.done, onCheckedChange = { vm.toggleTask(t) })
            Column(Modifier.weight(1f)) {
                Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${Dates.faDateShort(t.date)}${t.time?.let { " · ${Dates.faTime(it)}" } ?: ""} · ${Dates.faDigits(t.durationMin.toString())} دقیقه · ${t.category}" +
                        if (t.priority == "high") " · ✅" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.deleteTask(t) }) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(vm: MainViewModel, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }
    var time by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var priority by remember { mutableStateOf("med") }
    var category by remember { mutableStateOf("عمومی") }
    var timeOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("کار جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان کار") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = date == Dates.today(), onClick = { date = Dates.today() }, label = { Text("امروز") })
                    FilterChip(selected = date == Dates.tomorrow(), onClick = { date = Dates.tomorrow() }, label = { Text("فردا") })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ساعت (اختیاری)") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { timeOpen = true }) { Text("باز") }
                        }
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                        label = { Text("مدت (دقیقه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("اولویت: ", style = MaterialTheme.typography.bodySmall)
                    listOf("high" to "بالا", "med" to "عادی", "low" to "کم").forEach { (v, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = priority == v, onClick = { priority = v })
                            Text(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val timeFinal = time.ifBlank { null }
                    vm.addTask(title.trim(), date, timeFinal, duration.toIntOrNull() ?: 60, priority, category)
                    onClose()
                }
            ) { Text("افزودن") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("انصراف") } }
    )

    if (timeOpen) {
        TimeDialog(
            initial = time.ifBlank { "09:00" },
            onDismiss = { timeOpen = false },
            onConfirm = { time = it; timeOpen = false }
        )
    }
}

/* ================= عادت‌ها ================= */

@Composable
fun HabitsScreen(vm: MainViewModel) {
    val habits by vm.habits.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("عادت‌های روزانه")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "افزودن عادت") }
            }
        }
        if (habits.isEmpty()) item { EmptyHint("عادتی ثبت نشده. مثل «مطالعه»، «ورزش»، «کم‌شیرینی»..." ) }
        items(habits, key = { it.id }) { h ->
            HabitRow(h, vm)
        }
    }

    if (showAdd) AddHabitDialog(vm) { showAdd = false }
}

@Composable
private fun HabitRow(h: Habit, vm: MainViewModel) {
    val doneToday = h.doneOn(Dates.today())
    val thisWeek = remember(h.doneDates) {
        val start = Dates.weekStart()
        try {
            val j = org.json.JSONArray(h.doneDates)
            (0 until j.length()).count { j.optString(it) >= start }
        } catch (e: Exception) {
            0
        }
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = doneToday, onCheckedChange = { vm.toggleHabit(h) })
            Column(Modifier.weight(1f)) {
                Text(h.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${Dates.faDigits(thisWeek.toString())}/${Dates.faDigits(h.targetPerWeek.toString())} این هفته · ${h.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { (thisWeek.toFloat() / h.targetPerWeek).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
            IconButton(onClick = { vm.deleteHabit(h) }) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddHabitDialog(vm: MainViewModel, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("3") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("عادت جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام عادت") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit).take(2) },
                    label = { Text("هدف هفتگی (مرتبه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { vm.addHabit(name.trim(), target.toIntOrNull() ?: 3); onClose() }
            ) { Text("افزودن") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("انصراف") } }
    )
}

/* ================= چت ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MainViewModel) {
    val chat by vm.chat.collectAsState()
    val sending by vm.sending.collectAsState()
    val settings by vm.settings.collectAsState()
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!settings.geminiKey.isNullOrBlank()) {
                item {
                    ChatBubble(
                        text = "سلام! من دستیار برنامه‌ریزی شمایم. بگو امروز و فردا چه کارهایی داری، یا «امروز خسته‌ام» تا برنامه‌ات را سبک‌تر کنم. ✋",
                        mine = false
                    )
                }
            }
            items(chat) { m ->
                ChatBubble(text = m.content, mine = m.role == "user")
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("در حال فکر کردن...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("مثلاً: فردا ساعت ۴ باشگاه دارم") },
                maxLines = 3
            )
            IconButton(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) { text = ""; vm.sendChat(t) }
                },
                enabled = text.isNotBlank() && !sending
            ) {
                Icon(Icons.Default.Send, contentDescription = "ارسال")
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, mine: Boolean) {
    val shape = if (mine) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    val bg = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = if (mine) 0.dp else 40.dp, start = if (mine) 40.dp else 0.dp)
    ) {
        Column(modifier = Modifier.align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)) {
            Text(
                text,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(shape)
                    .background(bg)
                    .padding(12.dp)
            )
        }
    }
}

/* ================= گزارش ================= */

@Composable
fun ReportsScreen(vm: MainViewModel) {
    val weekly by vm.weekly.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionTitle("گزارش ۷ روز اخیر") }
        val w = weekly
        if (w == null) {
            item { EmptyHint("در حال محاسبه...") }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("انجام شدن برنامه‌ها", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${Dates.faDigits(w.done.toString())} از ${Dates.faDigits(w.planned.toString())} کار",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "٪${Dates.faDigits(w.completionPct.toString())}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("روزهای ورزش", Dates.faDigits(w.workoutDays.toString()), Modifier.weight(1f))
                    StatCard(
                        "میانگین خواب",
                        w.sleepAvgMin?.let { Dates.faDigits("${it / 60}س${it % 60}") } ?: "—",
                        Modifier.weight(1f)
                    )
                }
            }
            if (w.procrastinated.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
                        Column(Modifier.padding(12.dp)) {
                            Text("اغلب عقب افتاده", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                            w.procrastinated.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            if (w.habitRows.isNotEmpty()) {
                item { SectionTitle("عادت‌ها") }
                items(w.habitRows) { (name, count) ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f))
                            Text(Dates.faDigits("$count"), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ================= حافظه ================= */

@Composable
fun MemoryScreen(vm: MainViewModel) {
    val memories by vm.memories.collectAsState()
    var text by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionTitle("حافظهٔ دستیار")
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("چیزی یادم بگذار...") },
                    maxLines = 2
                )
                IconButton(
                    enabled = text.isNotBlank(),
                    onClick = { vm.addMemory(text.trim(), ""); text = "" }
                ) {
                    Icon(Icons.Outlined.MenuBook, "ذخیره در حافظه")
                }
            }
        }
        if (memories.isEmpty()) item { EmptyHint("هنوز چیزی ذخیره نشده.") }
        items(memories, key = { it.id }) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { vm.deleteMemory(m) }) {
                        Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/* ================= تنظیمات ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsState()
    val settings by vm.settings.collectAsState()

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var wake by remember(profile.wakeTime) { mutableStateOf(profile.wakeTime) }
    var sleep by remember(profile.sleepTime) { mutableStateOf(profile.sleepTime) }
    var breakfast by remember(profile.breakfastTime) { mutableStateOf(profile.breakfastTime) }
    var lunch by remember(profile.lunchTime) { mutableStateOf(profile.lunchTime) }
    var dinner by remember(profile.dinnerTime) { mutableStateOf(profile.dinnerTime) }
    var workout by remember(profile.workoutTime) { mutableStateOf(profile.workoutTime) }
    var workoutMin by remember(profile.workoutMin) { mutableStateOf(profile.workoutMin.toString()) }
    var bestEnergy by remember(profile.bestEnergy) { mutableStateOf(profile.bestEnergy) }
    var city by remember(profile.city) { mutableStateOf(profile.city) }

    var geminiKey by remember(settings.geminiKey) { mutableStateOf(settings.geminiKey) }
    var geminiModel by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
    var nightlyHour by remember(settings.nightlyHour) { mutableStateOf(settings.nightlyHour) }
    var notifyMin by remember(settings.notifyMin) { mutableStateOf(settings.notifyMin.toString()) }
    var proactiveNote by remember(settings.proactiveSleepNote) { mutableStateOf(settings.proactiveSleepNote) }
    var useBedtime by remember(settings.useBedtimeEstimate) { mutableStateOf(settings.useBedtimeEstimate) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionTitle("پروفایل") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, modifier = Modifier.fillMaxWidth())
                TimeField("وقت بیداری", wake) { wake = it }
                TimeField("وقت خواب", sleep) { sleep = it }
                TimeField("صبحانه", breakfast) { breakfast = it }
                TimeField("ناهار", lunch) { lunch = it }
                TimeField("شام", dinner) { dinner = it }
                TimeField("باشگاه", workout) { workout = it }
                OutlinedTextField(
                    value = workoutMin,
                    onValueChange = { workoutMin = it.filter(Char::isDigit).take(3) },
                    label = { Text("مدت باشگاه (دقیقه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        item {
            SectionTitle("روزهای ورزش")
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val weekNames = listOf("دوشنبه" to 1, "سه‌شنبه" to 2, "چهارشنبه" to 3, "پنجشنبه" to 4, "جمعه" to 5, "شنبه" to 6, "یکشنبه" to 7)
                items(weekNames) { (n, v) ->
                    FilterChip(
                        selected = profile.workoutDays.contains(v),
                        onClick = {
                            val newDays = if (profile.workoutDays.contains(v))
                                profile.workoutDays.filter { it != v }
                            else (profile.workoutDays + v).sorted()
                            vm.saveProfile(workoutDays = newDays)
                        },
                        label = { Text(n, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("بهترین زمان انرژی")
                listOf("morning" to "صبح", "afternoon" to "ظهر", "evening" to "شب").forEach { (v, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = bestEnergy == v, onClick = { bestEnergy = v })
                        Text(label)
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    vm.saveProfile(
                        name = name, wakeTime = wake, sleepTime = sleep,
                        breakfastTime = breakfast, lunchTime = lunch, dinnerTime = dinner,
                        workoutTime = workout, workoutDays = profile.workoutDays,
                        workoutMin = workoutMin.toIntOrNull() ?: 60, bestEnergy = bestEnergy, city = city
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیرهٔ پروفایل و به‌روزرسانی برنامه‌ها") }
        }

        item { SectionTitle("هوش مصنوعی (Gemini)") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("کلید API جمینی") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = geminiModel,
                    onValueChange = { geminiModel = it },
                    label = { Text("مدل (مثلاً gemini-2.5-flash)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "کلید فقط روی همین گوشی ذخیره می‌شود و مستقیم به سرویس جمینی می‌رود. از https://aistudio.google.com بگیرش.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SectionTitle("برنامه و اعلان") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("ساعت ساخت برنامهٔ فردا (مثلاً ۲۲:۰۰)", nightlyHour) { nightlyHour = it }
                OutlinedTextField(
                    value = notifyMin,
                    onValueChange = { notifyMin = it.filter(Char::isDigit).take(2) },
                    label = { Text("یادآوری چند دقیقه قبل از هر کار") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("اعلان پیش‌دستانهٔ خواب", modifier = Modifier.weight(1f))
                    Switch(checked = proactiveNote, onCheckedChange = { proactiveNote = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("محاسبهٔ میانگین خواب از برنامه", modifier = Modifier.weight(1f))
                    Switch(checked = useBedtime, onCheckedChange = { useBedtime = it })
                }
                Button(
                    onClick = {
                        vm.saveSettings(geminiKey, geminiModel, nightlyHour, notifyMin.toIntOrNull() ?: 10, proactiveNote, useBedtime)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ذخیرهٔ تنظیمات") }
            }
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = { open = true }) { Text("تغییر") }
        }
    )
    if (open) {
        TimeDialog(initial = value, onDismiss = { open = false }, onConfirm = { onChange(it); open = false })
    }
}