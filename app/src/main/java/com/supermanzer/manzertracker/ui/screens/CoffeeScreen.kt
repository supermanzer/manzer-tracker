package com.supermanzer.manzertracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.supermanzer.manzertracker.ui.theme.ManzerTrackerTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supermanzer.manzertracker.ManzerTrackerApplication
import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.CoffeeBrew
import com.supermanzer.manzertracker.data.Roaster
import com.supermanzer.manzertracker.ui.viewmodels.CoffeeViewModel
import com.supermanzer.manzertracker.ui.viewmodels.CoffeeViewModelFactory
import com.supermanzer.manzertracker.ui.theme.CoffeeDarkGradient1
import com.supermanzer.manzertracker.ui.theme.CoffeeDarkGradient2
import com.supermanzer.manzertracker.ui.theme.CoffeeLightGradient1
import com.supermanzer.manzertracker.ui.theme.CoffeeLightGradient2

enum class CoffeeFormType {
    NONE, BREW, ROASTER, BAG, BREW_DETAIL, EDIT_BREW, EDIT_BAG, EDIT_ROASTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeScreen() {
    val context = LocalContext.current
    val database = (context.applicationContext as ManzerTrackerApplication).database
    val viewModel: CoffeeViewModel = viewModel(
        factory = CoffeeViewModelFactory(database.coffeeDao())
    )

    val brews by viewModel.allBrews.collectAsState()
    val bags by viewModel.allBags.collectAsState()
    val roasters by viewModel.allRoasters.collectAsState()

    var activeForm by remember { mutableStateOf(CoffeeFormType.NONE) }
    var selectedBrew by remember { mutableStateOf<CoffeeBrew?>(null) }
    var selectedBag by remember { mutableStateOf<CoffeeBag?>(null) }
    var selectedRoaster by remember { mutableStateOf<Roaster?>(null) }
    
    var showFabMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Any?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val darkTheme = isSystemInDarkTheme()
    val coffeeGradient = if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(CoffeeDarkGradient1, CoffeeDarkGradient2)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(CoffeeLightGradient1, CoffeeLightGradient2)
        )
    }

    ManzerTrackerTheme(isFitness = false) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
            Box(modifier = Modifier.wrapContentSize()) {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Brew") },
                        onClick = {
                            activeForm = CoffeeFormType.BREW
                            showFabMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Coffee Bag") },
                        onClick = {
                            activeForm = CoffeeFormType.BAG
                            showFabMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add Roaster") },
                        onClick = {
                            activeForm = CoffeeFormType.ROASTER
                            showFabMenu = false
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(coffeeGradient)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)

        ) {
            Text(text = "Recent Brews", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(brews) { brew ->
                    val bag = bags.find { it.id == brew.bagId }
                    val roaster = roasters.find { it.id == bag?.roasterId }
                    BrewItem(
                        brew = brew,
                        bag = bag,
                        roaster = roaster,
                        onClick = {
                            selectedBrew = brew
                            activeForm = CoffeeFormType.BREW_DETAIL
                        }
                    )
                }
            }
        }

        if (activeForm != CoffeeFormType.NONE) {
            ModalBottomSheet(
                onDismissRequest = { 
                    activeForm = CoffeeFormType.NONE
                    selectedBrew = null
                    selectedBag = null
                    selectedRoaster = null
                },
                sheetState = sheetState
            ) {
                when (activeForm) {
                    CoffeeFormType.BREW -> CoffeeBrewForm(
                        bags = bags,
                        roasters = roasters,
                        onSave = { brew ->
                            viewModel.addBrew(brew)
                            activeForm = CoffeeFormType.NONE
                        }
                    )
                    CoffeeFormType.EDIT_BREW -> selectedBrew?.let { brew ->
                        CoffeeBrewForm(
                            brew = brew,
                            bags = bags,
                            roasters = roasters,
                            onSave = { updatedBrew ->
                                viewModel.updateBrew(updatedBrew)
                                activeForm = CoffeeFormType.NONE
                            }
                        )
                    }
                    CoffeeFormType.ROASTER -> RoasterForm(
                        onSave = { name, location ->
                            viewModel.addRoaster(name, location)
                            activeForm = CoffeeFormType.NONE
                        }
                    )
                    CoffeeFormType.EDIT_ROASTER -> selectedRoaster?.let { roaster ->
                        RoasterForm(
                            roaster = roaster,
                            onSave = { name, location ->
                                viewModel.updateRoaster(roaster.copy(name = name, location = location))
                                activeForm = CoffeeFormType.NONE
                            }
                        )
                    }
                    CoffeeFormType.BAG -> CoffeeBagForm(
                        roasters = roasters,
                        onSave = { roasterId, name, origin, variety, process, roastDate, region ->
                            viewModel.addCoffeeBag(roasterId, name, origin, variety, process, roastDate, region)
                            activeForm = CoffeeFormType.NONE
                        }
                    )
                    CoffeeFormType.EDIT_BAG -> selectedBag?.let { bag ->
                        CoffeeBagForm(
                            bag = bag,
                            roasters = roasters,
                            onSave = { roasterId, name, origin, variety, process, roastDate, region ->
                                viewModel.updateCoffeeBag(bag.copy(
                                    roasterId = roasterId,
                                    name = name,
                                    origin = origin,
                                    variety = variety,
                                    process = process,
                                    roastDate = roastDate,
                                    region = region
                                ))
                                activeForm = CoffeeFormType.NONE
                            }
                        )
                    }
                    CoffeeFormType.BREW_DETAIL -> {
                        selectedBrew?.let { brew ->
                            val bag = bags.find { it.id == brew.bagId }
                            val roaster = roasters.find { it.id == bag?.roasterId }
                            BrewDetail(
                                brew = brew, 
                                bag = bag, 
                                roaster = roaster,
                                onEditBrew = {
                                    activeForm = CoffeeFormType.EDIT_BREW
                                },
                                onEditBag = {
                                    selectedBag = bag
                                    activeForm = CoffeeFormType.EDIT_BAG
                                },
                                onEditRoaster = {
                                    selectedRoaster = roaster
                                    activeForm = CoffeeFormType.EDIT_ROASTER
                                },
                                onDeleteBrew = { showDeleteDialog = brew }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        showDeleteDialog?.let { item ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Item") },
                text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        when (item) {
                            is CoffeeBrew -> viewModel.deleteBrew(item)
                            is CoffeeBag -> viewModel.deleteCoffeeBag(item)
                            is Roaster -> viewModel.deleteRoaster(item)
                        }
                        showDeleteDialog = null
                        activeForm = CoffeeFormType.NONE
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
        }
}
}
