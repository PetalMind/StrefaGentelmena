package com.strefagentelmena.uiComposable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.strefagentelmena.viewModel.CustomersModelView
import com.strefagentelmena.viewModel.DashboardModelView
import com.strefagentelmena.viewModel.ScheduleModelView

val selectorsUI = Selectors()

class Selectors {
    @Composable
    fun ClientSelector(
        viewModel: DashboardModelView,
        customersModelView: CustomersModelView,
        scheduleModelView: ScheduleModelView
    ) {
        val fetchedClientsList by viewModel.customersLists.observeAsState(null)
        val selectedClient by customersModelView.selectedCustomer.observeAsState(null)
        val dialogShoudOpen = remember { mutableStateOf(false) }


        val selectedClientName =
            remember { mutableStateOf(selectedClient?.fullName ?: "Wybierz klienta") }

        val labelText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Black)) {
                append("Klient ")
            }
        }

        dialogsUI.FullScreenLogisticDialogSelector(
            labelText = labelText,
            selectedItem = selectedClientName.value,
            onItemChange = {
                selectedClientName.value = it
            },
            isEditable = true,
            items = fetchedClientsList?.map { it.fullName ?: "" } ?: emptyList(),
            onItemSelected = { client ->
                val findClient = viewModel.findCustomerByName(client)
                if (findClient != null) {
                    customersModelView.selectedCustomer.value = findClient
                    selectedClientName.value = findClient.fullName!!
                }
            },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = "Person")
            },
            dialogShouldOpen = dialogShoudOpen,
        )
    }

}
