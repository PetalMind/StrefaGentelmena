package com.strefagentelmena.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.strefagentelmena.R
import com.strefagentelmena.models.Customer
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.textModernTextFieldUI
import com.strefagentelmena.viewModel.CustomersModelView
import kotlinx.coroutines.launch

val screenCustomerView = CustomerScreen()

class CustomerScreen {

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
        val selectedClient by viewModel.selectedCustomer.observeAsState(null)

        val context = LocalContext.current
        val searchText = remember { mutableStateOf("") }

        val showedList = rememberUpdatedState(
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
                    viewModel.setSelectedCustomer(null)
                    viewModel.showAddCustomerDialog()
                })
            },
            floatingActionButtonPosition = FabPosition.End,
            topBar = {
                headersUI.AppBarWithBackArrow(
                    title = "Klienci salonu",
                    onBackPressed = {
                        navController.navigate("mainScreen")
                    }, compose = {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
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
                        }
                        SortMenu(viewModel)
                    })
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues),
            ) {
                AnimatedVisibility(visible = searchState) {
                    Box(
                        contentAlignment = Alignment.Center,
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

                if (showedList.value.isEmpty()) {
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
                        customerList = showedList.value,
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

        AnimatedVisibility(
            visible = clientDialogState,
            enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearEasing,
                ),
            ) + expandIn(),
            exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearEasing,
                ),
            ) + shrinkOut()
        ) {
            dialogsUI.OnAddOrEditCustomerDialog(
                onClose = { viewModel.closeCustomerDialog() },
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

        AnimatedVisibility(
            visible = deleteDialogState,
            enter = fadeIn() + expandIn(),
        ) {
            dialogsUI.DeleteDialog(onDismiss = { viewModel.closeDeleteDialog() }, onConfirm = {
                selectedClient?.let {
                    viewModel.deleteCustomer(context = context, customer = it)
                    viewModel.closeAllDialogs()
                }
            },
                objectName = selectedClient?.fullName ?: ""
            )
        }
    }

    @Composable
    fun SortMenu(viewModel: CustomersModelView) {
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Box(modifier = Modifier.padding(end = 8.dp)) {
            Box(
                modifier = Modifier
                    .background(colorsUI.headersBlue, RoundedCornerShape(15.dp))
                    .clip(RoundedCornerShape(15.dp))
                    .padding(10.dp)
                    .clickable {
                        expanded = !expanded
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sort),
                    contentDescription = "Search",
                )
            }
        }


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(onClick = {
                viewModel.sortClientsByName()
                expanded = false
            }, text = { Text(text = "Sortuj alfabetycznie") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sort_by_alpha),
                        contentDescription = "Sort Options"
                    )
                })

            DropdownMenuItem(
                onClick = {
                    viewModel.sortClientsByDate()
                    expanded = false
                },
                text = { Text(text = "Sortuj po dacie od najnowszych") },
                leadingIcon = {
                    Icon(Icons.Filled.DateRange, contentDescription = "Sort Options")
                })

            DropdownMenuItem(onClick = {
                viewModel.sortClientsByDateDesc()
                expanded = false
            }, text = { Text(text = "Sortuj po dacie od najstarszych") },
                leadingIcon = {
                    Icon(Icons.Filled.DateRange, contentDescription = "Sort Options")
                })
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
