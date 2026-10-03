package com.supermanzer.manzertracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.supermanzer.manzertracker.data.BrewMethod
import com.supermanzer.manzertracker.data.CoffeeBag
import com.supermanzer.manzertracker.data.CoffeeBrew
import com.supermanzer.manzertracker.data.Roaster
import com.supermanzer.manzertracker.ui.components.DropdownField
import com.supermanzer.manzertracker.ui.components.WheelPickerField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WaterTempRangeF = 150..212
private val GrindSizeRange = 1..30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeBrewForm(
    brew: CoffeeBrew? = null,
    bags: List<CoffeeBag>,
    roasters: List<Roaster>,
    previousBrews: List<CoffeeBrew> = emptyList(),
    onSave: (CoffeeBrew) -> Unit
) {
    var selectedBag by remember { mutableStateOf(bags.find { it.id == brew?.bagId }) }
    // A method saved before the dropdown existed is kept as-is unless it matches a known one.
    var method by remember {
        mutableStateOf(BrewMethod.fromLabel(brew?.method)?.label ?: brew?.method ?: BrewMethod.V60.label)
    }
    var ratio by remember { mutableStateOf(brew?.ratio ?: "1:15") }
    var waterTemp by remember { mutableStateOf(brew?.waterTemp ?: 205) }
    var grindSize by remember { mutableStateOf(brew?.grindSize ?: 15) }
    var rating by remember { mutableStateOf(brew?.rating) }
    var notes by remember { mutableStateOf(brew?.notes ?: "") }
    var nextBrewIdeas by remember { mutableStateOf(brew?.nextBrewIdeas ?: "") }
    var expanded by remember { mutableStateOf(false) }

    // Only for a new brew: the latest earlier brew of the selected bag, if it left any ideas.
    val lastBrewWithIdeas = remember(brew, selectedBag, previousBrews) {
        if (brew != null) null
        else previousBrews
            .filter { it.bagId == selectedBag?.id }
            .maxByOrNull { it.brewDate }
            ?.takeIf { !it.nextBrewIdeas.isNullOrBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (brew == null) "Add New Brew" else "Edit Brew", 
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedBag?.let { bag ->
                    val roaster = roasters.find { it.id == bag.roasterId }
                    "${roaster?.name ?: "Unknown"} - ${bag.name}"
                } ?: "Select Coffee Bag",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                bags.forEach { bag ->
                    val roaster = roasters.find { it.id == bag.roasterId }
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(text = "${roaster?.name ?: "Unknown"} - ${bag.name}", fontWeight = FontWeight.Bold)
                                bag.roastDate?.let {
                                    Text(
                                        text = "Roasted: ${DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()).format(it)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        onClick = {
                            selectedBag = bag
                            expanded = false
                        }
                    )
                }
            }
        }

        lastBrewWithIdeas?.let { last ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Ideas from last brew (${
                            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
                                .withZone(ZoneId.systemDefault()).format(last.brewDate)
                        })",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = last.nextBrewIdeas.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField(
                label = "Brew Method",
                options = BrewMethod.entries.map { it.label },
                selected = method,
                onSelected = { method = it },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ratio,
                onValueChange = { ratio = it },
                label = { Text("Ratio (1:X)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WheelPickerField(
                label = "Water Temp",
                value = waterTemp,
                range = WaterTempRangeF,
                onValueChange = { waterTemp = it },
                modifier = Modifier.weight(1f),
                dialogTitle = "Water Temp (°F)",
                valueLabel = { "$it °F" }
            )
            WheelPickerField(
                label = "Grind Size",
                value = grindSize,
                range = GrindSizeRange,
                onValueChange = { grindSize = it },
                modifier = Modifier.weight(1f)
            )
            DropdownField(
                label = "Rating",
                options = listOf(null) + (1..5),
                selected = rating,
                onSelected = { rating = it },
                modifier = Modifier.weight(1f),
                optionLabel = { it?.toString() ?: "None" }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = nextBrewIdeas,
            onValueChange = { nextBrewIdeas = it },
            label = { Text("Ideas for Next Brew") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                selectedBag?.let { bag ->
                    onSave(
                        brew?.copy(
                            bagId = bag.id,
                            method = method,
                            ratio = ratio,
                            waterTemp = waterTemp,
                            grindSize = grindSize,
                            rating = rating,
                            notes = notes,
                            nextBrewIdeas = nextBrewIdeas.takeIf { it.isNotBlank() }
                        ) ?: CoffeeBrew(
                            bagId = bag.id,
                            method = method,
                            ratio = ratio,
                            waterTemp = waterTemp,
                            grindSize = grindSize,
                            rating = rating,
                            notes = notes,
                            nextBrewIdeas = nextBrewIdeas.takeIf { it.isNotBlank() },
                            brewDate = Instant.now()
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedBag != null && method.isNotBlank()
        ) {
            Text(if (brew == null) "Save Brew" else "Update Brew")
        }
    }
}

@Composable
fun RoasterForm(
    roaster: Roaster? = null,
    onSave: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(roaster?.name ?: "") }
    var location by remember { mutableStateOf(roaster?.location ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = if (roaster == null) "Add New Roaster" else "Edit Roaster", 
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Roaster Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSave(name, location.takeIf { it.isNotBlank() }) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Text(if (roaster == null) "Save Roaster" else "Update Roaster")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeBagForm(
    bag: CoffeeBag? = null,
    roasters: List<Roaster>,
    onSave: (Long, String, String?, String?, String?, LocalDate?, String?) -> Unit
) {
    var selectedRoaster by remember { mutableStateOf(roasters.find { it.id == bag?.roasterId }) }
    var name by remember { mutableStateOf(bag?.name ?: "") }
    var origin by remember { mutableStateOf(bag?.origin ?: "") }
    var variety by remember { mutableStateOf(bag?.variety ?: "") }
    var process by remember { mutableStateOf(bag?.process ?: "") }
    var roastDate by remember { mutableStateOf(bag?.roastDate) }
    var region by remember { mutableStateOf(bag?.region ?: "") }
    var expanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = roastDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (bag == null) "Add New Coffee Bag" else "Edit Coffee Bag", 
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedRoaster?.name ?: "Select Roaster",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                roasters.forEach { roaster ->
                    DropdownMenuItem(
                        text = { Text(roaster.name) },
                        onClick = {
                            selectedRoaster = roaster
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Coffee Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text("Origin (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = variety,
            onValueChange = { variety = it },
            label = { Text("Variety (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = process,
            onValueChange = { process = it },
            label = { Text("Process (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = roastDate?.let { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()).format(it) } ?: "",
            onValueChange = {},
            label = { Text("Roast Date (Optional)") },
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable { showDatePicker = true }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            roastDate = LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = region,
            onValueChange = { region = it },
            label = { Text("Region (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                selectedRoaster?.let { roaster ->
                    onSave(
                        roaster.id,
                        name,
                        origin.takeIf { it.isNotBlank() },
                        variety.takeIf { it.isNotBlank() },
                        process.takeIf { it.isNotBlank() },
                        roastDate,
                        region.takeIf { it.isNotBlank() }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedRoaster != null && name.isNotBlank()
        ) {
            Text(if (bag == null) "Save Coffee Bag" else "Update Coffee Bag")
        }
    }
}
