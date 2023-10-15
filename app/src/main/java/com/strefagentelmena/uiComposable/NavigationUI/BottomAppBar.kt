package com.strefagentelmena.uiComposable.NavigationUI

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.strefagentelmena.models.composableModels.NavItem

val navUI = MaterialBottomAppBar()

class MaterialBottomAppBar {

    @Composable
    fun Material3BottomAppBar(
        onAddCustomerClick: () -> Unit,
    ) {
        BottomAppBar(
            actions = {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Check, contentDescription = "Localized description")
                }
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Localized description",
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onAddCustomerClick()},
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                ) {
                    Icon(Icons.Filled.Add, "Localized description")
                }
            }
        )
    }


}
