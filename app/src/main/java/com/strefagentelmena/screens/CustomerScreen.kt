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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.strefagentelmena.navigation.NavPendingActions
import com.strefagentelmena.navigation.popBackStackOrNavigateToMain
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.R
import com.strefagentelmena.models.Customer
import com.strefagentelmena.uiComposable.buttonsUI
import com.strefagentelmena.uiComposable.cardUI
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.uiComposable.StrefaDialogButton
import com.strefagentelmena.uiComposable.StrefaDialogButtonRow
import com.strefagentelmena.uiComposable.StrefaDialogButtonStyle
import com.strefagentelmena.uiComposable.StrefaDialogDeleteButton
import com.strefagentelmena.uiComposable.StrefaDialogFloatingBar
import com.strefagentelmena.uiComposable.StrefaModalBodyText
import com.strefagentelmena.uiComposable.StrefaModalIconFrame
import com.strefagentelmena.uiComposable.StrefaModalIconVariant
import com.strefagentelmena.uiComposable.StrefaModalPanel
import com.strefagentelmena.uiComposable.StrefaModalTitleText
import com.strefagentelmena.uiComposable.dialogsUI
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.uiComposable.reusableScreen
import com.strefagentelmena.uiComposable.textModernTextFieldUI
import com.strefagentelmena.ui.theme.SalonBg2
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonIconBoxBorder
import com.strefagentelmena.ui.theme.SalonRed
import com.strefagentelmena.ui.theme.SalonText
import com.strefagentelmena.viewModel.CustomersModelView
import kotlinx.coroutines.launch

val screenCustomerView = CustomerScreen()

class CustomerScreen {

    @Composable
    fun CustomersOverview(
        viewModel: CustomersModelView,
        navController: NavController,
    ) {
        val customerList by viewModel.customersLists.observeAsState(arrayListOf())
        val catalogReady by viewModel.customersCatalogReady.observeAsState(false)
        val searchedCustomerList by viewModel.searchedCustomersLists.observeAsState(arrayListOf())
        val clientDialogState by viewModel.clientDialogState.observeAsState(false)
        val searchState by viewModel.searchState.observeAsState(false)
        val message by viewModel.messages.observeAsState("")
        val deleteDialogState by viewModel.deleteDialogState.observeAsState(false)
        val selectedClient by viewModel.selectedCustomer.observeAsState(null)

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
            viewModel.loadClients(FirebaseDatabase.getInstance())
        }

        LaunchedEffect(catalogReady, customerList) {
            if (!catalogReady) return@LaunchedEffect
            val id = NavPendingActions.peekCustomerEditId() ?: return@LaunchedEffect
            if (id <= 0) {
                NavPendingActions.consumeCustomerEditId()
                return@LaunchedEffect
            }
            val c = customerList.firstOrNull { it.id == id }
            NavPendingActions.consumeCustomerEditId()
            if (c != null) {
                viewModel.selectedCustomer.value = c
                viewModel.setSelectedCustomerData()
                viewModel.showAddCustomerDialog()
            } else {
                viewModel.messages.value = "Nie znaleziono klienta na liście."
            }
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
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .border(0.5.dp, SalonIconBoxBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = SalonBg2,
                        contentColor = SalonText,
                        actionColor = SalonGold,
                    )
                }
            },
            floatingActionButton = {
                buttonsUI.LargeFloatingActionButton(icon = Icons.Default.Add, onClick = {
                    viewModel.setSelectedCustomer(null)
                    viewModel.showAddCustomerDialog()
                })
            },
            floatingActionButtonPosition = FabPosition.End,
            topBar = {
                headersUI.AppBarWithBackArrow(title = "Klienci salonu", onBackPressed = {
                    navController.popBackStackOrNavigateToMain()
                }, compose = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Box(modifier = Modifier
                            .background(
                                colorsUI.headersBlue, RoundedCornerShape(15.dp)
                            )
                            .clip(RoundedCornerShape(15.dp))
                            .padding(10.dp)
                            .clickable {
                                viewModel.setShowSearchState(!searchState)
                            }) {
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

                if (showedList.value?.isEmpty() == true) {
                    reusableScreen.EmptyScreen("Lista klientów jest pusta")
                } else {
                    showedList.value?.let {
                        ClientLazyColumn(customerList = it,
                            paddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                                viewModel.deleteCustomer(FirebaseDatabase.getInstance())
                                viewModel.changeDeleteDialogState()
                                viewModel.loadClients(FirebaseDatabase.getInstance())
                            })
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = clientDialogState, enter = slideInVertically(
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearEasing,
                ),
            ) + expandIn(), exit = slideOutVertically(
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearEasing,
                ),
            ) + shrinkOut()
        ) {
            dialogsUI.OnAddOrEditCustomerDialog(onClose = { viewModel.closeCustomerDialog() },
                onAddCustomer = {
                    if (viewModel.checkFormValidity()) {
                        viewModel.addCustomer(FirebaseDatabase.getInstance())
                    }
                },
                viewModel = viewModel,
                onEditCustomer = {
                    if (viewModel.checkFormValidity()) {
                        viewModel.editCustomer(FirebaseDatabase.getInstance())
                    }
                },
                onDeleteCustomer = {
                    viewModel.changeDeleteDialogState()
                })
        }

        AnimatedVisibility(
            visible = deleteDialogState,
            enter = fadeIn() + expandIn(),
        ) {
            DeleteCustomerStrefaDialog(
                customerName = selectedClient?.fullName?.ifBlank { "—" } ?: "—",
                onDismiss = { viewModel.changeDeleteDialogState() },
                onConfirm = {
                    selectedClient?.let {
                        viewModel.deleteCustomer(FirebaseDatabase.getInstance())
                    }
                    viewModel.changeDeleteDialogState()
                },
            )
        }
    }

    @Composable
    private fun DeleteCustomerStrefaDialog(
        customerName: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit,
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
                            iconTint = SalonRed,
                        )
                        StrefaModalTitleText("Usunąć klienta?")
                        StrefaModalBodyText(
                            text = "Klient $customerName zostanie ukryty wraz z jego wizytami. Dane pozostaną zapisane w Firebase.",
                        )
                    }
                    StrefaDialogFloatingBar(
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        StrefaDialogButtonRow(
                            first = { m ->
                                StrefaDialogButton(
                                    "Anuluj",
                                    onDismiss,
                                    m,
                                    StrefaDialogButtonStyle.Ghost,
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
    fun SortMenu(viewModel: CustomersModelView) {
        var expanded by remember { mutableStateOf(false) }

        Box(modifier = Modifier.padding(end = 8.dp)) {
            Box(modifier = Modifier
                .background(colorsUI.headersBlue, RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(15.dp))
                .padding(10.dp)
                .clickable {
                    expanded = !expanded
                }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sort),
                    contentDescription = "Sortuj listę",
                )
            }
        }


        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = {
                viewModel.sortClientsByName()
                expanded = false
            }, text = { Text(text = "Sortuj alfabetycznie") }, leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sort_by_alpha),
                    contentDescription = "Sort Options"
                )
            })

            DropdownMenuItem(onClick = {
                viewModel.sortClientsByDate()
                expanded = false
            }, text = { Text(text = "Ostatnia wizyta: od najnowszej") }, leadingIcon = {
                Icon(Icons.Filled.DateRange, contentDescription = "Sort Options")
            })

            DropdownMenuItem(onClick = {
                viewModel.sortClientsByDateDesc()
                expanded = false
            }, text = { Text(text = "Ostatnia wizyta: od najdawniejszej") }, leadingIcon = {
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
            items(customerList) { customer ->
                cardUI.SwipeToDismissCustomerCard(customer = customer, onClick = {
                    onCustomerClick(customer)
                }, onDismiss = {
                    onDelete(it)
                }, onEdit = { onEdit(it) })
            }
        }
    }
}
