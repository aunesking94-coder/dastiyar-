package com.dastiyar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dastiyar.app.data.Habit
import com.dastiyar.app.data.Memory
import com.dastiyar.app.data.Slot
import com.dastiyar.app.data.Task
import com.dastiyar.app.util.Dates

/* ================= اشتراکی ================= */

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
fun EmptyHint(text: String) {
    DastiyarCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ================= امروز ================= */

private fun greeting(hour: Int): String = when {
    hour < 5 -> "شب بخیر"
    hour < 12 -> "صبح بخیر"
    hour < 17 -> "ظهر بخیر"
    hour < 20 -> "عصر بخیر"
    else -> "شب بخیر"
}

@Composable
fun TodayScreen(vm: MainViewModel, onNavigate: (String) -> Unit) {
    val slots by vm.todaySlots.collectAsState()
    val tomorrow by vm.tomorrowSlots.collectAsState()
    val todayTasks by vm.todayTasks.collectAsState()
    val habits by vm.habits.collectAsState()
    val profile by vm.profile.collectAsState()
    val overdue by vm.overdue.collectAsState()
    var showWakeDialog by remember { mutableStateOf(false) }

    val habitDoneToday = habits.count { it.doneOn(Dates.today()) }

    val unique = slots.distinctBy { it.time }
    val done = unique.filter { it.done || it.kind != "task" }.size
    val total = unique.size
    val next = unique.firstOrNull { !it.done }
    val pct = if (total > 0) Dates.faDigits(((done * 100f) / total).toInt().toString()) + "٪" else "—"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    greeting(Dates.nowMinutes() / 60),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (profile.name.isNotBlank()) profile.name else "دستیار برنامه‌ریزی",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        Dates.faDate(Dates.today()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (total > 0) {
                        StatusPill(
                            "${Dates.faDigits("$done")}/${Dates.faDigits("$total")} انجام",
                            if (done == total) DastiyarSuccess else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            DastiyarCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("برنامهٔ امروز", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (total == 0) "هنوز برنامه‌ای ندارم"
                                else if (next == null) "همه‌چیز تکمیل است"
                                else "بعدی: ${Dates.faTime(next.time)} — ${next.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            pct,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (total > 0) done / total.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(
                        "کارها",
                        if (todayTasks.isNotEmpty()) "${Dates.faDigits(todayTasks.size.toString())} کار" else "چیزی نیست",
                        Icons.Outlined.Checklist,
                        Modifier.weight(1f)
                    ) { onNavigate("tasks") }
                    QuickTile(
                        "عادت‌ها",
                        if (habitDoneToday > 0) "${Dates.faDigits("$habitDoneToday")} امروز" else "ثبت نشده",
                        Icons.Outlined.FitnessCenter,
                        Modifier.weight(1f)
                    ) { onNavigate("habits") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile("گفتگو", "بپرس یا بگو", Icons.Outlined.Chat, Modifier.weight(1f)) { onNavigate("chat") }
                    QuickTile("گزارش", "آمار هفتگی", Icons.Outlined.Assessment, Modifier.weight(1f)) { onNavigate("reports") }
                }
            }
        }

        if (overdue.isNotEmpty()) {
            item {
                DastiyarCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = DastiyarDanger.copy(alpha = 0.08f)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = DastiyarDanger, modifier = Modifier.size(16.dp))
                            Text("کارهای عقب‌مانده", style = MaterialTheme.typography.titleSmall, color = DastiyarDanger)
                        }
                        overdue.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(5.dp).background(DastiyarDanger, RoundedCornerShape(3.dp)))
                                Text(
                                    "${t.title} (${Dates.faDateShort(t.date)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DastiyarButton(
                        "بازسازی امروز",
                        onClick = { vm.buildToday() },
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.weight(1f)
                    )
                    DastiyarOutlinedButton(
                        "ساخت فردا (${Dates.faDigits(tomorrow.size.toString())})",
                        onClick = { vm.buildTomorrowOnDemand() },
                        modifier = Modifier.weight(1f)
                    )
                }
                val wakeSlot = slots.firstOrNull { it.title == "بیدار شدن" }
                val wakeDone = wakeSlot?.done == true
                val canLogWake = wakeSlot != null && !wakeDone && Dates.nowMinutes() >= Dates.timeToMin(wakeSlot.time) - 30
                if (canLogWake) {
                    DastiyarButton(
                        "ثبت بیدار شدی",
                        onClick = { showWakeDialog = true },
                        icon = Icons.Outlined.WbSunny,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (slots.isEmpty()) {
            item {
                EmptyHint("برنامهٔ امروز ساخته نشده. دکمهٔ «بازسازی امروز» را بزن یا در گفتگو به دستیار بگو امروز چه کارهایی داری.")
            }
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
private fun QuickTile(
    label: String,
    desc: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    DastiyarCard(modifier = modifier, onClick = onClick) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SlotRow(slot: Slot, onToggle: () -> Unit) {
    val bg = when {
        slot.done -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        slot.kind == "task" && slot.priority == "high" -> DastiyarDanger.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }
    DastiyarCard(modifier = Modifier.fillMaxWidth(), containerColor = bg) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = slot.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        slot.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (slot.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (slot.kind == "task" && slot.priority == "high") {
                        Spacer(Modifier.width(8.dp))
                        StatusPill("مهم", DastiyarDanger)
                    }
                }
                if (slot.note.isNotBlank()) {
                    Text(
                        slot.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    Dates.faTime(slot.time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    Dates.faTime(slot.endTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن کار")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (overdue.isNotEmpty()) {
                item { SectionHeader("عقب‌مانده (${Dates.faDigits(overdue.size.toString())})") }
                items(overdue, key = { it.id }) { t -> TaskRow(t, vm) }
            }
            item { SectionHeader("امروز", Dates.faDateShort(Dates.today())) }
            if (today.isEmpty()) item { EmptyHint("کاری ثبت نشده.") }
            items(today, key = { it.id }) { t -> TaskRow(t, vm) }
            item { SectionHeader("فردا") }
            if (tomorrow.isEmpty()) item { EmptyHint("کاری ثبت نشده.") }
            items(tomorrow, key = { it.id }) { t -> TaskRow(t, vm) }
        }
    }

    if (showAdd) AddTaskDialog(vm) { showAdd = false }
}

@Composable
private fun TaskRow(t: Task, vm: MainViewModel) {
    DastiyarCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (t.priority == "high") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = t.done,
                onCheckedChange = { vm.toggleTask(t) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(Dates.faDateShort(t.date))
                        t.time?.let { append(" · ${Dates.faTime(it)}") }
                        append(" · ${Dates.faDigits(t.durationMin.toString())} دقیقه · ${t.category}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (t.priority == "high") {
                Spacer(Modifier.width(8.dp))
                StatusPill("مهم", DastiyarDanger)
            }
            IconButton(onClick = { vm.deleteTask(t) }) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("عادت‌های روزانه")
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = { showAdd = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Add, "افزودن عادت")
                }
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
    val progress = (thisWeek.toFloat() / h.targetPerWeek).coerceIn(0f, 1f)
    DastiyarCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = doneToday,
                onCheckedChange = { vm.toggleHabit(h) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        h.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${Dates.faDigits(thisWeek.toString())}/${Dates.faDigits(h.targetPerWeek.toString())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress >= 1f) DastiyarSuccess else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            IconButton(onClick = { vm.deleteHabit(h) }) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

/* ================= گفتگو ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MainViewModel) {
    val chat by vm.chat.collectAsState()
    val sending by vm.sending.collectAsState()
    val settings by vm.settings.collectAsState()
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(top = 6.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!settings.geminiKey.isNullOrBlank()) {
                item {
                    ChatBubble(
                        text = "سلام! من دستیار برنامه‌ریزی شما هستم. بگو امروز و فردا چه کارهایی داری، یا «امروز خسته‌ام» تا برنامه‌ات را سبک‌تر کنم.",
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
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("مثلاً: فردا ساعت ۴ باشگاه دارم") },
                maxLines = 3,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.width(10.dp))
            FilledIconButton(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) { text = ""; vm.sendChat(t) }
                },
                enabled = text.isNotBlank() && !sending,
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Send, contentDescription = "ارسال")
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, mine: Boolean) {
    val shape = if (mine)
        RoundedCornerShape(16.dp, 4.dp, 4.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 4.dp)
    val bg = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = if (mine) 48.dp else 0.dp, start = if (mine) 0.dp else 48.dp)
    ) {
        Column(modifier = Modifier.align(if (mine) Alignment.CenterEnd else Alignment.CenterStart)) {
            Text(
                text,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(shape)
                    .background(bg)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("گزارش ۷ روز اخیر") }
        val w = weekly
        if (w == null) {
            item { EmptyHint("در حال محاسبه...") }
        } else {
            item {
                DastiyarCard(modifier = Modifier.fillMaxWidth()) {
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
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    DastiyarCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = DastiyarDanger.copy(alpha = 0.08f)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("اغلب عقب افتاده", style = MaterialTheme.typography.titleSmall, color = DastiyarDanger)
                            w.procrastinated.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            if (w.habitRows.isNotEmpty()) {
                item { SectionHeader("عادت‌ها") }
                items(w.habitRows) { (name, count) ->
                    DastiyarCard(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f))
                            Text(Dates.faDigits("$count"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    DastiyarCard(modifier = modifier) {
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("حافظهٔ دستیار") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("چیزی یادم بگذار...") },
                    maxLines = 2,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.width(10.dp))
                FilledIconButton(
                    enabled = text.isNotBlank(),
                    onClick = { vm.addMemory(text.trim(), ""); text = "" },
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.MenuBook, "ذخیره در حافظه")
                }
            }
        }
        if (memories.isEmpty()) item { EmptyHint("هنوز چیزی ذخیره نشده.") }
        items(memories, key = { it.id }) { m ->
            DastiyarCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { vm.deleteMemory(m) }) {
                        Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup(
                title = "پروفایل",
                subtitle = "روتین روزانه و سلیقهٔ تو"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        }

        item {
            SettingsGroup(title = "روزهای ورزش") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("morning" to "صبح", "afternoon" to "ظهر", "evening" to "شب").forEach { (v, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = bestEnergy == v, onClick = { bestEnergy = v })
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    DastiyarButton(
                        "ذخیرهٔ پروفایل و به‌روزرسانی برنامه‌ها",
                        onClick = {
                            vm.saveProfile(
                                name = name, wakeTime = wake, sleepTime = sleep,
                                breakfastTime = breakfast, lunchTime = lunch, dinnerTime = dinner,
                                workoutTime = workout, workoutDays = profile.workoutDays,
                                workoutMin = workoutMin.toIntOrNull() ?: 60, bestEnergy = bestEnergy, city = city
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "هوش مصنوعی (Gemini)", subtitle = "کلید فقط روی همین گوشی می‌ماند") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        "کلید از https://aistudio.google.com ساخته می‌شود و مستقیم به سرویس جمینی می‌رود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "برنامه و اعلان") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    DastiyarButton(
                        "ذخیرهٔ تنظیمات",
                        onClick = {
                            vm.saveSettings(geminiKey, geminiModel, nightlyHour, notifyMin.toIntOrNull() ?: 10, proactiveNote, useBedtime)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, subtitle)
        DastiyarCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                content()
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