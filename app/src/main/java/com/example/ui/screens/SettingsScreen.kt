package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.SmtpResult
import com.example.model.ServerConfig
import com.example.model.SmtpConfig
import com.example.model.TableConfig
import com.example.ui.MainViewModel
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
    val tableConfig by viewModel.tableConfig.collectAsStateWithLifecycle()
    val smtpConfig by viewModel.smtpConfig.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Server State
    var ip by remember(serverConfig) { mutableStateOf(serverConfig.ip) }
    var port by remember(serverConfig) { mutableStateOf(serverConfig.port) }
    var dbName by remember(serverConfig) { mutableStateOf(serverConfig.databaseName) }
    var dbUser by remember(serverConfig) { mutableStateOf(serverConfig.dbUser) }
    var dbPass by remember(serverConfig) { mutableStateOf(serverConfig.dbPassword) }
    var useHttps by remember(serverConfig) { mutableStateOf(serverConfig.useHttps) }
    var showPassword by remember { mutableStateOf(false) }
    var apiUser by remember(serverConfig) { mutableStateOf(serverConfig.apiUser) }
    var apiPassword by remember(serverConfig) { mutableStateOf(serverConfig.apiPassword) }
    var apiKey by remember(serverConfig) { mutableStateOf(serverConfig.apiKey) }

    var testStatus by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTestingServer by remember { mutableStateOf(false) }

    // Tables State
    var userTable by remember(tableConfig) { mutableStateOf(tableConfig.userTable) }
    var userNameField by remember(tableConfig) { mutableStateOf(tableConfig.userNameField) }
    var userEmailField by remember(tableConfig) { mutableStateOf(tableConfig.userEmailField) }
    var clientTable by remember(tableConfig) { mutableStateOf(tableConfig.clientTable) }
    var clientNameField by remember(tableConfig) { mutableStateOf(tableConfig.clientNameField) }
    var companyEmail by remember(tableConfig) { mutableStateOf(tableConfig.companyNotificationEmail) }

    // SMTP State
    var smtpSenderName by remember(smtpConfig) { mutableStateOf(smtpConfig.senderName) }
    var smtpSenderEmail by remember(smtpConfig) { mutableStateOf(smtpConfig.senderEmail) }
    var smtpHost by remember(smtpConfig) { mutableStateOf(smtpConfig.host) }
    var smtpPort by remember(smtpConfig) { mutableStateOf(smtpConfig.port) }
    var smtpRequireAuth by remember(smtpConfig) { mutableStateOf(smtpConfig.requireAuth) }
    var smtpSecurityType by remember(smtpConfig) { mutableStateOf(smtpConfig.securityType) }
    var smtpUsername by remember(smtpConfig) { mutableStateOf(smtpConfig.username) }
    var smtpPassword by remember(smtpConfig) { mutableStateOf(smtpConfig.password) }
    var showSmtpPass by remember { mutableStateOf(false) }
    var smtpTestRecipient by remember(smtpConfig) { mutableStateOf("geral@iterp.pt") }

    var smtpTestStatus by remember { mutableStateOf<SmtpResult?>(null) }
    var isTestingSmtp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Configurações Globais",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Servidor YetiForce • Tabelas BD • SMTP • Sessão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4 Tabs: 1. Servidor, 2. Tabelas, 3. SMTP, 4. Sessão
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Servidor", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tabelas", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("SMTP", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Sessão", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Tab Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 1: SERVIDOR
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Configuração do Servidor YetiForce",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Defina os parâmetros de ligação à base de dados MySQL / MariaDB e API WebService do CRM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // IP Field
                            OutlinedTextField(
                                value = ip,
                                onValueChange = { ip = it },
                                label = { Text("Endereço IP / Host *") },
                                placeholder = { Text("Ex: 192.168.1.100 ou crm.almaforce.pt") },
                                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_ip")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Port Field
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("Porta *") },
                                placeholder = { Text("80 ou 443 ou 3306") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_port")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Database Name Field
                            OutlinedTextField(
                                value = dbName,
                                onValueChange = { dbName = it },
                                label = { Text("Nome da Base de Dados *") },
                                placeholder = { Text("yetiforce") },
                                leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_dbname")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // DB User Field
                            OutlinedTextField(
                                value = dbUser,
                                onValueChange = { dbUser = it },
                                label = { Text("Nome do Utilizador BD *") },
                                placeholder = { Text("yetiforce_user") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_dbuser")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // DB Password Field
                            OutlinedTextField(
                                value = dbPass,
                                onValueChange = { dbPass = it },
                                label = { Text("Password BD") },
                                placeholder = { Text("Palavra-passe da base de dados") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_dbpass")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Use HTTPS Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Usar Ligação Segura SSL/HTTPS", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (useHttps) "HTTPS Ativo (Porta 443)" else "HTTP Normal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = useHttps,
                                    onCheckedChange = { useHttps = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // YetiForce Web Service API Section
                            Text(
                                text = "Web Service - Applications (API YetiForce)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Credenciais da aplicação em YetiForce > Configurações > Integração > Web service - Applications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = apiUser,
                                onValueChange = { apiUser = it },
                                label = { Text("Utilizador da Aplicação API *") },
                                placeholder = { Text("admin") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("settings_input_apiuser")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = apiPassword,
                                onValueChange = { apiPassword = it },
                                label = { Text("Palavra-passe da Aplicação API *") },
                                placeholder = { Text("branco4admin") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("settings_input_apipass")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("Chave da Aplicação (API Key / X-API-KEY) *") },
                                placeholder = { Text("XfRn1BEJM6sa4Wmpc3TxEdVqbhYvf07G") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("settings_input_apikey")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Test Connection Button
                            OutlinedButton(
                                onClick = {
                                    val testCfg = ServerConfig(
                                        ip = ip.trim(),
                                        port = port.trim(),
                                        databaseName = dbName.trim(),
                                        dbUser = dbUser.trim(),
                                        dbPassword = dbPass,
                                        useHttps = useHttps,
                                        apiKey = apiKey.trim(),
                                        apiUser = apiUser.trim(),
                                        apiPassword = apiPassword
                                    )
                                    isTestingServer = true
                                    testStatus = null
                                    coroutineScope.launch {
                                        val res = viewModel.testConnection(testCfg)
                                        testStatus = res
                                        isTestingServer = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_btn_test_connection"),
                                enabled = !isTestingServer && ip.isNotBlank() && port.isNotBlank()
                            ) {
                                if (isTestingServer) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A testar ligação...")
                                } else {
                                    Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Testar Ligação ao Servidor")
                                }
                            }

                            testStatus?.let { status ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (status is ConnectionTestResult.Success) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (status is ConnectionTestResult.Success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (status is ConnectionTestResult.Success) SuccessGreen else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (status is ConnectionTestResult.Success) status.details else (status as ConnectionTestResult.Error).details,
                                            color = if (status is ConnectionTestResult.Success) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Save Server Config Button
                            Button(
                                onClick = {
                                    val newConfig = ServerConfig(
                                        ip = ip.trim(),
                                        port = port.trim(),
                                        databaseName = dbName.trim(),
                                        dbUser = dbUser.trim(),
                                        dbPassword = dbPass,
                                        useHttps = useHttps,
                                        apiKey = apiKey.trim(),
                                        apiUser = apiUser.trim(),
                                        apiPassword = apiPassword
                                    )
                                    viewModel.saveServerConfig(newConfig)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_btn_save_server"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = ip.isNotBlank() && port.isNotBlank() && dbName.isNotBlank() && dbUser.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Parâmetros do Servidor")
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: TABELAS E CAMPOS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Mapeamento de Tabelas e Campos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure os nomes exatos das tabelas de utilizadores e clientes na base de dados do YetiForce.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tabela de Utilizadores (Login)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = userTable,
                                onValueChange = { userTable = it },
                                label = { Text("Nome da Tabela de Utilizadores *") },
                                placeholder = { Text("vtiger_users") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_tbl_users")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = userNameField,
                                onValueChange = { userNameField = it },
                                label = { Text("Campo do Nome do Utilizador *") },
                                placeholder = { Text("first_name, last_name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_fld_user_name")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = userEmailField,
                                onValueChange = { userEmailField = it },
                                label = { Text("Campo do Email Pessoal (Notificações) *") },
                                placeholder = { Text("email1") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_fld_user_email")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Tabela de Clientes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = clientTable,
                                onValueChange = { clientTable = it },
                                label = { Text("Nome da Tabela de Clientes *") },
                                placeholder = { Text("vtiger_account") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_tbl_clients")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = clientNameField,
                                onValueChange = { clientNameField = it },
                                label = { Text("Campo do Nome do Cliente *") },
                                placeholder = { Text("accountname") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_fld_client_name")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Notificações para Empresa",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = companyEmail,
                                onValueChange = { companyEmail = it },
                                label = { Text("Email Notificações Empresa (Assiduidade) *") },
                                placeholder = { Text("rh@almaforce.pt") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_company_email")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val newConfig = TableConfig(
                                        userTable = userTable.trim(),
                                        userNameField = userNameField.trim(),
                                        userEmailField = userEmailField.trim(),
                                        clientTable = clientTable.trim(),
                                        clientNameField = clientNameField.trim(),
                                        companyNotificationEmail = companyEmail.trim()
                                    )
                                    viewModel.saveTableConfig(newConfig)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_btn_save_tables"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = userTable.isNotBlank() && clientTable.isNotBlank() && companyEmail.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Mapeamento de Tabelas")
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: SMTP
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Configuração do Servidor SMTP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Defina os dados de envio de email por SMTP direto pela aplicação (sem pedir cliente de email externo).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Nome Remetente
                            OutlinedTextField(
                                value = smtpSenderName,
                                onValueChange = { smtpSenderName = it },
                                label = { Text("Nome do Remetente *") },
                                placeholder = { Text("AlmaForce CRM") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_smtp_sender_name")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Email Remetente
                            OutlinedTextField(
                                value = smtpSenderEmail,
                                onValueChange = {
                                    smtpSenderEmail = it
                                    if (smtpTestRecipient.isBlank()) smtpTestRecipient = it
                                },
                                label = { Text("Email do Remetente (From) *") },
                                placeholder = { Text("notificacoes@empresa.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_smtp_sender_email")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Host SMTP
                            OutlinedTextField(
                                value = smtpHost,
                                onValueChange = { smtpHost = it },
                                label = { Text("Host SMTP *") },
                                placeholder = { Text("smtp.almaforce.pt ou smtp.gmail.com") },
                                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_smtp_host")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Porta SMTP
                            OutlinedTextField(
                                value = smtpPort,
                                onValueChange = { smtpPort = it },
                                label = { Text("Porta SMTP *") },
                                placeholder = { Text("587 ou 465 ou 25") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_smtp_port")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Criptografia / Segurança
                            Text(
                                text = "Segurança / Criptografia:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("TLS", "SSL", "NONE").forEach { sec ->
                                    FilterChip(
                                        selected = smtpSecurityType == sec,
                                        onClick = {
                                            smtpSecurityType = sec
                                            if (sec == "SSL" && smtpPort == "587") smtpPort = "465"
                                            if (sec == "TLS" && smtpPort == "465") smtpPort = "587"
                                        },
                                        label = { Text(if (sec == "NONE") "Sem SSL" else sec) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Autenticação Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Com Autenticação SMTP", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = smtpRequireAuth,
                                    onCheckedChange = { smtpRequireAuth = it }
                                )
                            }

                            if (smtpRequireAuth) {
                                Spacer(modifier = Modifier.height(10.dp))

                                // Utilizador SMTP
                                OutlinedTextField(
                                    value = smtpUsername,
                                    onValueChange = { smtpUsername = it },
                                    label = { Text("Utilizador / Email de Autenticação *") },
                                    placeholder = { Text("user@empresa.com") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_smtp_user")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Password SMTP
                                OutlinedTextField(
                                    value = smtpPassword,
                                    onValueChange = { smtpPassword = it },
                                    label = { Text("Palavra-passe SMTP *") },
                                    placeholder = { Text("Palavra-passe da conta SMTP") },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showSmtpPass = !showSmtpPass }) {
                                            Icon(
                                                imageVector = if (showSmtpPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (showSmtpPass) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_smtp_pass")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Testar Envio SMTP
                            OutlinedTextField(
                                value = smtpTestRecipient,
                                onValueChange = { smtpTestRecipient = it },
                                label = { Text("Email de Destino do Teste") },
                                placeholder = { Text("teste@empresa.com") },
                                leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val cfg = SmtpConfig(
                                        senderName = smtpSenderName.trim(),
                                        senderEmail = smtpSenderEmail.trim(),
                                        host = smtpHost.trim(),
                                        port = smtpPort.trim(),
                                        requireAuth = smtpRequireAuth,
                                        securityType = smtpSecurityType,
                                        username = smtpUsername.trim(),
                                        password = smtpPassword
                                    )
                                    isTestingSmtp = true
                                    smtpTestStatus = null
                                    coroutineScope.launch {
                                        val res = viewModel.testSmtp(cfg, smtpTestRecipient.trim())
                                        smtpTestStatus = res
                                        isTestingSmtp = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_btn_test_smtp"),
                                enabled = !isTestingSmtp && smtpHost.isNotBlank()
                            ) {
                                if (isTestingSmtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("A enviar email de teste via SMTP...")
                                } else {
                                    Icon(imageVector = Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Testar Envio por SMTP")
                                }
                            }

                            smtpTestStatus?.let { status ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (status is SmtpResult.Success) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (status is SmtpResult.Success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (status is SmtpResult.Success) SuccessGreen else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (status is SmtpResult.Success) status.message else "${(status as SmtpResult.Error).errorMessage}: ${status.details}",
                                            color = if (status is SmtpResult.Success) SuccessGreen else MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Guardar SMTP
                            Button(
                                onClick = {
                                    val newConfig = SmtpConfig(
                                        senderName = smtpSenderName.trim(),
                                        senderEmail = smtpSenderEmail.trim(),
                                        host = smtpHost.trim(),
                                        port = smtpPort.trim(),
                                        requireAuth = smtpRequireAuth,
                                        securityType = smtpSecurityType,
                                        username = smtpUsername.trim(),
                                        password = smtpPassword
                                    )
                                    viewModel.saveSmtpConfig(newConfig)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_btn_save_smtp"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = smtpHost.isNotBlank() && smtpSenderEmail.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Configurações SMTP")
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 4: SESSÃO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Informações do Utilizador Autenticado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dados reais da sessão ativa obtidos do YetiForce CRM:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val user = currentUser
                            if (user != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(text = "Nome: ${user.fullName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Utilizador: ${user.userName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Email: ${user.email.ifBlank { "Sem email definido" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Função: ${user.roleName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "ID: ${user.id} • Estado: ${user.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nenhum utilizador com sessão ativa.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Logout Button
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Terminar Sessão e Sair")
                            }
                        }
                    }
                }
            }
        }
    }
}
