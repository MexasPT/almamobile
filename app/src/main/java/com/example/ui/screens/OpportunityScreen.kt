package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.EmailMessage
import com.example.data.remote.SmtpResult
import com.example.model.Client
import com.example.model.Opportunity
import com.example.ui.MainViewModel
import com.example.ui.components.MapLocationCard
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityScreen(
    viewModel: MainViewModel
) {
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isGpsRefreshing by viewModel.isGpsRefreshing.collectAsStateWithLifecycle()
    val allClients by viewModel.allClients.collectAsStateWithLifecycle()
    val allOpportunities by viewModel.allOpportunities.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Form States
    var opportunityType by remember { mutableStateOf("Cliente") } // "Cliente" or "Lead"
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var clientSearchQuery by remember { mutableStateOf("") }
    var showClientDialog by remember { mutableStateOf(false) }

    var leadCompany by remember { mutableStateOf("") }

    var subject by remember { mutableStateOf("AlmaForce") }
    var customSubject by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }

    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // In-App Opportunity Save + SMTP Result Dialog
    var saveResultDialog by remember { mutableStateOf<Triple<Opportunity, EmailMessage, SmtpResult>?>(null) }

    val subjectOptions = listOf("AlmaForce", "Faturação", "Outros")

    // Client Selector Dialog
    if (showClientDialog) {
        val filteredClients = if (clientSearchQuery.isBlank()) {
            allClients
        } else {
            allClients.filter { it.accountName.contains(clientSearchQuery, ignoreCase = true) || it.city.contains(clientSearchQuery, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showClientDialog = false },
            title = {
                Text(
                    text = "Selecionar Cliente (vtiger_account)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = clientSearchQuery,
                        onValueChange = { clientSearchQuery = it },
                        placeholder = { Text("Pesquisar cliente por nome...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (clientSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { clientSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_client")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        if (filteredClients.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (allClients.isEmpty()) "Nenhum cliente sincronizado do CRM YetiForce. Aceda às Configurações para validar a tabela de clientes." else "Nenhum cliente encontrado com esse nome.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        } else {
                            items(filteredClients) { client ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedClient = client
                                            showClientDialog = false
                                        }
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedClient?.id == client.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = client.accountName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (client.city.isNotBlank()) {
                                                Text(
                                                    text = "${client.city} • ${client.industry}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClientDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // In-App Opportunity Save Feedback Dialog
    saveResultDialog?.let { (opp, emailMsg, smtpRes) ->
        val isSmtpSuccess = smtpRes is SmtpResult.Success

        AlertDialog(
            onDismissRequest = { saveResultDialog = null },
            icon = {
                Icon(
                    imageVector = if (isSmtpSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSmtpSuccess) SuccessGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Oportunidade Gravada!",
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "A oportunidade comercial foi guardada na base de dados, associada às coordenadas GPS da morada e enviada via SMTP para o seu email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "• Tipo: ${opp.type} (${opp.displayEntityName})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "• Assunto: ${opp.finalSubject}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "• Morada GPS: ${opp.streetAddress}", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SMTP status banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSmtpSuccess) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = if (isSmtpSuccess) SuccessGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isSmtpSuccess) "Notificação SMTP enviada para:" else "Aviso de Envio SMTP:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSmtpSuccess) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (isSmtpSuccess) opp.userEmail.ifBlank { emailMsg.recipient } else (smtpRes as SmtpResult.Error).details,
                                    fontSize = 11.sp,
                                    color = if (isSmtpSuccess) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveResultDialog = null },
                    modifier = Modifier.testTag("btn_close_opp_dialog")
                ) {
                    Text("OK, Concluir")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Tab Selector
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Nova Oportunidade", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Histórico Geral (${allOpportunities.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTabIndex == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Interactive Map with device location
                MapLocationCard(
                    location = currentLocation,
                    isRefreshing = isGpsRefreshing,
                    onRefresh = { viewModel.refreshGps() },
                    title = "Localização da Oportunidade"
                )

                // 2. Formulário
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_opportunity_form"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Dados da Oportunidade Comercial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Registe a visita ou contacto comercial associado às coordenadas GPS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo: Tipo de Oportunidade (Cliente / Lead)
                        Text(
                            text = "Tipo de Oportunidade *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("segment_opportunity_type")
                        ) {
                            SegmentedButton(
                                selected = opportunityType == "Cliente",
                                onClick = { opportunityType = "Cliente" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Icon(imageVector = Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cliente (vtiger_account)")
                            }

                            SegmentedButton(
                                selected = opportunityType == "Lead",
                                onClick = { opportunityType = "Lead" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lead (Prospeção)")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Conditional Fields
                        if (opportunityType == "Cliente") {
                            Text(
                                text = "Selecionar Cliente *",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showClientDialog = true }
                                    .testTag("btn_select_client"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = if (selectedClient != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = selectedClient?.accountName ?: "Clique para escolher o Cliente...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedClient != null) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedClient != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (selectedClient != null && selectedClient!!.city.isNotBlank()) {
                                                Text(
                                                    text = "${selectedClient!!.city} • ${selectedClient!!.vatNumber}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Pesquisar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = leadCompany,
                                onValueChange = { leadCompany = it },
                                label = { Text("Empresa \"Lead\" *") },
                                placeholder = { Text("Nome da empresa ou contacto proscrito") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_lead_company")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo: Assunto (AlmaForce, Faturação, Outros)
                        Text(
                            text = "Assunto *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = subjectMenuExpanded,
                            onExpandedChange = { subjectMenuExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = subject,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Assunto") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("select_subject")
                            )

                            ExposedDropdownMenu(
                                expanded = subjectMenuExpanded,
                                onDismissRequest = { subjectMenuExpanded = false }
                            ) {
                                subjectOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            subject = option
                                            subjectMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (subject == "Outros") {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customSubject,
                                onValueChange = { customSubject = it },
                                label = { Text("Outro Assunto *") },
                                placeholder = { Text("Especifique detalhadamente o assunto...") },
                                isError = customSubject.isBlank(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_subject")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Observações
                        OutlinedTextField(
                            value = observations,
                            onValueChange = { observations = it },
                            label = { Text("Observações") },
                            placeholder = { Text("Registe notas sobre a reunião, requisitos do cliente ou próximos passos...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_observations")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botão Guardar
                        val isFormValid = if (opportunityType == "Cliente") {
                            selectedClient != null && (subject != "Outros" || customSubject.isNotBlank())
                        } else {
                            leadCompany.isNotBlank() && (subject != "Outros" || customSubject.isNotBlank())
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                viewModel.saveOpportunity(
                                    type = opportunityType,
                                    client = selectedClient,
                                    leadCompany = leadCompany,
                                    subject = subject,
                                    customSubject = customSubject,
                                    notes = observations,
                                    onComplete = { opp, emailMsg, smtpRes ->
                                        isSaving = false
                                        saveResultDialog = Triple(opp, emailMsg, smtpRes)
                                        // Reset form
                                        leadCompany = ""
                                        selectedClient = null
                                        customSubject = ""
                                        observations = ""
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_save_opportunity"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isFormValid && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A gravar e enviar por SMTP...")
                            } else {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar e Enviar por SMTP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Histórico de Oportunidades
            if (allOpportunities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ainda não existem oportunidades registadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allOpportunities) { opp ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("item_opportunity_${opp.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (opp.type == "Cliente") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = opp.type,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (opp.type == "Cliente") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Text(
                                        text = sdf.format(Date(opp.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = opp.displayEntityName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Assunto: ${opp.finalSubject}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (opp.observations.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = opp.observations,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = opp.streetAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
