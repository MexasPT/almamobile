package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.ConnectionTestResult
import com.example.model.ServerConfig
import com.example.model.TableConfig
import com.example.ui.MainViewModel
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val tableConfig by viewModel.tableConfig.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var selectedSubmenu by remember { mutableIntStateOf(0) } // 0: Servidor, 1: Tabelas e Campos, 2: Sessão

    // Submenu 1: Server Config State
    var ip by remember(serverConfig) { mutableStateOf(serverConfig.ip) }
    var port by remember(serverConfig) { mutableStateOf(serverConfig.port) }
    var dbName by remember(serverConfig) { mutableStateOf(serverConfig.databaseName) }
    var dbUser by remember(serverConfig) { mutableStateOf(serverConfig.dbUser) }
    var dbPass by remember(serverConfig) { mutableStateOf(serverConfig.dbPassword) }
    var useHttps by remember(serverConfig) { mutableStateOf(serverConfig.useHttps) }
    var showPassword by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

    // Submenu 2: Tables and Fields State
    var userTable by remember(tableConfig) { mutableStateOf(tableConfig.userTable) }
    var userNameField by remember(tableConfig) { mutableStateOf(tableConfig.userNameField) }
    var userEmailField by remember(tableConfig) { mutableStateOf(tableConfig.userEmailField) }
    var clientTable by remember(tableConfig) { mutableStateOf(tableConfig.clientTable) }
    var clientNameField by remember(tableConfig) { mutableStateOf(tableConfig.clientNameField) }
    var companyEmail by remember(tableConfig) { mutableStateOf(tableConfig.companyNotificationEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Submenu Tab Selector
        TabRow(
            selectedTabIndex = selectedSubmenu,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubmenu == 0,
                onClick = { selectedSubmenu = 0 },
                text = { Text("1. Servidor", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubmenu == 1,
                onClick = { selectedSubmenu = 1 },
                text = { Text("2. Tabelas", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubmenu == 2,
                onClick = { selectedSubmenu = 2 },
                text = { Text("3. Sessão", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedSubmenu) {
                0 -> {
                    // Submenu 1: Configuração do Servidor
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_settings_server"),
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
                                text = "Submenu 1: Configuração do Servidor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Defina os parâmetros de ligação à base de dados YetiForce CRM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // IP
                            OutlinedTextField(
                                value = ip,
                                onValueChange = { ip = it },
                                label = { Text("Endereço IP / Host *") },
                                placeholder = { Text("Ex: 192.168.1.100 ou crm.almaforce.pt") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Dns, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_settings_ip")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Porta
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("Porta *") },
                                placeholder = { Text("80 ou 443 ou 3306") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_settings_port")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Base de dados (nome)
                            OutlinedTextField(
                                value = dbName,
                                onValueChange = { dbName = it },
                                label = { Text("Base de Dados (Nome) *") },
                                placeholder = { Text("yetiforce") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Storage, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_settings_dbname")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Utilizador bd
                            OutlinedTextField(
                                value = dbUser,
                                onValueChange = { dbUser = it },
                                label = { Text("Utilizador da BD *") },
                                placeholder = { Text("yetiforce_user") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_settings_dbuser")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password
                            OutlinedTextField(
                                value = dbPass,
                                onValueChange = { dbPass = it },
                                label = { Text("Password da BD") },
                                placeholder = { Text("Palavra-passe") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "Esconder" else "Mostrar"
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_settings_dbpass")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // HTTPS Protocol Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Usar Ligação Segura SSL/HTTPS",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = useHttps,
                                    onCheckedChange = { useHttps = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Test Connection Button
                            OutlinedButton(
                                onClick = {
                                    val testConfig = ServerConfig(
                                        ip = ip.trim(),
                                        port = port.trim(),
                                        databaseName = dbName.trim(),
                                        dbUser = dbUser.trim(),
                                        dbPassword = dbPass,
                                        useHttps = useHttps
                                    )
                                    isTestingConnection = true
                                    testResult = null
                                    coroutineScope.launch {
                                        val res = viewModel.testConnection(testConfig)
                                        testResult = res
                                        isTestingConnection = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_settings_test_conn"),
                                enabled = !isTestingConnection && ip.isNotBlank() && port.isNotBlank()
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A testar comunicação...")
                                } else {
                                    Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Testar Ligação ao Servidor")
                                }
                            }

                            testResult?.let { res ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (res is ConnectionTestResult.Success) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (res is ConnectionTestResult.Success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (res is ConnectionTestResult.Success) SuccessGreen else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (res is ConnectionTestResult.Success) res.details else (res as ConnectionTestResult.Error).details,
                                            fontSize = 12.sp,
                                            color = if (res is ConnectionTestResult.Success) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Botão para guardar e guarda no smartphone
                            Button(
                                onClick = {
                                    val newConfig = ServerConfig(
                                        ip = ip.trim(),
                                        port = port.trim(),
                                        databaseName = dbName.trim(),
                                        dbUser = dbUser.trim(),
                                        dbPassword = dbPass,
                                        useHttps = useHttps
                                    )
                                    viewModel.saveServerConfig(newConfig)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_save_server_settings"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = ip.isNotBlank() && port.isNotBlank() && dbName.isNotBlank() && dbUser.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar no Dispositivo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                1 -> {
                    // Submenu 2: Configuração de Tabelas e Campos
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_settings_tables"),
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
                                text = "Submenu 2: Configuração de Tabelas e Campos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Defina os nomes das tabelas e campos do YetiForce CRM e emails de destino.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Secção 1: Utilizadores
                            Text(
                                text = "Tabela & Campos de Utilizadores",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tabela de Utilizadores
                            OutlinedTextField(
                                value = userTable,
                                onValueChange = { userTable = it },
                                label = { Text("Tabela de Utilizadores *") },
                                placeholder = { Text("vtiger_users") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_tbl_users")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Campo do Nome
                            OutlinedTextField(
                                value = userNameField,
                                onValueChange = { userNameField = it },
                                label = { Text("Campo do Nome (Utilizador) *") },
                                placeholder = { Text("first_name, last_name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_fld_user_name")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Campo do Email "Notificações Pessoais"
                            OutlinedTextField(
                                value = userEmailField,
                                onValueChange = { userEmailField = it },
                                label = { Text("Campo do Email \"Notificações Pessoais\" *") },
                                placeholder = { Text("email1") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_fld_user_email")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Secção 2: Clientes
                            Text(
                                text = "Tabela & Campos de Clientes",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tabela de Clientes
                            OutlinedTextField(
                                value = clientTable,
                                onValueChange = { clientTable = it },
                                label = { Text("Tabela de Clientes *") },
                                placeholder = { Text("vtiger_account") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_tbl_clients")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Campo do Nome
                            OutlinedTextField(
                                value = clientNameField,
                                onValueChange = { clientNameField = it },
                                label = { Text("Campo do Nome (Cliente) *") },
                                placeholder = { Text("accountname") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_fld_client_name")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Secção 3: Email Notificações Empresa
                            Text(
                                text = "Email Notificações Empresa (Assiduidade)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Email Notificações Empresa
                            OutlinedTextField(
                                value = companyEmail,
                                onValueChange = { companyEmail = it },
                                label = { Text("Email Notificações Empresa *") },
                                placeholder = { Text("rh@almaforce.pt ou geral@empresa.com") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_company_email")
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Botão Guardar
                            Button(
                                onClick = {
                                    val newTables = TableConfig(
                                        userTable = userTable.trim(),
                                        userNameField = userNameField.trim(),
                                        userEmailField = userEmailField.trim(),
                                        clientTable = clientTable.trim(),
                                        clientNameField = clientNameField.trim(),
                                        companyNotificationEmail = companyEmail.trim()
                                    )
                                    viewModel.saveTableConfig(newTables)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_save_table_settings"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = userTable.isNotBlank() && clientTable.isNotBlank() && companyEmail.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Tabelas e Emails", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                2 -> {
                    // Submenu 3: Sessão & Ações do Sistema
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Sessão Atual do Utilizador",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = currentUser?.fullName ?: "Utilizador",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Utilizador: ${currentUser?.userName} • ${currentUser?.roleName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Email Pessoal: ${currentUser?.email}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.repository.syncClients()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizar Lista de Clientes CRM")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.logout()
                                    onLogout()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_logout"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Terminar Sessão", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
