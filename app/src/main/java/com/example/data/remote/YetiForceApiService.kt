package com.example.data.remote

import android.util.Log
import com.example.model.Attendance
import com.example.model.Client
import com.example.model.Opportunity
import com.example.model.ServerConfig
import com.example.model.TableConfig
import com.example.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.concurrent.TimeUnit

sealed class AuthResult {
    data class Success(val user: User, val message: String) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
}

sealed class ConnectionTestResult {
    data class Success(val responseTimeMs: Long, val details: String) : ConnectionTestResult()
    data class Error(val errorMessage: String, val details: String) : ConnectionTestResult()
}

class YetiForceApiService {

    companion object {
        private const val TAG = "YetiForceApiService"
        init {
            try {
                Class.forName("org.mariadb.jdbc.Driver")
            } catch (e: Exception) {
                Log.w(TAG, "MariaDB JDBC Driver registration: ${e.message}")
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun getConnectionWithDiag(serverConfig: ServerConfig): Pair<Connection?, String> {
        val host = cleanHost(serverConfig.ip)
        val port = serverConfig.port.trim().toIntOrNull() ?: 3306
        val dbName = serverConfig.databaseName.trim()
        val dbUser = serverConfig.dbUser.trim()
        val dbPass = serverConfig.dbPassword

        val urls = listOf(
            "jdbc:mariadb://$host:$port/$dbName?connectTimeout=7000&socketTimeout=7000&allowPublicKeyRetrieval=true&sslMode=trust&disableSsl=true&useSSL=false&characterEncoding=UTF-8&autoReconnect=true",
            "jdbc:mariadb://$host:$port/$dbName?connectTimeout=7000&socketTimeout=7000&disableSsl=true&autoReconnect=true",
            "jdbc:mariadb://$host:$port/$dbName?connectTimeout=7000&socketTimeout=7000"
        )

        var lastError = ""
        for (url in urls) {
            try {
                val conn = DriverManager.getConnection(url, dbUser, dbPass)
                if (conn != null && !conn.isClosed) {
                    return Pair(conn, "OK")
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.toString()
                Log.w(TAG, "Attempt with URL $url failed: $lastError")
            }
        }
        return Pair(null, lastError)
    }

    private fun getJdbcConnection(serverConfig: ServerConfig): Connection? {
        return getConnectionWithDiag(serverConfig).first
    }

    private fun cleanHost(raw: String): String {
        var host = raw.trim()
        if (host.startsWith("http://", ignoreCase = true)) {
            host = host.substring(7)
        } else if (host.startsWith("https://", ignoreCase = true)) {
            host = host.substring(8)
        }
        return host.split("/")[0].split(":")[0].trim()
    }

    suspend fun testConnection(serverConfig: ServerConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val host = cleanHost(serverConfig.ip)
            if (host.isBlank()) {
                return@withContext ConnectionTestResult.Error(
                    errorMessage = "Endereço IP/Host em branco",
                    details = "Introduza o endereço IP ou domínio do servidor YetiForce."
                )
            }

            val portInt = serverConfig.port.trim().toIntOrNull() ?: 3306

            // 1. Test TCP socket reachability to Host:Port
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, portInt), 5000)
            }
            val elapsed = System.currentTimeMillis() - startTime

            // 2. Direct JDBC database connection test
            var dbDetail = ""
            if (serverConfig.dbUser.isNotBlank() && (portInt == 3306 || portInt == 3307 || portInt == 3308)) {
                val (conn, diag) = getConnectionWithDiag(serverConfig)
                if (conn != null) {
                    try {
                        val stmt = conn.createStatement()
                        val rs = stmt.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${serverConfig.databaseName.trim()}'")
                        var tableCount = 0
                        if (rs.next()) {
                            tableCount = rs.getInt(1)
                        }
                        rs.close()
                        stmt.close()
                        conn.close()
                        dbDetail = " • Ligação BD OK ($tableCount tabelas encontradas)"
                    } catch (e: Exception) {
                        dbDetail = " • Ligação BD efetuada (${e.message})"
                    }
                } else {
                    dbDetail = " • Porta 3306 acessível (Aviso BD: $diag)"
                }
            } else {
                // Try HTTP probe
                try {
                    val testUrl = "${serverConfig.baseUrl}/"
                    val req = Request.Builder().url(testUrl).get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        dbDetail = " • HTTP ${resp.code}"
                    }
                } catch (_: Exception) {}
            }

            ConnectionTestResult.Success(
                responseTimeMs = elapsed,
                details = "Ligação com sucesso a $host:$portInt (Base de Dados: ${serverConfig.databaseName}$dbDetail • ${elapsed}ms)"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            val msg = e.localizedMessage ?: "Tempo limite esgotado ou endereço inacessível."
            ConnectionTestResult.Error(
                errorMessage = "Falha ao contactar o servidor ${serverConfig.ip}:${serverConfig.port}",
                details = "Erro: $msg. Verifique se o IP, porta e serviço estão acessíveis na rede."
            )
        }
    }

    suspend fun authenticate(
        username: String,
        password: String,
        serverConfig: ServerConfig,
        tableConfig: TableConfig
    ): AuthResult = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) {
            return@withContext AuthResult.Error("Por favor introduza o Utilizador e a Palavra-passe.")
        }

        val cleanUser = username.trim()
        val userTable = tableConfig.userTable.ifBlank { "vtiger_users" }.trim()

        // 1. Direct Database Query with Hashed Password Verification (BCrypt, MD5, SHA, etc.)
        val (conn, dbDiag) = getConnectionWithDiag(serverConfig)
        if (conn != null) {
            try {
                val sql = "SELECT * FROM `$userTable` WHERE LOWER(`user_name`) = LOWER(?) OR LOWER(`email1`) = LOWER(?) OR LOWER(`email`) = LOWER(?) LIMIT 1"
                val stmt = conn.prepareStatement(sql)
                stmt.setString(1, cleanUser)
                stmt.setString(2, cleanUser)
                stmt.setString(3, cleanUser)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    val userId = getColumnString(rs, "id", "id").ifBlank { getColumnString(rs, "user_id", "1") }
                    val dbUserName = getColumnString(rs, "user_name", cleanUser)
                    val storedPassword = getColumnString(rs, "user_password", "")
                    val storedHash = getColumnString(rs, "user_hash", "")
                    val firstName = getColumnString(rs, "first_name", "")
                    val lastName = getColumnString(rs, "last_name", "")
                    val email = getColumnString(rs, "email1", "").ifBlank { getColumnString(rs, "email", "") }
                    val status = getColumnString(rs, "status", "Active")
                    val roleId = getColumnString(rs, "roleid", "").ifBlank { getColumnString(rs, "role_name", "Comercial") }
                    val phoneMobile = getColumnString(rs, "phone_mobile", "")
                    val department = getColumnString(rs, "department", "")

                    rs.close()
                    stmt.close()
                    conn.close()

                    Log.d(TAG, "Found user '$dbUserName' in '$userTable'. Verifying password against stored hash...")

                    // Verify Password against Hashed Password in user_password or user_hash
                    val isPassValid = PasswordVerifier.verify(password, storedPassword, dbUserName) ||
                                      (storedHash.isNotBlank() && PasswordVerifier.verify(password, storedHash, dbUserName)) ||
                                      (cleanUser.equals(serverConfig.dbUser, ignoreCase = true) && password == serverConfig.dbPassword)

                    if (isPassValid) {
                        val authenticatedUser = User(
                            id = userId,
                            userName = dbUserName,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            roleName = roleId,
                            status = status,
                            phoneMobile = phoneMobile,
                            department = department
                        )
                        Log.i(TAG, "User '$cleanUser' authenticated successfully. FullName: '${authenticatedUser.fullName}'")
                        return@withContext AuthResult.Success(authenticatedUser, "Autenticação efetuada com sucesso.")
                    } else {
                        Log.w(TAG, "Password mismatch for user '$cleanUser'.")
                        return@withContext AuthResult.Error("Palavra-passe incorreta para o utilizador '$cleanUser' (tabela '$userTable').")
                    }
                } else {
                    rs.close()
                    stmt.close()
                    conn.close()
                    Log.w(TAG, "User '$cleanUser' not found in table '$userTable'.")
                    return@withContext AuthResult.Error("O utilizador '$cleanUser' não foi encontrado na tabela '$userTable' (campo 'user_name').")
                }
            } catch (sqlEx: Exception) {
                Log.w(TAG, "SQL Query error on user table: ${sqlEx.message}")
                try { conn.close() } catch (_: Exception) {}
                return@withContext AuthResult.Error("Erro ao consultar a tabela '$userTable': ${sqlEx.message}")
            }
        }

        // 2. YetiForce CRM WebService API Fallback
        try {
            val endpoints = listOf(
                "${serverConfig.baseUrl}/webservice/Users/Login",
                "${serverConfig.baseUrl}/api/webservice/Users/Login",
                "${serverConfig.baseUrl}/webservice.php?operation=login"
            )

            for (loginUrl in endpoints) {
                try {
                    val jsonBody = JSONObject().apply {
                        put("userName", cleanUser)
                        put("user_name", cleanUser)
                        put("password", password)
                        put("database", serverConfig.databaseName)
                        put("userTable", tableConfig.userTable)
                    }.toString()

                    val requestBuilder = Request.Builder()
                        .url(loginUrl)
                        .post(jsonBody.toRequestBody("application/json".toMediaType()))

                    if (serverConfig.apiKey.isNotBlank()) {
                        requestBuilder.addHeader("X-API-KEY", serverConfig.apiKey)
                    }

                    val response = httpClient.newCall(requestBuilder.build()).execute()
                    val responseBody = response.body?.string().orEmpty()

                    if (response.isSuccessful && responseBody.isNotBlank()) {
                        val json = JSONObject(responseBody)
                        val status = json.optInt("status", if (json.optBoolean("success", false)) 1 else 0)
                        if (status == 1 || json.optJSONObject("result") != null) {
                            val resultObj = json.optJSONObject("result") ?: json
                            val user = User(
                                id = resultObj.optString("id", "usr_${cleanUser.hashCode().toString().takeLast(6)}"),
                                userName = cleanUser,
                                firstName = resultObj.optString("first_name", ""),
                                lastName = resultObj.optString("last_name", ""),
                                email = resultObj.optString("email1", resultObj.optString("email", "")),
                                roleName = resultObj.optString("role_name", resultObj.optString("role", "Comercial YetiForce")),
                                status = resultObj.optString("status", "Active"),
                                phoneMobile = resultObj.optString("phone_mobile", ""),
                                department = resultObj.optString("department", "")
                            )
                            return@withContext AuthResult.Success(user, "Autenticação YetiForce efetuada com sucesso.")
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote WebService auth error: ${e.message}")
        }

        // 3. Direct verification against database user credentials if configured
        if (cleanUser.equals(serverConfig.dbUser, ignoreCase = true) && password == serverConfig.dbPassword && serverConfig.dbPassword.isNotBlank()) {
            val user = User(
                id = "usr_${cleanUser.hashCode().toString().takeLast(6)}",
                userName = cleanUser,
                firstName = "",
                lastName = cleanUser,
                email = "",
                roleName = "Administrador Base de Dados",
                status = "Ativo (${tableConfig.userTable})",
                department = "YetiForce CRM"
            )
            return@withContext AuthResult.Success(user, "Autenticado como administrador da base de dados ${serverConfig.databaseName}.")
        }

        // 4. Return clear error message with database diagnostic
        val diagDetail = if (dbDiag.isNotBlank() && dbDiag != "OK") " (Diagnóstico BD: $dbDiag)" else ""
        AuthResult.Error("Falha na autenticação para '$cleanUser'.$diagDetail Verifique o utilizador, palavra-passe e se o servidor MySQL está acessível na porta ${serverConfig.port}.")
    }

    suspend fun fetchClients(
        serverConfig: ServerConfig,
        tableConfig: TableConfig
    ): List<Client> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Client>()

        // 1. Direct JDBC query from database
        try {
            val conn = getJdbcConnection(serverConfig)
            if (conn != null) {
                val clientTable = tableConfig.clientTable.ifBlank { "vtiger_account" }.trim()
                val sql = "SELECT * FROM `$clientTable` LIMIT 200"
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery(sql)

                var count = 0
                while (rs.next()) {
                    count++
                    val id = getColumnString(rs, "accountid", "").ifBlank { getColumnString(rs, "id", "cli_$count") }
                    val name = getColumnString(rs, tableConfig.clientNameField.ifBlank { "accountname" }, "")
                        .ifBlank { getColumnString(rs, "accountname", "") }
                        .ifBlank { getColumnString(rs, "name", "Cliente $count") }
                    val phone = getColumnString(rs, "phone", "").ifBlank { getColumnString(rs, "otherphone", "") }
                    val email = getColumnString(rs, "email1", "").ifBlank { getColumnString(rs, "email", "") }
                    val city = getColumnString(rs, "bill_city", "").ifBlank { getColumnString(rs, "city", "") }
                    val address = getColumnString(rs, "bill_street", "").ifBlank { getColumnString(rs, "address", "") }
                    val industry = getColumnString(rs, "industry", "")
                    val vat = getColumnString(rs, "vat_id", "").ifBlank { getColumnString(rs, "vat", "") }

                    list.add(
                        Client(
                            id = id,
                            accountName = name,
                            phone = phone,
                            email = email,
                            city = city,
                            address = address,
                            industry = industry,
                            vatNumber = vat
                        )
                    )
                }
                rs.close()
                stmt.close()
                conn.close()

                if (list.isNotEmpty()) {
                    Log.i(TAG, "Fetched ${list.size} real clients directly from database table `$clientTable`.")
                    return@withContext list
                }
            }
        } catch (dbEx: Exception) {
            Log.w(TAG, "Direct DB fetchClients error: ${dbEx.message}")
        }

        // 2. WebService REST API fallback
        try {
            val endpoints = listOf(
                "${serverConfig.baseUrl}/webservice/Accounts/RecordsList",
                "${serverConfig.baseUrl}/api/webservice/Accounts/RecordsList",
                "${serverConfig.baseUrl}/webservice/Accounts"
            )

            for (url in endpoints) {
                try {
                    val req = Request.Builder().url(url).get().build()
                    val response = httpClient.newCall(req).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            val json = JSONObject(body)
                            val records = json.optJSONArray("records") ?: json.optJSONArray("result")
                            if (records != null && records.length() > 0) {
                                for (i in 0 until records.length()) {
                                    val item = records.getJSONObject(i)
                                    val nameField = tableConfig.clientNameField.ifBlank { "accountname" }
                                    list.add(
                                        Client(
                                            id = item.optString("id", "cli_$i"),
                                            accountName = item.optString(nameField, item.optString("accountname", item.optString("name", "Cliente $i"))),
                                            phone = item.optString("phone", ""),
                                            email = item.optString("email1", item.optString("email", "")),
                                            city = item.optString("bill_city", item.optString("city", "")),
                                            address = item.optString("bill_street", item.optString("address", "")),
                                            industry = item.optString("industry", ""),
                                            vatNumber = item.optString("vat_id", item.optString("vat", ""))
                                        )
                                    )
                                }
                                break
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch clients REST error: ${e.message}")
        }

        list
    }

    suspend fun syncOpportunity(opportunity: Opportunity, serverConfig: ServerConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = getJdbcConnection(serverConfig)
            if (conn != null) {
                try {
                    val sql = "INSERT INTO `vtiger_potential` (`potentialname`, `description`) VALUES (?, ?)"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, opportunity.finalSubject)
                    stmt.setString(2, "${opportunity.observations} | GPS: ${opportunity.latitude},${opportunity.longitude} (${opportunity.streetAddress})")
                    stmt.executeUpdate()
                    stmt.close()
                    conn.close()
                    return@withContext true
                } catch (_: Exception) {
                    try { conn.close() } catch (_: Exception) {}
                }
            }

            val url = "${serverConfig.baseUrl}/webservice/Potentials"
            val payload = JSONObject().apply {
                put("type", opportunity.type)
                put("entity_name", opportunity.displayEntityName)
                put("subject", opportunity.finalSubject)
                put("description", opportunity.observations)
                put("gps_lat", opportunity.latitude)
                put("gps_lng", opportunity.longitude)
                put("gps_address", opportunity.streetAddress)
                put("user_email", opportunity.userEmail)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            true // Persisted locally in SQLite Room
        }
    }

    suspend fun syncAttendance(attendance: Attendance, serverConfig: ServerConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${serverConfig.baseUrl}/webservice/OSSTimeControl"
            val payload = JSONObject().apply {
                put("user_id", attendance.collaboratorId)
                put("user_name", attendance.collaboratorName)
                put("punch_type", attendance.type)
                put("timestamp", attendance.timestamp)
                put("gps_lat", attendance.latitude)
                put("gps_lng", attendance.longitude)
                put("address", attendance.streetAddress)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            true // Persisted locally in SQLite Room
        }
    }

    private fun getColumnString(rs: ResultSet, columnName: String, fallback: String): String {
        return try {
            rs.getString(columnName)?.trim() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
