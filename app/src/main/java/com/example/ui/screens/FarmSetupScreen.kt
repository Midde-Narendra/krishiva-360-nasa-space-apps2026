package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.models.Farm
import com.example.domain.models.Farmer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmSetupScreen(
    currentFarmer: Farmer,
    currentFarm: Farm,
    onSaveFarm: (
        farmerName: String,
        phone: String,
        village: String,
        district: String,
        state: String,
        farmName: String,
        areaAcres: Double?,
        mainCrop: String
    ) -> Unit,
    onSkipToDashboard: () -> Unit
) {
    var farmerName by remember(currentFarmer.name) { mutableStateOf(currentFarmer.name) }
    var phone by remember(currentFarmer.phone) { mutableStateOf(currentFarmer.phone) }
    var village by remember(currentFarmer.village) { mutableStateOf(currentFarmer.village) }
    var district by remember(currentFarmer.district) { mutableStateOf(currentFarmer.district) }
    var stateName by remember(currentFarmer.state) { mutableStateOf(currentFarmer.state) }
    var farmName by remember(currentFarm.name) { mutableStateOf(currentFarm.name) }
    var areaAcresText by remember(currentFarm.areaAcres) {
        mutableStateOf(currentFarm.areaAcres?.toString() ?: "")
    }
    var selectedCrop by remember(currentFarm.mainCrop) {
        mutableStateOf(currentFarm.mainCrop.ifBlank { "Groundnut" })
    }

    val crops = listOf("Groundnut", "Maize", "Bengal Gram (Chickpea)", "Cotton", "Paddy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("farm_setup_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Farm Profile Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure your local farm profile for field moisture scans and irrigation guidance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = farmerName,
                    onValueChange = { farmerName = it },
                    label = { Text("Farmer Name") },
                    placeholder = { Text("Enter farmer name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("input_farmer_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    placeholder = { Text("Enter contact number") },
                    modifier = Modifier.fillMaxWidth().testTag("input_farmer_phone"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Farm / Plot Name") },
                    placeholder = { Text("e.g. North Field Plot A") },
                    leadingIcon = { Icon(Icons.Default.Agriculture, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("input_farm_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text("Village") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.weight(1f).testTag("input_village"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District") },
                        modifier = Modifier.weight(1f).testTag("input_district"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stateName,
                        onValueChange = { stateName = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f).testTag("input_state"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = areaAcresText,
                        onValueChange = { areaAcresText = it },
                        label = { Text("Area (Acres)") },
                        modifier = Modifier.weight(1f).testTag("input_area_acres"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text(
                    text = "Primary / Target Crop",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    crops.forEach { crop ->
                        val selected = selectedCrop == crop
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCrop = crop },
                            label = { Text(crop) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val area = areaAcresText.toDoubleOrNull()?.takeIf { it > 0.0 }
                onSaveFarm(
                    farmerName.trim(),
                    phone.trim(),
                    village.trim(),
                    district.trim(),
                    stateName.trim(),
                    farmName.trim(),
                    area,
                    selectedCrop
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_farm_continue_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Farm Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onSkipToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("skip_setup_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Skip to Live Dashboard",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
