package com.strefagentelmena.uiComposable

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.strefagentelmena.R
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.LocalDate

val dialogsUI = Dialogs()

class Dialogs {

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun OnAddOrEditCustomerDialog(
        viewModel: CustomersModelView,
        onClose: () -> Unit,
        onAddCustomer: () -> Unit,
        onEditCustomer: () -> Unit,
        onDeleteCustomer: () -> Unit
    ) {
        val selectedCustomer by viewModel.selectedCustomer.observeAsState(null)
        val headerText =
            if (selectedCustomer == null) "Nowy klient" else "Edytuj klienta"

        var firstName by remember { mutableStateOf(selectedCustomer?.firstName ?: "") }
        var lastName by remember { mutableStateOf(selectedCustomer?.lastName ?: "") }
        var phoneNumber by remember { mutableStateOf(selectedCustomer?.phoneNumber ?: "") }

        //errors fromViewModel
        val firstNameError by viewModel.firstNameError.observeAsState("")
        val lastNameError by viewModel.lastNameError.observeAsState("")
        val phoneNumberError by viewModel.phoneNumberError.observeAsState("")

        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        LaunchedEffect(selectedCustomer) {
            if (selectedCustomer != null) {
                viewModel.setSelectedCustomerData()
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
                                    containerColor = colorsUI.carmine
                                )
                            }
                        })

                    Column(modifier = Modifier.padding(16.dp)) {
                        textModernTextFieldUI.ModernTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                viewModel.validateFirstName(it)
                                viewModel.setCustomerName(it)
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
                            autoFocus = true,
                        )

                        textModernTextFieldUI.ModernTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                viewModel.validateLastName(it)
                                viewModel.setCustomerLastName(it)
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
                            autoFocus = false
                        )

                        textModernTextFieldUI.ModernTextField(
                            value = phoneNumber,
                            onValueChange = {
                                if (it.length <= 9) {
                                    phoneNumber = it
                                }
                                viewModel.validatePhoneNumber(it)
                                viewModel.setCustomerPhoneNumber(it)
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
                            autoFocus = false
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
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun OnAddOrEditSchedule(
        viewModel: ScheduleModelView,
    ) {

        val isNewState by viewModel.isNewAppointment.observeAsState(false)
        val deleteDialogState by viewModel.deleteDialog.observeAsState(false)
        val selectedAppointment by viewModel.selectedAppointment.observeAsState(null)
        val customersList by viewModel.customersList.observeAsState(emptyList())
        val selectedClient by viewModel.selectedClient.observeAsState(null)
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState(if (isNewState) selectedAppointment?.date else "")
        val selectedTime by viewModel.selectedAppointmentTime.observeAsState(if (isNewState) selectedAppointment?.startTime else "")

        val title = if (isNewState) "Dodaj" else "Edytujv"
        val context = LocalContext.current

        Dialog(
            onDismissRequest = { viewModel.hideApoimentDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Row {
                        headersUI.AppBarWithBackArrow(
                            title = title,
                            onClick = {},
                            onBackPressed = { viewModel.hideApoimentDialog() },
                            compose = {
                                if (selectedAppointment != null) {
                                        buttonsUI.HeaderIconButton(
                                            icon = R.drawable.ic_delete,
                                            onClick = {
                                                viewModel.showDeleteDialog()
                                            },
                                            containerColor = colorsUI.carmine
                                        )

                                        buttonsUI.HeaderIconButton(
                                            icon = R.drawable.ic_notification,
                                            onClick = {

                                            },
                                            containerColor = colorsUI.sunset
                                        )
                                }
                            })
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        formUI.AppointmentForm(
                            viewModel = viewModel,
                            onSave = {
                                if (selectedClient != null && selectedDate?.isNotEmpty() == true && selectedTime?.isNotEmpty() == true) {
                                    if (isNewState) {
                                        viewModel.createNewApointment(
                                            isNew = isNewState,
                                            selectedClient = selectedClient,
                                            date = selectedDate ?: return@AppointmentForm,
                                            startTime = selectedTime
                                                ?: return@AppointmentForm,
                                            context
                                        )
                                    } else {
                                        viewModel.editAppointment(
                                            context,
                                            customersList
                                                ?: return@AppointmentForm,
                                        )

                                        viewModel.hideApoimentDialog()
                                    }
                                }
                            },
                            onCancel = { viewModel.hideApoimentDialog() },
                        )
                    }
                }
            }
        }
        if (deleteDialogState) {
            ConfirmDeleteDialog(
                onDismiss = { viewModel.hideDeleteDialog() },
                text = "Czy na pewno chcesz usunąć wizytę"
            ) {
                viewModel.removeAppointment(selectedAppointment?.id ?: 0, context)
                viewModel.hideApoimentDialog()
            }
        }
    }

    @Composable
    fun ConfirmDeleteDialog(
        onDismiss: () -> Unit,
        text: String,
        onConfirm: () -> Unit,
    ) {
        AlertDialog(
            title = { Text("Potwierdzenie") },
            text = { Text(text) },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("Tak")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Nie")
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
            FullScreenDialog(
                onDismissRequest = { expanded.value = false },
                itemList = clientList.map { it.fullName },
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


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    fun FullScreenLogisticDialogSelector(
        labelText: AnnotatedString,
        selectedItem: String,
        onItemChange: (String) -> Unit,
        isEditable: Boolean,
        items: List<String>,
        onItemSelected: (String) -> Unit,
        dialogShouldOpen: MutableState<Boolean>,
        itemFilter: (String, String) -> Boolean = { item, query ->
            item.contains(
                query,
                ignoreCase = true
            )
        },
        showError: Boolean = false,
        modifier: Modifier = Modifier,
        leadingIcon: @Composable (() -> Unit)? = null,  // nowy argument dla ikony
    ) {
        var selectedItemState by remember(selectedItem) { mutableStateOf(selectedItem) }

        val interactionSource = remember {
            object : MutableInteractionSource {
                override val interactions = MutableSharedFlow<Interaction>(
                    extraBufferCapacity = 16,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST,
                )

                override suspend fun emit(interaction: Interaction) {
                    if (interaction is PressInteraction.Release) {
                        if (isEditable) {
                            dialogShouldOpen.value = !dialogShouldOpen.value
                        }
                    }

                    interactions.emit(interaction)
                }

                override fun tryEmit(interaction: Interaction): Boolean {
                    return interactions.tryEmit(interaction)
                }
            }
        }

        LaunchedEffect(selectedItemState) {
            onItemChange(selectedItemState)
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (isEditable) {
                            dialogShouldOpen.value = !dialogShouldOpen.value
                        }
                    }
                )
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),  // Brak zaokrąglenia
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                TextField(
                    value = selectedItemState,
                    onValueChange = onItemChange,
                    label = {
                        Text(
                            labelText,
                            color = if (isEditable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    readOnly = true,
                    enabled = isEditable,
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
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = RoundedCornerShape(0.dp)  // Brak zaokrąglenia
                )
            }
        }

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


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    fun showDatePickerDialog(
        context: Context,
        dateSetListener: (String) -> Unit,
        viewModel: ScheduleModelView,
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
                        text = "Czy chcesz usunąć",
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

}
