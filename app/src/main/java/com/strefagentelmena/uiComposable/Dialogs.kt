package com.strefagentelmena.uiComposable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.R
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.CustomerNote
import com.strefagentelmena.models.ageShortLabel
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val dialogsUI = Dialogs()

class Dialogs {

    companion object {
        private val visitHistoryDateParse = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val visitHistoryWeekdayPl =
            DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("pl-PL"))
        private val noteAddedAtFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    private fun formatNoteAddedAt(millis: Long): String {
        if (millis <= 0L) return "wcześniej"
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(noteAddedAtFormat)
    }

    private fun visitHistoryDateWithWeekday(raw: String): String {
        if (raw.isEmpty()) return "—"
        val date = runCatching { LocalDate.parse(raw, visitHistoryDateParse) }.getOrNull()
            ?: return raw
        return "$raw (${visitHistoryWeekdayPl.format(date)})"
    }

    @Composable
    fun OnAddOrEditCustomerDialog(
        viewModel: CustomersModelView,
        onClose: () -> Unit,
        onAddCustomer: () -> Unit,
        onEditCustomer: () -> Unit,
        onDeleteCustomer: () -> Unit,
    ) {
        val selectedCustomer by viewModel.selectedCustomer.observeAsState()

        val firstName by viewModel.customerName.observeAsState("")
        val lastName by viewModel.customerLastName.observeAsState("")
        val phoneNumber by viewModel.customerPhoneNumber.observeAsState("")
        val email by viewModel.customerEmail.observeAsState("")
        val note by viewModel.customerNote.observeAsState("")
        val noteHistory by viewModel.customerNoteHistory.observeAsState(emptyList())
        val noteEditTarget by viewModel.customerNoteEditTarget.observeAsState(null)
        var noteHistoryExpanded by remember(selectedCustomer?.id) { mutableStateOf(true) }
        var notePendingDelete by remember(selectedCustomer?.id) { mutableStateOf<CustomerNote?>(null) }

        //errors fromViewModel
        val firstNameError by viewModel.firstNameError.observeAsState("")
        val lastNameError by viewModel.lastNameError.observeAsState("")
        val phoneNumberError by viewModel.phoneNumberError.observeAsState("")

        val headerText =
            if (selectedCustomer == null) "Nowy klient" else "Edytuj klienta"

        val focusRequester = remember { FocusRequester() }

        val visitHistory by viewModel.customerVisitHistory.observeAsState(emptyList())
        val visitHistoryLoading by viewModel.customerVisitHistoryLoading.observeAsState(false)
        var visitHistoryExpanded by remember(selectedCustomer?.id) { mutableStateOf(true) }

        val allCustomers by viewModel.customersLists.observeAsState(emptyList())
        var pickChildOpen by remember(selectedCustomer?.id) { mutableStateOf(false) }

        val addNewChildVisible by viewModel.addNewChildDialogVisible.observeAsState(false)
        val newChildFirst by viewModel.newChildFirstName.observeAsState("")
        val newChildAge by viewModel.newChildAgeYears.observeAsState("")
        val newChildBirth by viewModel.newChildBirthDate.observeAsState("")
        val newChildFirstErr by viewModel.newChildFirstNameError.observeAsState("")
        val context = LocalContext.current

        LaunchedEffect(selectedCustomer) {
            val customer = selectedCustomer
            if (customer != null) {
                viewModel.setSelectedCustomerData()
                viewModel.loadCustomerVisitHistory(
                    FirebaseDatabase.getInstance(),
                    customer.id,
                )
            } else {
                viewModel.clearSelectedClientAndData()
            }
        }

        Dialog(
            onDismissRequest = { onClose() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        headersUI.AppBarWithBackArrow(
                            title = headerText,
                            onClick = {},
                            onBackPressed = { onClose() },
                            compose = {
                                if (selectedCustomer != null) {
                                    buttonsUI.HeaderIconButton(
                                        icon = R.drawable.ic_delete,
                                        onClick = {
                                            onDeleteCustomer()
                                        },
                                        iconColor = Color.White,
                                        containerColor = colorsUI.amaranthPurple
                                    )
                                }
                            })

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                                .padding(bottom = 96.dp),
                        ) {
                        textModernTextFieldUI.ModernTextField(
                            value = firstName,
                            onValueChange = {
                                viewModel.setCustomerName(it)
                                if (it.isNotBlank()) {
                                    viewModel.validateFirstName(it)
                                }
                            },
                            label = "Imię",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            isError = firstNameError.isNotEmpty(),
                            supportText = firstNameError,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Person",
                                )
                            },
                        )

                        textModernTextFieldUI.ModernTextField(
                            value = lastName,
                            onValueChange = {
                                viewModel.setCustomerLastName(it)

                                if (it.isNotBlank()) {
                                    viewModel.validateLastName(it)
                                }
                            },
                            label = "Nazwisko",
                            isError = lastNameError.isNotEmpty(),
                            supportText = lastNameError,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Person",
                                )
                            },
                        )

                        textModernTextFieldUI.ModernTextField(
                            value = phoneNumber,
                            onValueChange = {
                                viewModel.setCustomerPhoneNumber(it)
                                if (it.isNotBlank()) {
                                    viewModel.validatePhoneNumber(it)
                                }
                            },
                            label = "Numer Telefonu",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneNumberError.isNotEmpty(),
                            supportText = phoneNumberError,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = "Person",
                                )
                            },
                        )
                        textModernTextFieldUI.ModernTextField(
                            value = email,
                            onValueChange = { viewModel.setCustomerEmail(it) },
                            label = "E-mail",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = false,
                            supportText = null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                )
                            },
                        )

                        selectedCustomer?.let { cust ->
                            if (cust.id > 0 && cust.parentCustomerId > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val familyShape = RoundedCornerShape(16.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(familyShape)
                                        .background(colorsUI.papaya)
                                        .border(0.5.dp, colorsUI.border, familyShape)
                                        .padding(14.dp),
                                ) {
                                    Text(
                                        text = "Powiązanie rodzic–dziecko",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorsUI.rusticBrown,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val parent = allCustomers.firstOrNull { it.id == cust.parentCustomerId }
                                    Text(
                                        text = "Rodzic: ${parent?.fullName ?: "nieznany (ID ${cust.parentCustomerId})"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorsUI.fontGrey,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.unlinkChildParent(FirebaseDatabase.getInstance(), cust.id)
                                        },
                                    ) {
                                        Text(
                                            text = "Odłącz od rodzica",
                                            color = colorsUI.amaranthPurple,
                                        )
                                    }
                                }
                            } else if (cust.id > 0 && cust.parentCustomerId == 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val familyShape = RoundedCornerShape(16.dp)
                                val children =
                                    allCustomers.filter { it.parentCustomerId == cust.id }
                                        .sortedBy { it.fullName }
                                val eligible = viewModel.eligibleToAssignAsChild(cust.id)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(familyShape)
                                        .background(colorsUI.papaya)
                                        .border(0.5.dp, colorsUI.border, familyShape)
                                        .padding(14.dp),
                                ) {
                                    Text(
                                        text = "Dzieci w profilu",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorsUI.rusticBrown,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (children.isEmpty()) {
                                        Text(
                                            text = "Brak przypisanych dzieci. Możesz powiązać istniejące konto klienta (bez własnych dzieci).",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorsUI.darkGrey,
                                        )
                                    } else {
                                        children.forEach { ch ->
                                            val agePart = ch.ageShortLabel()?.let { " · $it" }.orEmpty()
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = ch.fullName + agePart,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colorsUI.fontGrey,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                TextButton(
                                                    onClick = {
                                                        viewModel.unlinkChildParent(
                                                            FirebaseDatabase.getInstance(),
                                                            ch.id,
                                                        )
                                                    },
                                                ) {
                                                    Text(
                                                        text = "Usuń",
                                                        color = colorsUI.amaranthPurple,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.openAddNewChildDialog() }) {
                                        Text(
                                            text = "Dodaj nowe dziecko",
                                            color = colorsUI.jade,
                                        )
                                    }
                                    if (eligible.isNotEmpty()) {
                                        TextButton(onClick = { pickChildOpen = true }) {
                                            Text(
                                                text = "Dodaj dziecko z listy klientów",
                                                color = colorsUI.jade,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val notesSectionShape = RoundedCornerShape(16.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(notesSectionShape)
                                .background(colorsUI.papaya)
                                .border(0.5.dp, colorsUI.border, notesSectionShape),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { noteHistoryExpanded = !noteHistoryExpanded }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBox,
                                    contentDescription = null,
                                    tint = colorsUI.rusticBrown,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notatki",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorsUI.fontGrey,
                                    )
                                    if (noteHistory.isNotEmpty()) {
                                        Text(
                                            text = "${noteHistory.size} wpisów",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorsUI.darkGrey,
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (noteHistoryExpanded) {
                                        Icons.Filled.KeyboardArrowUp
                                    } else {
                                        Icons.Filled.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = colorsUI.murrey,
                                )
                            }
                            AnimatedVisibility(visible = noteHistoryExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                                ) {
                                    val editTarget = noteEditTarget
                                    if (noteHistory.isEmpty()) {
                                        Text(
                                            text = "Brak zapisanych notatek. Dodaj pierwszą poniżej.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorsUI.darkGrey,
                                        )
                                    } else {
                                        noteHistory.forEach { entry: CustomerNote ->
                                            val cardShape = RoundedCornerShape(12.dp)
                                            val isBeingEdited =
                                                editTarget != null &&
                                                    editTarget.addedAtMillis == entry.addedAtMillis &&
                                                    editTarget.text == entry.text
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(
                                                        width = if (isBeingEdited) 1.dp else 0.5.dp,
                                                        color = if (isBeingEdited) colorsUI.jade else colorsUI.border,
                                                        shape = cardShape,
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = colorsUI.raisinBlack,
                                                ),
                                                shape = cardShape,
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.Top,
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = formatNoteAddedAt(entry.addedAtMillis),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colorsUI.darkGrey,
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = entry.text,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = colorsUI.fontGrey,
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.beginEditCustomerNote(entry) },
                                                        enabled = editTarget == null,
                                                        modifier = Modifier.size(40.dp),
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Edit,
                                                            contentDescription = "Edytuj notatkę",
                                                            tint = if (editTarget == null) colorsUI.jade else colorsUI.darkGrey,
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { notePendingDelete = entry },
                                                        enabled = editTarget == null,
                                                        modifier = Modifier.size(40.dp),
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = "Usuń notatkę",
                                                            tint = if (editTarget == null) colorsUI.murrey else colorsUI.darkGrey,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    textModernTextFieldUI.ModernTextField(
                                        value = note,
                                        onValueChange = { viewModel.setSelectedCustomerNote(it) },
                                        label = if (editTarget != null) "Edytuj notatkę" else "Nowa notatka",
                                        isError = false,
                                        supportText = null,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountBox,
                                                contentDescription = null,
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp),
                                    )
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        if (editTarget != null) {
                                            TextButton(onClick = { viewModel.cancelCustomerNoteEdit() }) {
                                                Text("Anuluj", color = colorsUI.darkGrey)
                                            }
                                            TextButton(
                                                onClick = { viewModel.applyCustomerNoteEdit() },
                                                enabled = note.isNotBlank(),
                                            ) {
                                                Text(
                                                    "Zapisz zmianę",
                                                    color = if (note.isNotBlank()) colorsUI.jade else colorsUI.darkGrey,
                                                )
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { viewModel.appendCustomerNote() },
                                                enabled = note.isNotBlank(),
                                            ) {
                                                Text(
                                                    "Dodaj notatkę",
                                                    color = if (note.isNotBlank()) colorsUI.jade else colorsUI.darkGrey,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        selectedCustomer?.let { c ->
                            val summaryShape = RoundedCornerShape(16.dp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(summaryShape)
                                    .background(colorsUI.papaya)
                                    .border(0.5.dp, colorsUI.border, summaryShape)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = "Podsumowanie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorsUI.rusticBrown,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = c.visitCount.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorsUI.fontGrey,
                                        )
                                        Text(
                                            text = "WIZYTY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorsUI.darkGrey,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = if (c.avgWeeksBetweenVisits > 0.0) {
                                                String.format(Locale.US, "%.1f", c.avgWeeksBetweenVisits)
                                            } else {
                                                "—"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorsUI.fontGrey,
                                        )
                                        Text(
                                            text = "TYG. ŚR.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorsUI.darkGrey,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            val historyShape = RoundedCornerShape(16.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(historyShape)
                                    .background(colorsUI.papaya)
                                    .border(0.5.dp, colorsUI.border, historyShape),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { visitHistoryExpanded = !visitHistoryExpanded }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = colorsUI.rusticBrown,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Historia wizyt",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorsUI.fontGrey,
                                        )
                                        if (!visitHistoryLoading && visitHistory.isNotEmpty()) {
                                            Text(
                                                text = "${visitHistory.size} wizyt",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorsUI.darkGrey,
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (visitHistoryExpanded) {
                                            Icons.Filled.KeyboardArrowUp
                                        } else {
                                            Icons.Filled.KeyboardArrowDown
                                        },
                                        contentDescription = if (visitHistoryExpanded) {
                                            "Zwiń historię"
                                        } else {
                                            "Rozwiń historię"
                                        },
                                        tint = colorsUI.murrey,
                                    )
                                }
                                AnimatedVisibility(visible = visitHistoryExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                                    ) {
                                        when {
                                            visitHistoryLoading -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 20.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }

                                            visitHistory.isEmpty() -> {
                                                Text(
                                                    text = "Brak wizyt w harmonogramie dla tego klienta.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colorsUI.darkGrey,
                                                )
                                            }

                                            else -> {
                                                visitHistory.forEach { appointment ->
                                                    val workerLabel = listOf(
                                                        appointment.employee.name,
                                                        appointment.employee.surname,
                                                    ).joinToString(" ") { it.trim() }.trim()
                                                        .ifEmpty { "—" }
                                                    val cardShape = RoundedCornerShape(12.dp)
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .border(0.5.dp, colorsUI.border, cardShape),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = colorsUI.raisinBlack,
                                                        ),
                                                        shape = cardShape,
                                                    ) {
                                                        Column(Modifier.padding(12.dp)) {
                                                            Text(
                                                                text = visitHistoryDateWithWeekday(appointment.date),
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colorsUI.fontGrey,
                                                            )
                                                            Text(
                                                                text = "${appointment.startTime} – ${appointment.endTime}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = colorsUI.fontGrey,
                                                            )
                                                            Text(
                                                                text = workerLabel,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = colorsUI.darkGrey,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Anuluj",
                                    onClick = { onClose() },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogButton(
                                    text = "Zapisz",
                                    onClick = {
                                        if (selectedCustomer == null) {
                                            onAddCustomer()
                                        } else {
                                            onEditCustomer()
                                        }
                                    },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Green,
                                )
                            },
                        )
                    }
                }
            }
        }

        val pendingNoteDelete = notePendingDelete
        if (pendingNoteDelete != null) {
            DeleteCustomerNoteConfirmDialog(
                note = pendingNoteDelete,
                onConfirm = {
                    viewModel.removeCustomerNote(pendingNoteDelete)
                    notePendingDelete = null
                },
                onDismiss = { notePendingDelete = null },
            )
        }

        if (pickChildOpen) {
            val sc = selectedCustomer
            Dialog(onDismissRequest = { pickChildOpen = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = "Wybierz klienta jako dziecko",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorsUI.fontGrey,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val eligible =
                                if (sc != null) viewModel.eligibleToAssignAsChild(sc.id) else emptyList()
                            if (eligible.isEmpty()) {
                                Text(
                                    text = "Brak dostępnych klientów do powiązania.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorsUI.darkGrey,
                                )
                            } else {
                                eligible.forEach { cand ->
                                    TextButton(
                                        onClick = {
                                            if (sc != null) {
                                                viewModel.linkChildToParent(
                                                    FirebaseDatabase.getInstance(),
                                                    sc.id,
                                                    cand.id,
                                                )
                                            }
                                            pickChildOpen = false
                                        },
                                    ) {
                                        Text(
                                            text = cand.fullName,
                                            color = colorsUI.jade,
                                        )
                                    }
                                }
                            }
                        }
                        StrefaDialogFloatingBar(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            StrefaDialogButton(
                                text = "Anuluj",
                                onClick = { pickChildOpen = false },
                                modifier = Modifier.fillMaxWidth(),
                                style = StrefaDialogButtonStyle.Ghost,
                            )
                        }
                    }
                }
            }
        }

        if (addNewChildVisible) {
            val par = selectedCustomer
            Dialog(onDismissRequest = { viewModel.dismissAddNewChildDialog() }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "Nowe dziecko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorsUI.fontGrey,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nazwisko: ${par?.lastName?.trim().orEmpty().ifBlank { "—" }} (jak u rodzica)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorsUI.rusticBrown,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        textModernTextFieldUI.ModernTextField(
                            value = newChildFirst,
                            onValueChange = { viewModel.setNewChildFirstName(it) },
                            label = "Imię",
                            modifier = Modifier.fillMaxWidth(),
                            isError = newChildFirstErr.isNotEmpty(),
                            supportText = newChildFirstErr.ifEmpty { null },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        textModernTextFieldUI.ModernTextField(
                            value = newChildAge,
                            onValueChange = { viewModel.setNewChildAgeYears(it) },
                            label = "Wiek (lata, opcjonalnie)",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = false,
                            supportText = null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Data urodzenia (opcjonalnie)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorsUI.fontGrey,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = newChildBirth.ifBlank { "Nie wybrano" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (newChildBirth.isBlank()) colorsUI.darkGrey else colorsUI.fontGrey,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = {
                                    textModernTextFieldUI.showDatePickerDialog(
                                        context = context,
                                        currentDateDisplay = newChildBirth,
                                        onDateSet = { viewModel.setNewChildBirthDate(it) },
                                        onCancel = {},
                                    )
                                },
                            ) {
                                Text("Kalendarz", color = colorsUI.jade)
                            }
                            if (newChildBirth.isNotBlank()) {
                                TextButton(onClick = { viewModel.clearNewChildBirthDate() }) {
                                    Text("Wyczyść", color = colorsUI.darkGrey)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Jeśli podasz datę urodzenia, wiek będzie liczony automatycznie i co rok się zaktualizuje. Sam wiek zapisujemy jako przybliżenie (1 stycznia danego roku).",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorsUI.darkGrey,
                        )
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Anuluj",
                                    onClick = { viewModel.dismissAddNewChildDialog() },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogButton(
                                    text = "Dodaj dziecko",
                                    onClick = {
                                        viewModel.submitNewChildForParent(FirebaseDatabase.getInstance())
                                    },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Gold,
                                )
                            },
                        )
                    }
                    }
                }
            }
        }
    }

    @Composable
    private fun DeleteCustomerNoteConfirmDialog(
        note: CustomerNote,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val previewText = note.text.trim().let { t ->
            if (t.length > 280) t.take(277) + "…" else t
        }
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StrefaModalPanel {
                        StrefaModalIconFrame(
                            variant = StrefaModalIconVariant.Danger,
                            icon = Icons.Outlined.Delete,
                            iconTint = colorsUI.amaranthPurple,
                        )
                        StrefaModalTitleText(
                            text = "Usunąć notatkę?",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        StrefaModalBodyText(
                            text = "Tej operacji nie można cofnąć.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        StrefaDialogOptionList(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            StrefaDialogInfoListItem(
                                label = "Data",
                                value = formatNoteAddedAt(note.addedAtMillis),
                            )
                            StrefaDialogInfoListItem(
                                label = "Treść",
                                value = previewText,
                            )
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Anuluj",
                                    onClick = onDismiss,
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogDeleteButton(
                                    onClick = onConfirm,
                                    modifier = m,
                                )
                            },
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun OnAddOrEditSchedule(
        viewModel: ScheduleModelView,
    ) {
        val isNewState by viewModel.isNewAppointment.observeAsState(false)
        val deleteDialogState by viewModel.deleteDialogState.observeAsState(false)
        val selectedAppointment by viewModel.selectedAppointment.observeAsState()
        val appointmentError by viewModel.appointmentError.observeAsState("")
        val context = LocalContext.current

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val sendNotification = remember { mutableStateOf(false) }
        var sendSmsOnSave by remember(isNewState) { mutableStateOf(true) }
        val title = if (isNewState) "Dodaj" else "Edytuj"
        fun saveNewAppointment() {
            viewModel.createNewAppointment(
                isNew = isNewState,
                sendSmsOnSave = sendSmsOnSave,
            )
        }
        val smsPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                saveNewAppointment()
            } else {
                viewModel.setMessages("Nie można wysłać SMS bez uprawnienia. Wizyta nie została zapisana.")
            }
        }

        val selectedDate = remember(selectedAppointment, isNewState) {
            selectedAppointment?.date?.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it, formatter) }.getOrNull()
            } ?: LocalDate.now()
        }

        val selectedTime = remember(selectedAppointment, isNewState) {
            selectedAppointment?.startTime?.takeIf { it.isNotBlank() }?.let {
                parseAppointmentTimeString(it)
            } ?: LocalTime.now()
        }


        // Obsługa powiadomień
        LaunchedEffect(selectedAppointment, isNewState) {
            // #region agent log
            com.strefagentelmena.AgentDebugLog.log(
                hypothesisId = "H6",
                location = "Dialogs.OnAddOrEditSchedule.LaunchedEffect",
                message = "notification_reset_gate",
                data = mapOf(
                    "isNewState" to isNewState.toString(),
                    "apptId" to (selectedAppointment?.id?.toString() ?: "null"),
                ),
            )
            // #endregion
            selectedAppointment?.let {
                val parsedApptDate =
                    it.date.takeIf { d -> d.isNotBlank() }?.let { d ->
                        runCatching { LocalDate.parse(d, formatter) }.getOrNull()
                    }
                val parsedApptStart = parseAppointmentTimeString(it.startTime)
                val dateChanged = parsedApptDate != null && selectedDate != parsedApptDate
                val timeChanged = parsedApptStart != null && selectedTime != parsedApptStart
                if (it.id != 0 && (dateChanged || timeChanged)) {
                    sendNotification.value = true
                }
            }

            if (isNewState) {
                viewModel.setSelectedClient(Customer())
                viewModel.selectedAppointment.value = Appointment()
            }
        }

        // Główne okno dialogowe
        Dialog(
            onDismissRequest = { viewModel.changeAppointmentDialogState() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AppHeader(
                            title = title,
                            selectedAppointment = selectedAppointment,
                            onDeleteClick = { viewModel.setDeleteDialogState() },
                            onNotificationClick = { viewModel.openNotificationDialog() },
                            onBackClick = { viewModel.changeAppointmentDialogState() },
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                                .padding(bottom = 96.dp),
                        ) {
                            formUI.AppointmentForm(viewModel = viewModel)
                            if (isNewState) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sendSmsOnSave = !sendSmsOnSave }
                                        .padding(top = 12.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = sendSmsOnSave,
                                        onCheckedChange = { sendSmsOnSave = it },
                                    )
                                    Text(
                                        text = "Wyślij SMS przy zapisie",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Anuluj",
                                    onClick = { viewModel.changeAppointmentDialogState() },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogButton(
                                    text = "Zapisz",
                                    onClick = {
                                        if (isNewState) {
                                            if (
                                                sendSmsOnSave &&
                                                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) !=
                                                PackageManager.PERMISSION_GRANTED
                                            ) {
                                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                            } else {
                                                saveNewAppointment()
                                            }
                                        } else if (
                                            viewModel.editAppointment(
                                                FirebaseDatabase.getInstance(),
                                                sendNotification.value,
                                            )
                                        ) {
                                            viewModel.changeAppointmentDialogState()
                                        }
                                    },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Green,
                                    enabled = appointmentError.isBlank(),
                                )
                            },
                        )
                    }
                }
            }
        }

        // Dialog potwierdzający usunięcie
        if (deleteDialogState) {
            DeleteAppointmentStrefaDialog(
                appointment = selectedAppointment,
                onConfirm = {
                    viewModel.removeAppointment(
                        id = selectedAppointment?.id ?: 0,
                    )
                    viewModel.closeAllDialog()
                },
                onDismiss = { viewModel.setDeleteDialogState() },
            )
        }
    }

    @Composable
    private fun AppHeader(
        title: String,
        selectedAppointment: Appointment?,
        onDeleteClick: () -> Unit,
        onNotificationClick: () -> Unit,
        onBackClick: () -> Unit
    ) {
        headersUI.AppBarWithBackArrow(
            title = title,
            onClick = {}, // Nieobsługiwany
            onBackPressed = onBackClick,
            compose = {
                if (selectedAppointment != null) {
                    buttonsUI.HeaderIconButton(
                        icon = R.drawable.ic_delete,
                        onClick = onDeleteClick,
                        containerColor = colorsUI.amaranthPurple,
                        iconColor = Color.White
                    )
                    if (!selectedAppointment.notificationSent) {
                        buttonsUI.HeaderIconButton(
                            icon = R.drawable.ic_notification,
                            onClick = onNotificationClick,
                            containerColor = colorsUI.sunset
                        )
                    }
                }
            }
        )
    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class,
        ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class
    )
    @Composable
    fun <T> FullScreenDialog(
        onDismissRequest: () -> Unit,
        itemList: List<T>,
        itemFilter: (T, String) -> Boolean,
        itemOnClick: (T) -> Unit,
        labelText: String,
        itemText: (T) -> String,
        openDialog: Boolean
    ) {
        val inputText = remember { mutableStateOf("") }
        var filteredItemList by remember { mutableStateOf(itemList) }
        val selectedItem = remember { mutableStateOf<T?>(null) }
        val keyboardController = LocalSoftwareKeyboardController.current

        DisposableEffect(inputText.value) {
            filteredItemList = itemList.filter { item ->
                itemFilter(item, inputText.value)
            }
            onDispose { }
        }

        if (openDialog) {
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(
                    dismissOnClickOutside = false,
                    dismissOnBackPress = false
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            TextField(
                                textStyle = TextStyle(
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                ),
                                readOnly = false,
                                value = inputText.value,
                                onValueChange = {
                                    inputText.value = it
                                },

                                label = {
                                    Text(
                                        labelText,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Icon"
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear button",
                                        modifier = Modifier.clickable {
                                            onDismissRequest()
                                        }
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                isError = false,
                                singleLine = true, // Dodane singleLine
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                    }
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ) // Dodane KeyboardOptions
                            )

                            LazyColumn(modifier = Modifier.heightIn(min = 0.dp, max = 500.dp)) {
                                items(filteredItemList.size) { index ->
                                    val item = filteredItemList[index]
                                    val isSelected = selectedItem.value == item
                                    AnimatedContent(
                                        targetState = isSelected,
                                        transitionSpec = {
                                            fadeIn() togetherWith fadeOut()
                                        }, label = ""
                                    ) { targetState ->
                                        val visibility = if (targetState) {
                                            Modifier.alpha(1f)
                                        } else {
                                            Modifier.alpha(0.5f)
                                        }
                                        Text(
                                            text = itemText(item),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 18.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedItem.value = item
                                                    itemOnClick(item)
                                                    onDismissRequest()
                                                }
                                                .padding(vertical = 10.dp, horizontal = 16.dp)
                                                .then(visibility)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun FullScreenClientsList(
        showDialog: MutableState<Boolean>,
        expanded: MutableState<Boolean>,
        viewModel: CustomersModelView,
    ) {
        val clientList by viewModel.customersLists.observeAsState(emptyList())

        if (expanded.value) {
            clientList?.let {
                FullScreenDialog(
                    onDismissRequest = { expanded.value = false },
                    itemList = it.map { it.fullName },
                    itemOnClick = { client ->
                        showDialog.value = false

                        expanded.value = false
                    },
                    itemFilter = { item: String?, query: String ->
                        item!!.contains(query, ignoreCase = true)
                    },
                    labelText = "",
                    itemText = { item -> item },
                    openDialog = showDialog.value
                )
            }
        }
    }


    @Composable
    fun FullScreenLogisticDialogSelector(
        labelText: AnnotatedString,
        selectedItem: String,
        onItemChange: (String) -> Unit,
        isEditable: Boolean,
        items: List<String>,
        onItemSelected: (String) -> Unit,
        dialogShouldOpen: MutableState<Boolean> = mutableStateOf(false),
        itemFilter: (String, String) -> Boolean = { item, query ->
            item.contains(
                query,
                ignoreCase = true
            )
        },
        showError: Boolean = false,
        modifier: Modifier = Modifier,
        leadingIcon: @Composable (() -> Unit)? = null, // new argument for the icon
    ) {
        var selectedItemState by remember(selectedItem) { mutableStateOf(selectedItem) }

        val interactionSource = remember { MutableInteractionSource() }

        LaunchedEffect(selectedItemState) {
            onItemChange(selectedItemState)
        }

        OutlinedTextField(
            value = selectedItemState,
            onValueChange = onItemChange,
            label = {
                Text(
                    labelText, color = MaterialTheme.colorScheme.primary
                )
            },
            readOnly = true,
            enabled = false,
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isEditable) FontWeight.Bold else FontWeight.Normal
            ),
            isError = showError,
            interactionSource = interactionSource,
            leadingIcon = {
                if (leadingIcon != null) {
                    leadingIcon()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    dialogShouldOpen.value = true
                },
            shape = RoundedCornerShape(8.dp)
        )


        if (dialogShouldOpen.value) {
            FullScreenDialog(
                onDismissRequest = { dialogShouldOpen.value = false },
                itemList = items,
                itemOnClick = { item ->
                    selectedItemState = item
                    onItemSelected(item)
                    dialogShouldOpen.value = false
                },
                itemFilter = itemFilter,
                labelText = labelText.toString(),
                itemText = { item -> item },
                openDialog = dialogShouldOpen.value
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    fun showDatePickerDialog(
        context: Context,
        dateSetListener: (String) -> Unit,
    ) {
        val current = LocalDate.now()
        val datePickerDialog = android.app.DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val formattedDate = "${dayOfMonth.toString().padStart(2, '0')}.${
                (month + 1).toString().padStart(2, '0')
            }.$year"
            dateSetListener(formattedDate)

            // Aktualizujemy dni tygodnia w viewModel
            val weekDaysAsInts = appFunctions.getCurrentWeekDays(formattedDate)

        }, current.year, current.monthValue - 1, current.dayOfMonth)

        datePickerDialog.show()
    }

    @Composable
    fun DeleteDialog(
        objectName: String,
        labelName: String = "Czy chcesz usunąć",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(alignment = Alignment.CenterHorizontally),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(65.dp)
                                        .align(alignment = Alignment.Center),
                                )
                            }

                            Text(
                                text = labelName,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .align(alignment = Alignment.CenterHorizontally),
                            ) {
                                Text(
                                    text = objectName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(alignment = Alignment.Center),
                                    softWrap = true,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Nie",
                                    onClick = { onDismiss() },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogDeleteButton(
                                    text = "Tak",
                                    onClick = { onConfirm() },
                                    modifier = m,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DeleteAppointmentStrefaDialog(
        appointment: Appointment?,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StrefaModalPanel {
                        StrefaModalIconFrame(
                            variant = StrefaModalIconVariant.Danger,
                            icon = Icons.Outlined.Delete,
                            iconTint = colorsUI.amaranthPurple,
                        )
                        StrefaModalTitleText(
                            text = "Usunąć wizytę?",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        StrefaModalBodyText(
                            text = "Tej operacji nie można cofnąć.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        StrefaDialogOptionList(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            StrefaDialogInfoListItem(
                                label = "Klient",
                                value = appointment?.customer?.fullName.orEmpty(),
                            )
                            StrefaDialogInfoListItem(
                                label = "Data",
                                value = appointment?.date.orEmpty(),
                            )
                            StrefaDialogInfoListItem(
                                label = "Godziny",
                                value = listOf(
                                    appointment?.startTime.orEmpty(),
                                    appointment?.endTime.orEmpty(),
                                ).filter { it.isNotBlank() }.joinToString(" - "),
                            )
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Anuluj",
                                    onClick = onDismiss,
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogDeleteButton(
                                    onClick = onConfirm,
                                    modifier = m,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SendNotificationDialog(
        objectName: String,
        labelName: String = "Czy chcesz wyslac SMS?",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(alignment = Alignment.CenterHorizontally),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(65.dp)
                                        .align(alignment = Alignment.Center),
                                )
                            }

                            Text(
                                text = labelName,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .align(alignment = Alignment.CenterHorizontally),
                            ) {
                                Text(
                                    text = objectName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(alignment = Alignment.Center),
                                    softWrap = true,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    text = "Nie",
                                    onClick = { onDismiss() },
                                    modifier = m,
                                    style = StrefaDialogButtonStyle.Ghost,
                                )
                            },
                            second = { m ->
                                StrefaDialogNotifyButton(
                                    text = "Tak",
                                    onClick = { onConfirm() },
                                    modifier = m,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

}
