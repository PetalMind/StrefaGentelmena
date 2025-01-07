package com.strefagentelmena.uiComposable

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.R
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val dialogsUI = Dialogs()

class Dialogs {

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
        val note by viewModel.customerNote.observeAsState("")

        //errors fromViewModel
        val firstNameError by viewModel.firstNameError.observeAsState("")
        val lastNameError by viewModel.lastNameError.observeAsState("")
        val phoneNumberError by viewModel.phoneNumberError.observeAsState("")

        val headerText =
            if (selectedCustomer == null) "Nowy klient" else "Edytuj klienta"

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(selectedCustomer) {
            if (selectedCustomer != null) {
                viewModel.setSelectedCustomerData()
            } else {
                viewModel.clearSelectedClientAndData()
            }
        }

        Dialog(
            onDismissRequest = { onClose() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                                    containerColor = colorsUI.carmine
                                )
                            }
                        })

                    Column(modifier = Modifier.padding(16.dp)) {
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
                            value = note,
                            onValueChange = {
                                viewModel.setSelectedCustomerNote(it)
                            },
                            label = "Notatka",
                            isError = false,
                            supportText = null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBox,
                                    contentDescription = "Person",
                                )
                            },
                            modifier = Modifier.height(150.dp)
                        )

                        buttonsUI.ButtonsRow(
                            cancelText = "Anuluj",
                            confirmText = "Zapisz",
                            onClick = {
                                if (selectedCustomer == null) {
                                    onAddCustomer()
                                } else {
                                    onEditCustomer()
                                }
                            },
                            onDismiss = {
                                onClose()
                            },
                            containerColor = colorsUI.green,
                            modifier = Modifier.padding(top = 16.dp),
                            buttonEnabled = true
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
        val selectedClient by viewModel.selectedClient.observeAsState()
        val selectedWorker by viewModel.selectedEmployee.observeAsState()

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val sendNotification = remember { mutableStateOf(false) }
        val title = if (isNewState) "Dodaj" else "Edytuj"
        val context = LocalContext.current

        val selectedDate = remember(selectedAppointment, isNewState) {
            selectedAppointment?.date?.takeIf { it.isNotBlank() }?.let {
                LocalDate.parse(it, formatter)
            } ?: LocalDate.now()
        }

        val selectedTime = remember(selectedAppointment, isNewState) {
            selectedAppointment?.startTime?.takeIf { it.isNotBlank() }?.let {
                LocalTime.parse(it, timeFormatter)
            } ?: LocalTime.now()
        }


        // Obsługa powiadomień
        LaunchedEffect(selectedAppointment, isNewState) {
            selectedAppointment?.let {
                if (it.id != 0 && selectedDate != LocalDate.parse(it.date, formatter) ||
                    selectedTime != LocalTime.parse(it.startTime, timeFormatter)
                ) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Nagłówek z przyciskami
                    AppHeader(
                        title = title,
                        selectedAppointment = selectedAppointment,
                        onDeleteClick = { viewModel.setDeleteDialogState() },
                        onNotificationClick = { viewModel.changeNotificationDialogState() },
                        onBackClick = { viewModel.changeAppointmentDialogState() }
                    )

                    // Formularz edycji/dodawania wizyty
                    Column(modifier = Modifier.padding(16.dp)) {
                        formUI.AppointmentForm(
                            viewModel = viewModel,
                            onSave = {
                                if (isNewState) {
                                    viewModel.createNewAppointment(isNewState)
                                } else {
                                    viewModel.editAppointment(
                                        FirebaseDatabase.getInstance(),
                                        sendNotification.value
                                    )
                                    viewModel.changeAppointmentDialogState()

                                }
                            },
                            onCancel = { viewModel.changeAppointmentDialogState() },
                        )
                    }
                }
            }
        }

        // Dialog potwierdzający usunięcie
        if (deleteDialogState) {
            DeleteDialog(
                objectName = "${selectedAppointment?.customer?.fullName ?: ""}\n${selectedAppointment?.date ?: ""}",
                onConfirm = {
                    viewModel.removeAppointment(
                        id = selectedAppointment?.id ?: 0,
                    )
                    viewModel.closeAllDialog()
                },
                onDismiss = { viewModel.setDeleteDialogState() },
                labelName = "Czy chcesz usunąć wizytę?"
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
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(alignment = Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .size(65.dp)
                                .align(alignment = Alignment.Center)
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
                            .align(alignment = Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = objectName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(alignment = Alignment.Center),
                            softWrap = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    buttonsUI.ButtonsRow(
                        onClick = { onConfirm() },
                        onDismiss = { onDismiss() },
                        confirmText = "Tak",
                        cancelText = "Nie"
                    )
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
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(alignment = Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier
                                .size(65.dp)
                                .align(alignment = Alignment.Center)
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
                            .align(alignment = Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = objectName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(alignment = Alignment.Center),
                            softWrap = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    buttonsUI.ButtonsRow(
                        onClick = { onConfirm() },
                        onDismiss = { onDismiss() },
                        confirmText = "Tak",
                        cancelText = "Nie",
                        containerColor = colorsUI.green
                    )
                }
            }
        }
    }

}
