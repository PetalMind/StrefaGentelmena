package com.strefagentelmena.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.strefagentelmena.models.Customer
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.textModernTextFieldUI
import com.strefagentelmena.viewModel.CustomersModelView
import kotlinx.coroutines.launch

val screenCustomerView = CustomerView()

class CustomerView {

    @Composable
    fun CustomerListView(
        viewModel: CustomersModelView,
        navController: NavController,
    ) {
        val customerList by viewModel.customersLists.observeAsState(arrayListOf())
        val searchedCustomerList by viewModel.searchedCustomersLists.observeAsState(arrayListOf())
        val clientDialogState by viewModel.clientDialogState.observeAsState(false)
        val searchState by viewModel.searchState.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val deleteDialogState by viewModel.deleteDialogState.observeAsState(false)
        val selectedClient by viewModel.selectedCustomer.observeAsState(Customer())

        val context = LocalContext.current
        val searchText = remember { mutableStateOf("") }

        val showedList by rememberUpdatedState(
            if (searchState) searchedCustomerList else customerList
        )

        // Inicjalizacja stanu Scaffold
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            viewModel.clearMessage()
            viewModel.closeAllDialogs()
            viewModel.loadClients(context)
        }

        // Efekt wyzwalany, gdy wartość 'message' się zmienia
        LaunchedEffect(message) {
            if (message.isNotEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                    viewModel.clearMessage()
                }
            }
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = {
                buttonsUI.ExtendedFab(text = "Dodaj", icon = Icons.Default.Add, onClick = {
                    viewModel.selectedCustomer.value = null
                    viewModel.showAddCustomerDialog()
                })
            },
            floatingActionButtonPosition = FabPosition.End,
            topBar = {
                headersUI.AppBarWithBackArrow(title = "Klienci salonu", onBackPressed = {
                    navController.navigate("dashboard")
                }, compose = {
                    Box(
                        modifier = Modifier
                            .background(colorsUI.headersBlue, RoundedCornerShape(15.dp))
                            .clip(RoundedCornerShape(15.dp))
                            .padding(10.dp)
                            .clickable {
                                viewModel.setShowSearchState(!searchState)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                        )
                    }
                })
            },
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                if (searchState) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        textModernTextFieldUI.SearchTextField(
                            onSearchTextChange = {
                                searchText.value = it
                                viewModel.searchCustomers(it)
                            },
                            searchText = searchText,
                        )
                    }
                }

                if (showedList.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Lista klientów jest pusta",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    ClientLazyColumn(
                        customerList = showedList,
                        paddingValues = PaddingValues(8.dp, 4.dp),
                        onCustomerClick = { customer ->
                            viewModel.selectedCustomer.value = customer
                            viewModel.showAddCustomerDialog()
                        },
                        onEdit = {
                            viewModel.selectedCustomer.value = it
                            viewModel.showAddCustomerDialog()
                        },
                        onDelete = {
                            viewModel.selectedCustomer.value = it
                            viewModel.showDeleteDialog()
                        }
                    )
                }
            }
        }

        if (clientDialogState == true) {
            dialogsUI.OnAddOrEditCustomerDialog(
                onClose = { viewModel.closeAddClientDialog() },
                onAddCustomer = {
                    if (viewModel.validateAllFields()
                    ) {
                        viewModel.addCustomer(context)
                    }
                },
                viewModel = viewModel,
                onEditCustomer = {
                    viewModel.editCustomer(context)
                },
                onDeleteCustomer = {
                    viewModel.showDeleteDialog()
                }
            )
        }

        if (deleteDialogState == true) {
            dialogsUI.DeleteDialog(onDismiss = { viewModel.closeDeleteDialog() }, onConfirm = {
                selectedClient?.let { viewModel.deleteCustomer(context = context, customer = it) }
                viewModel.closeDeleteDialog()
                viewModel.closeAllDialogs()
            }, objectName = selectedClient?.fullName ?: "")
        }
    }

    @Composable
    fun ClientLazyColumn(
        customerList: List<Customer>,
        paddingValues: PaddingValues,
        onCustomerClick: (Customer) -> Unit,
        onDelete: (Customer) -> Unit,
        onEdit: (Customer) -> Unit
    ) {
        LazyColumn(contentPadding = paddingValues) {
            items(customerList, key = { it.id ?: 0 }) { customer ->
                cardUI.SwipeToDismissCustomerCard(customer = customer, onClick = {
                    onCustomerClick(customer)
                },
                    onDismiss = {
                        onDelete(it)
                    },
                    onEdit = { onEdit(it) })
            }
        }
    }
}
