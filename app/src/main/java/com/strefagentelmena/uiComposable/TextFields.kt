package com.strefagentelmena.uiComposable

import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.R
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale

val textModernTextFieldUI = TextFields()

class TextFields {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
        modifier: Modifier = Modifier
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current ?: return

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
            isError = isError,
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions.copy(
                imeAction = ImeAction.Done
            ),
            interactionSource = interactionSource,
            leadingIcon = leadingIcon,
            textStyle = TextStyle.Default.copy(
                fontSize = 20.sp,
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        AnimatedVisibility(
            visible = isError && supportText != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = supportText ?: "",
                style = TextStyle(color = Color.Red),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    fun SearchTextField(searchText: MutableState<String>, onSearchTextChange: (String) -> Unit) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            shape = RoundedCornerShape(8.dp),
            value = searchText.value,
            onValueChange = onSearchTextChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search, contentDescription = "Search Icon"
                )
            },
            trailingIcon = {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        modifier = Modifier.size(24.dp),
                        contentDescription = "Clear Icon"
                    )
                }
            },
            label = {
                Text(
                    text = "Wyszukaj...", color = MaterialTheme.colorScheme.primary
                )
            },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        // Request focus and show the keyboard
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
            keyboardController?.show()
            searchText.value = ""
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DateOutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
    ) {
        val context = LocalContext.current
        val interactionSource = remember { MutableInteractionSource() }
        val showDatePicker = remember { mutableStateOf(false) }

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            readOnly = true,
            enabled = false,
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange, contentDescription = "Search Icon"
                )
            },
            label = {
                Text(
                    "Data wizyty", color = MaterialTheme.colorScheme.primary
                )
            },
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDatePicker.value = true
                }
        )

        if (showDatePicker.value) {
            showDatePickerDialog(
                context = context,
                onDateSet = {
                    onValueChange(it)
                    showDatePicker.value = false
                },
                onCancel = {
                    showDatePicker.value = false
                }
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TimeOutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String
    ) {
        val showTimePicker = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val interactionSource = remember { MutableInteractionSource() }


        OutlinedTextField(
            shape = RoundedCornerShape(8.dp),
            value = value,
            onValueChange = { onValueChange(it) },
            singleLine = true,
            label = {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            readOnly = true,
            enabled = false,
            textStyle = TextStyle(
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock),
                    contentDescription = "Search Icon"
                )
            },
            modifier = modifier
                .clickable {
                    showTimePicker.value = true
                },
            interactionSource = interactionSource
        )


        if (showTimePicker.value) {
            showTimePickerDialog(context = context, startTime = value, onTimeSet = {
                onValueChange(it)
                showTimePicker.value = false
            }, onCancel = {
                showTimePicker.value = false
            })
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    fun showDatePickerDialog(context: Context, onDateSet: (String) -> Unit, onCancel: () -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val picker = android.app.DatePickerDialog(context, { _, y, m, d ->
            val formattedDate = String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)
            onDateSet(formattedDate)
        }, year, month, day)

        picker.setOnCancelListener { onCancel() }
        picker.show()
    }

    private fun showTimePickerDialog(
        context: Context,
        onTimeSet: (String) -> Unit,
        startTime: String = "",
        onCancel: () -> Unit
    ) {
        val currentTime = if (startTime.isNotEmpty()) {
            LocalTime.parse(startTime)
        } else {
            LocalTime.now()
        }

        val timePickerDialog = TimePickerDialog(
            context, { _, hourOfDay, minute ->
                val formattedTime =
                    String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                onTimeSet(formattedTime)
            }, currentTime.hour, currentTime.minute, true
        )

        timePickerDialog.show()
        timePickerDialog.setOnCancelListener { onCancel() }
    }
}