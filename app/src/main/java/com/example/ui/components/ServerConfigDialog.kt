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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.ServerConfig
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ServerConfigDialog(
    currentConfig: ServerConfig,
    onDismiss: () -> Unit,
    onSave: (ServerConfig) -> Unit,
    onTestConnection: suspend (ServerConfig) -> ConnectionTestResult,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var ip by remember { mutableStateOf(currentConfig.ip) }
    var port by remember { mutableStateOf(currentConfig.port) }
    var dbName by remember { mutableStateOf(currentConfig.databaseName) }
    var dbUser by remember { mutableStateOf(currentConfig.dbUser) }
    var dbPass by remember { mutableStateOf(currentConfig.dbPassword) }
    var useHttps by remember { mutableStateOf(currentConfig.useHttps) }
    var showPassword by remember { mutableStateOf(false) }

    var testStatus by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_server_config"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Configuração do Servidor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Configure a ligação ao CRM YetiForce (IP, Porta, Base de Dados, Utilizador e Password). Esta informação fica guardada com segurança no dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // Use HTTPS Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Usar Protocolo Seguro (HTTPS)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (useHttps) "Porta 443 / SSL Ativo" else "HTTP Padrão (Porta 80)",
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

                // Test Connection Button
                OutlinedButton(
                    onClick = {
                        val testCfg = ServerConfig(
                            ip = ip.trim(),
                            port = port.trim(),
                            databaseName = dbName.trim(),
                            dbUser = dbUser.trim(),
                            dbPassword = dbPass,
                            useHttps = useHttps
                        )
                        isTesting = true
                        testStatus = null
                        coroutineScope.launch {
                            val res = onTestConnection(testCfg)
                            testStatus = res
                            isTesting = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_test_connection"),
                    enabled = !isTesting && ip.isNotBlank() && port.isNotBlank()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("A testar ligação...")
                    } else {
                        Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testar Ligação ao Servidor")
                    }
                }

                // Test Result Card
                testStatus?.let { status ->
                    Spacer(modifier = Modifier.height(12.dp))
                    when (status) {
                        is ConnectionTestResult.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = SuccessGreen.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = status.details,
                                        color = SuccessGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        is ConnectionTestResult.Error -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = status.errorMessage,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = status.details,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 11.sp
                                        )
                                    }
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
                    val newConfig = ServerConfig(
                        ip = ip.trim(),
                        port = port.trim(),
                        databaseName = dbName.trim(),
                        dbUser = dbUser.trim(),
                        dbPassword = dbPass,
                        useHttps = useHttps
                    )
                    onSave(newConfig)
                },
                modifier = Modifier.testTag("btn_save_server_config"),
                enabled = ip.isNotBlank() && port.isNotBlank() && dbName.isNotBlank() && dbUser.isNotBlank()
            ) {
                Text("Guardar no Dispositivo")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_server_config")
            ) {
                Text("Cancelar")
            }
        }
    )
}
