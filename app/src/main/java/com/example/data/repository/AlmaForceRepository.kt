package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.remote.AuthResult
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.EmailDispatcher
import com.example.data.remote.EmailMessage
import com.example.data.remote.SmtpClient
import com.example.data.remote.SmtpResult
import com.example.data.remote.YetiForceApiService
import com.example.location.LocationHelper
import com.example.model.Attendance
import com.example.model.Client
import com.example.model.GpsLocation
import com.example.model.Opportunity
import com.example.model.ServerConfig
import com.example.model.SmtpConfig
import com.example.model.TableConfig
import com.example.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar

class AlmaForceRepository(
    private val database: AppDatabase,
    private val preferencesManager: PreferencesManager,
    private val apiService: YetiForceApiService,
    private val smtpClient: SmtpClient,
    val locationHelper: LocationHelper
) {
    val serverConfig: StateFlow<ServerConfig> = preferencesManager.serverConfigFlow
    val tableConfig: StateFlow<TableConfig> = preferencesManager.tableConfigFlow
    val smtpConfig: StateFlow<SmtpConfig> = preferencesManager.smtpConfigFlow
    val isLoggedIn: StateFlow<Boolean> = preferencesManager.isLoggedInFlow
    val currentUser: Flow<User?> = database.userDao().getCachedUser()

    val allOpportunities: Flow<List<Opportunity>> = database.opportunityDao().getAllOpportunities()
    val allAttendance: Flow<List<Attendance>> = database.attendanceDao().getAllAttendance()
    val allClients: Flow<List<Client>> = database.clientDao().getAllClients()
    val currentLocation: StateFlow<GpsLocation> = locationHelper.currentLocation

    fun searchClients(query: String): Flow<List<Client>> = database.clientDao().searchClients(query)

    fun getTodayAttendance(): Flow<List<Attendance>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return database.attendanceDao().getTodayPunches(cal.timeInMillis)
    }

    suspend fun getTodayPunchesSync(): List<Attendance> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return database.attendanceDao().getTodayPunchesSync(cal.timeInMillis)
    }

    suspend fun testServerConnection(config: ServerConfig): ConnectionTestResult {
        return apiService.testConnection(config)
    }

    suspend fun testSmtp(config: SmtpConfig, testRecipient: String): SmtpResult {
        return smtpClient.testSmtpConnection(config, testRecipient)
    }

    fun saveServerConfig(config: ServerConfig) {
        preferencesManager.saveServerConfig(config)
    }

    fun saveTableConfig(config: TableConfig) {
        preferencesManager.saveTableConfig(config)
    }

    fun saveSmtpConfig(config: SmtpConfig) {
        preferencesManager.saveSmtpConfig(config)
    }

    suspend fun login(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val currentServer = serverConfig.value
        val currentTable = tableConfig.value
        val result = apiService.authenticate(username, password, currentServer, currentTable)
        if (result is AuthResult.Success) {
            database.userDao().insertUser(result.user)
            preferencesManager.setLoggedIn(
                isLoggedIn = true,
                username = result.user.userName,
                userEmail = result.user.email,
                fullName = result.user.fullName
            )
            // Sync real clients from YetiForce CRM
            syncClients()
        }
        result
    }

    fun logout() {
        preferencesManager.setLoggedIn(false)
    }

    suspend fun syncClients() = withContext(Dispatchers.IO) {
        val clients = apiService.fetchClients(serverConfig.value, tableConfig.value)
        if (clients.isNotEmpty()) {
            database.clientDao().insertClients(clients)
        }
    }

    suspend fun saveOpportunity(opportunity: Opportunity): Triple<Long, EmailMessage, SmtpResult> = withContext(Dispatchers.IO) {
        val id = database.opportunityDao().insertOpportunity(opportunity)
        val savedOpp = opportunity.copy(id = id)

        // Sync with YetiForce
        apiService.syncOpportunity(savedOpp, serverConfig.value)

        // Build Email Notification for Personal Email
        val emailMessage = EmailDispatcher.generateOpportunityEmailContent(savedOpp)

        // Send directly via SMTP in background
        val smtpResult = if (opportunity.userEmail.isNotBlank()) {
            smtpClient.sendEmail(
                config = smtpConfig.value,
                recipient = opportunity.userEmail,
                subject = emailMessage.subject,
                body = emailMessage.body
            )
        } else {
            SmtpResult.Error("Email pessoal do utilizador em branco", "Defina o email nas configurações ou perfil.")
        }

        Triple(id, emailMessage, smtpResult)
    }

    suspend fun registerAttendance(
        type: String,
        collaboratorName: String,
        collaboratorId: String,
        entryTimestamp: Long? = null
    ): Triple<Attendance, EmailMessage, SmtpResult> = withContext(Dispatchers.IO) {
        val gps = locationHelper.refreshLocation()
        val companyEmail = tableConfig.value.companyNotificationEmail

        val record = Attendance(
            type = type,
            collaboratorName = collaboratorName,
            collaboratorId = collaboratorId,
            companyEmail = companyEmail,
            timestamp = System.currentTimeMillis(),
            entryTimestamp = entryTimestamp,
            latitude = gps.latitude,
            longitude = gps.longitude,
            streetAddress = gps.streetWithNumber,
            isSynced = true,
            emailSent = true
        )

        val id = database.attendanceDao().insertAttendance(record)
        val savedAttendance = record.copy(id = id)

        // Sync with YetiForce CRM
        apiService.syncAttendance(savedAttendance, serverConfig.value)

        // Build Email Notification for Company Email
        val emailMessage = EmailDispatcher.generateAttendanceEmailContent(savedAttendance)

        // Send directly via SMTP in background
        val smtpResult = if (companyEmail.isNotBlank()) {
            smtpClient.sendEmail(
                config = smtpConfig.value,
                recipient = companyEmail,
                subject = emailMessage.subject,
                body = emailMessage.body
            )
        } else {
            SmtpResult.Error("Email da empresa em branco", "Defina o email da empresa nas configurações.")
        }

        Triple(savedAttendance, emailMessage, smtpResult)
    }

    suspend fun refreshGps(): GpsLocation {
        return locationHelper.refreshLocation()
    }
}
