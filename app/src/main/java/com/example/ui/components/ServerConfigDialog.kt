package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.SmtpResult
import com.example.model.ServerConfig
import com.example.model.SmtpConfig
import com.example.model.TableConfig
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ServerConfigDialog(
    currentConfig: ServerConfig,
    currentTableConfig: TableConfig,
    currentSmtpConfig: SmtpConfig,
    onDismiss: () -> Unit,
    onSaveAll: (ServerConfig, TableConfig, SmtpConfig) -> Unit,
    onTestConnection: suspend (ServerConfig) -> ConnectionTestResult,
    onTestSmtp: suspend (SmtpConfig, String) -> SmtpResult,
    coroutineScope: CoroutineScope
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Servidor, 1: Tabelas, 2: SMTP

    // Server State
    var ip by remember { mutableStateOf(currentConfig.ip) }
    var port by remember { mutableStateOf(currentConfig.port) }
    var dbName by remember { mutableStateOf(currentConfig.databaseName) }
    var dbUser by remember { mutableStateOf(currentConfig.dbUser) }
    var dbPass by remember { mutableStateOf(currentConfig.dbPassword) }
    var useHttps by remember { mutableStateOf(currentConfig.useHttps) }
    var showPassword by remember { mutableStateOf(false) }
    var apiUser by remember { mutableStateOf(currentConfig.apiUser) }
    var apiPassword by remember { mutableStateOf(currentConfig.apiPassword) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }

    var testStatus by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTestingServer by remember { mutableStateOf(false) }

    // Tables State
    var userTable by remember { mutableStateOf(currentTableConfig.userTable) }
    var userNameField by remember { mutableStateOf(currentTableConfig.userNameField) }
    var userEmailField by remember { mutableStateOf(currentTableConfig.userEmailField) }
    var clientTable by remember { mutableStateOf(currentTableConfig.clientTable) }
    var clientNameField by remember { mutableStateOf(currentTableConfig.clientNameField) }
    var companyEmail by remember { mutableStateOf(currentTableConfig.companyNotificationEmail) }

    // SMTP State
    var smtpSenderName by remember { mutableStateOf(currentSmtpConfig.senderName) }
    var smtpSenderEmail by remember { mutableStateOf(currentSmtpConfig.senderEmail) }
    var smtpHost by remember { mutableStateOf(currentSmtpConfig.host) }
    var smtpPort by remember { mutableStateOf(currentSmtpConfig.port) }
    var smtpRequireAuth by remember { mutableStateOf(currentSmtpConfig.requireAuth) }
    var smtpSecurityType by remember { mutableStateOf(currentSmtpConfig.securityType) } // "TLS", "SSL", "NONE"
    var smtpUsername by remember { mutableStateOf(currentSmtpConfig.username) }
    var smtpPassword by remember { mutableStateOf(currentSmtpConfig.password) }
    var showSmtpPassword by remember { mutableStateOf(false) }
    var smtpTestEmail by remember { mutableStateOf("geral@iterp.pt") }

    var smtpTestStatus by remember { mutableStateOf<SmtpResult?>(null) }
    var isTestingSmtp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_server_config"),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Configuração Inicial do Sistema",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("1. Servidor", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("2. Tabelas", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("3. SMTP", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 1: SERVIDOR
                        Text(
                            text = "Parâmetros de Ligação à Base de Dados YetiForce CRM:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // IP Field
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text("Endereço IP / Host Servidor *") },
                            placeholder = { Text("Ex: 192.168.1.100 ou crm.almaforce.pt") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Dns, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_ip")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Port Field
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
                                .testTag("input_config_port")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Database Name Field
                        OutlinedTextField(
                            value = dbName,
                            onValueChange = { dbName = it },
                            label = { Text("Nome da Base de Dados *") },
                            placeholder = { Text("yetiforce") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Storage, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_dbname")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // DB User Field
                        OutlinedTextField(
                            value = dbUser,
                            onValueChange = { dbUser = it },
                            label = { Text("Nome do Utilizador BD *") },
                            placeholder = { Text("yetiforce_user") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_dbuser")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // DB Password Field
                        OutlinedTextField(
                            value = dbPass,
                            onValueChange = { dbPass = it },
                            label = { Text("Password BD") },
                            placeholder = { Text("Palavra-passe da base de dados") },
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
                                .testTag("input_config_dbpass")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Use HTTPS Switch
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

                        Spacer(modifier = Modifier.height(14.dp))

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
                            modifier = Modifier.fillMaxWidth().testTag("input_config_apiuser")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiPassword,
                            onValueChange = { apiPassword = it },
                            label = { Text("Palavra-passe da Aplicação API *") },
                            placeholder = { Text("branco4admin") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_config_apipass")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("Chave da Aplicação (API Key / X-API-KEY) *") },
                            placeholder = { Text("XfRn1BEJM6sa4Wmpc3TxEdVqbhYvf07G") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_config_apikey")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                                    val res = onTestConnection(testCfg)
                                    testStatus = res
                                    isTestingServer = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_connection"),
                            enabled = !isTestingServer && ip.isNotBlank() && port.isNotBlank()
                        ) {
                            if (isTestingServer) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A testar ligação ao servidor...")
                            } else {
                                Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testar Ligação ao Servidor")
                            }
                        }

                        // Test Result Card
                        testStatus?.let { status ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (status is ConnectionTestResult.Success) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                    }

                    1 -> {
                        // TAB 2: TABELAS E CAMPOS
                        Text(
                            text = "Configure os nomes exatos das tabelas de utilizadores e clientes na base de dados para o login e consultas funcionarem:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                            label = { Text("Tabela de Utilizadores *") },
                            placeholder = { Text("vtiger_users") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_tbl_users")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = userNameField,
                            onValueChange = { userNameField = it },
                            label = { Text("Campo Nome do Utilizador *") },
                            placeholder = { Text("first_name, last_name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_fld_user_name")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = userEmailField,
                            onValueChange = { userEmailField = it },
                            label = { Text("Campo Email Pessoal (Notificações) *") },
                            placeholder = { Text("email1") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_fld_user_email")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                            label = { Text("Tabela de Clientes *") },
                            placeholder = { Text("vtiger_account") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_tbl_clients")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = clientNameField,
                            onValueChange = { clientNameField = it },
                            label = { Text("Campo Nome do Cliente *") },
                            placeholder = { Text("accountname") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_fld_client_name")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Notificações Empresa",
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
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_config_company_email")
                        )
                    }

                    2 -> {
                        // TAB 3: SMTP
                        Text(
                            text = "Configure o servidor SMTP para envio automático de emails em segundo plano:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nome Remetente
                        OutlinedTextField(
                            value = smtpSenderName,
                            onValueChange = { smtpSenderName = it },
                            label = { Text("Nome do Remetente *") },
                            placeholder = { Text("AlmaForce CRM") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_smtp_sender_name")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Email Remetente
                        OutlinedTextField(
                            value = smtpSenderEmail,
                            onValueChange = {
                                smtpSenderEmail = it
                                if (smtpTestEmail.isBlank()) smtpTestEmail = it
                            },
                            label = { Text("Email do Remetente (From) *") },
                            placeholder = { Text("notificacoes@empresa.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_smtp_sender_email")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                .testTag("input_smtp_host")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                .testTag("input_smtp_port")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Autenticação Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Com Autenticação SMTP", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = smtpRequireAuth,
                                onCheckedChange = { smtpRequireAuth = it }
                            )
                        }

                        if (smtpRequireAuth) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Utilizador SMTP
                            OutlinedTextField(
                                value = smtpUsername,
                                onValueChange = { smtpUsername = it },
                                label = { Text("Utilizador / Email SMTP *") },
                                placeholder = { Text("user@empresa.com") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_smtp_user")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Password SMTP
                            OutlinedTextField(
                                value = smtpPassword,
                                onValueChange = { smtpPassword = it },
                                label = { Text("Palavra-passe SMTP *") },
                                placeholder = { Text("Palavra-passe da conta") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showSmtpPassword = !showSmtpPassword }) {
                                        Icon(
                                            imageVector = if (showSmtpPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (showSmtpPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_smtp_pass")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Testar SMTP
                        OutlinedTextField(
                            value = smtpTestEmail,
                            onValueChange = { smtpTestEmail = it },
                            label = { Text("Email de Destino do Teste") },
                            placeholder = { Text("teste@empresa.com") },
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
                                    val res = onTestSmtp(cfg, smtpTestEmail.trim())
                                    smtpTestStatus = res
                                    isTestingSmtp = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_smtp"),
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
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newServer = ServerConfig(
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
                    val newTables = TableConfig(
                        userTable = userTable.trim(),
                        userNameField = userNameField.trim(),
                        userEmailField = userEmailField.trim(),
                        clientTable = clientTable.trim(),
                        clientNameField = clientNameField.trim(),
                        companyNotificationEmail = companyEmail.trim()
                    )
                    val newSmtp = SmtpConfig(
                        senderName = smtpSenderName.trim(),
                        senderEmail = smtpSenderEmail.trim(),
                        host = smtpHost.trim(),
                        port = smtpPort.trim(),
                        requireAuth = smtpRequireAuth,
                        securityType = smtpSecurityType,
                        username = smtpUsername.trim(),
                        password = smtpPassword
                    )
                    onSaveAll(newServer, newTables, newSmtp)
                },
                modifier = Modifier.testTag("btn_save_server_config"),
                enabled = ip.isNotBlank() && port.isNotBlank() && dbName.isNotBlank() && userTable.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Todas as Configurações")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_server_config")
            ) {
                Text("Fechar")
            }
        }
    )
}
