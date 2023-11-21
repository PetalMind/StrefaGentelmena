package com.strefagentelmena.uiComposable

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
        autoFocus: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        var isFocused by remember { mutableStateOf(false) }
        val interactionSource = remember { MutableInteractionSource() }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current ?: return
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current

        LaunchedEffect(autoFocus) {
            if (autoFocus) {
                focusRequester.requestFocus()
                softwareKeyboardController.show()
            }
        }

        Surface(
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.padding(vertical = 4.dp)
        ) {
            Column {
                TextField(value = value,
                    onValueChange = { onValueChange(it) },
                    label = { Text(label) },
                    isError = isError,
                    keyboardActions = keyboardActions,
                    keyboardOptions = keyboardOptions.copy(
                        imeAction = ImeAction.Done
                    ),
                    interactionSource = interactionSource,
                    leadingIcon = leadingIcon,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = colorsUI.grey,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    textStyle = TextStyle.Default.copy(
                        fontSize = 20.sp,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
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
        }
    }


    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    fun SearchTextField(searchText: MutableState<String>, onSearchTextChange: (String) -> Unit) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp, horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.surface,
                )
            )
        }

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
        onFocusLost: (Boolean) -> Unit,
    ) {
        val context = LocalContext.current
        val focusRequester = remember { FocusRequester() }
        Surface(
            shape = RoundedCornerShape(8.dp),  // Brak zaokrąglenia
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            TextField(value = value,
                onValueChange = { onValueChange(it) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.surface,
                ),
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DateRange, contentDescription = "Search Icon"
                    )
                },
                label = {
                    Text(
                        "Data wizyty", color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            showDatePickerDialog(context) { date ->
                                onValueChange(date.toString())
                            }
                        }
                        onFocusLost(state.isFocused)
                    })
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TimeOutlinedTextField(
        value: String,
        onValueChange: (String) -> Unit,
        onFocusLost: (Boolean) -> Unit,
    ) {
        val context = LocalContext.current
        val focusRequester = remember { FocusRequester() }
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            shape = RoundedCornerShape(8.dp),  // Brak zaokrąglenia
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            TextField(value = value,
                onValueChange = { onValueChange(it) },
                singleLine = true,
                label = {
                    Text(
                        "Godzina wizyty", color = MaterialTheme.colorScheme.primary
                    )
                },
                readOnly = true,
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                interactionSource = interactionSource,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.surface,
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = "Search Icon"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            showTimePickerDialog(context) { time ->
                                onValueChange(time)
                            }
                        }
                        onFocusLost(state.isFocused)
                    })
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun showDatePickerDialog(context: Context, onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val picker = android.app.DatePickerDialog(context, { _, y, m, d ->
            val formattedDate = String.format(Locale.getDefault(), "%02d.%02d.%04d", d, m + 1, y)
            onDateSet(formattedDate)
        }, year, month, day)
        picker.show()
    }

    fun showTimePickerDialog(context: Context, onTimeSet: (String) -> Unit) {
        val currentTime = LocalTime.now()
        val timePickerDialog = TimePickerDialog(
            context, { _, hourOfDay, minute ->
                val formattedTime =
                    String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                onTimeSet(formattedTime)
            }, currentTime.hour, currentTime.minute, true
        )
        timePickerDialog.show()
    }
}
