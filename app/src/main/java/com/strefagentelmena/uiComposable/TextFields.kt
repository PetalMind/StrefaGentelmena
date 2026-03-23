package com.strefagentelmena.uiComposable

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.strefagentelmena.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Promień zgodny z mockupem HTML (search-box, karty ~12–16px). */
private val StrefaTextFieldShape = RoundedCornerShape(12.dp)

val textModernTextFieldUI = TextFields()

@Composable
private fun strefaOutlinedFieldColors(
    isError: Boolean = false,
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorCursorColor = MaterialTheme.colorScheme.error,
    focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTrailingIconColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedSupportingTextColor = MaterialTheme.colorScheme.error,
    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,
)

class TextFields {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModernTextField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        isError: Boolean = false,
        supportText: String? = null,
        keyboardActions: KeyboardActions = KeyboardActions(),
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        leadingIcon: @Composable (() -> Unit)? = null,
        modifier: Modifier = Modifier,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val mergedKeyboard = keyboardOptions.copy(imeAction = ImeAction.Done)

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            isError = isError,
            supportingText = if (isError && supportText != null) {
                { Text(supportText, style = MaterialTheme.typography.bodySmall) }
            } else null,
            keyboardActions = keyboardActions,
            keyboardOptions = mergedKeyboard,
            interactionSource = interactionSource,
            leadingIcon = leadingIcon,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = StrefaTextFieldShape,
            colors = strefaOutlinedFieldColors(isError = isError),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun SearchTextField(
        searchText: MutableState<String>,
        onSearchTextChange: (String) -> Unit,
    ) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            shape = StrefaTextFieldShape,
            value = searchText.value,
            onValueChange = onSearchTextChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = "Wyszukaj…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Szukaj",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (searchText.value.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchTextChange("") },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Wyczyść",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            colors = strefaOutlinedFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() },
            ),
        )

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DateOutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String = "Data wizyty",
    ) {
        val context = LocalContext.current
        val interactionSource = remember { MutableInteractionSource() }
        val openPickerInteraction = remember { MutableInteractionSource() }
        val showDatePicker = remember { mutableStateOf(false) }

        LaunchedEffect(showDatePicker.value) {
            if (!showDatePicker.value) return@LaunchedEffect
            showDatePickerDialog(
                context = context,
                currentDateDisplay = value,
                onDateSet = {
                    onValueChange(it)
                    showDatePicker.value = false
                },
                onCancel = {
                    showDatePicker.value = false
                },
            )
        }

        Box(modifier = modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                shape = StrefaTextFieldShape,
                readOnly = true,
                enabled = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Wybierz datę",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                interactionSource = interactionSource,
                colors = strefaOutlinedFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = openPickerInteraction,
                        indication = null,
                    ) { showDatePicker.value = true },
            )
        }

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TimeOutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String,
    ) {
        val showTimePicker = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val interactionSource = remember { MutableInteractionSource() }
        val openPickerInteraction = remember { MutableInteractionSource() }

        LaunchedEffect(showTimePicker.value) {
            if (!showTimePicker.value) return@LaunchedEffect
            showTimePickerDialog(
                context = context,
                startTime = value,
                onTimeSet = {
                    onValueChange(it)
                    showTimePicker.value = false
                },
                onCancel = {
                    showTimePicker.value = false
                },
            )
        }

        Box(modifier = modifier.fillMaxWidth()) {
            OutlinedTextField(
                shape = StrefaTextFieldShape,
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                readOnly = true,
                enabled = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = "Wybierz godzinę",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = strefaOutlinedFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = openPickerInteraction,
                        indication = null,
                    ) { showTimePicker.value = true },
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun showDatePickerDialog(
        context: Context,
        currentDateDisplay: String,
        onDateSet: (String) -> Unit,
        onCancel: () -> Unit,
    ) {
        val plDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val initial = runCatching {
            val trimmed = currentDateDisplay.trim()
            if (trimmed.isEmpty() || trimmed == "null") null
            else LocalDate.parse(trimmed, plDate)
        }.getOrNull() ?: LocalDate.now()

        val picker = android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val formattedDate =
                    String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)
                onDateSet(formattedDate)
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        )

        picker.setOnCancelListener { onCancel() }
        picker.show()
    }

    private fun parseTimeForPickerOrNow(startTime: String): LocalTime {
        val t = startTime.trim()
        if (t.isEmpty()) return LocalTime.now()
        val flexible = DateTimeFormatter.ofPattern("H:m")
        return runCatching { LocalTime.parse(t, flexible) }
            .recoverCatching { LocalTime.parse(t, DateTimeFormatter.ofPattern("HH:mm")) }
            .getOrElse { LocalTime.now() }
    }

    private fun showTimePickerDialog(
        context: Context,
        onTimeSet: (String) -> Unit,
        startTime: String = "",
        onCancel: () -> Unit,
    ) {
        val currentTime = parseTimeForPickerOrNow(startTime)

        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formattedTime =
                    String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                onTimeSet(formattedTime)
            },
            currentTime.hour,
            currentTime.minute,
            true,
        )

        timePickerDialog.show()
        timePickerDialog.setOnCancelListener { onCancel() }
    }
}
