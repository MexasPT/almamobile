package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.remote.AuthResult
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.EmailDispatcher
import com.example.data.remote.EmailMessage
import com.example.data.remote.YetiForceApiService
import com.example.data.repository.AlmaForceRepository
import com.example.location.LocationHelper
import com.example.model.Attendance
import com.example.model.Client
import com.example.model.GpsLocation
import com.example.model.Opportunity
import com.example.model.ServerConfig
import com.example.model.TableConfig
import com.example.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class UiNotification {
    data class Success(val message: String, val emailMessage: EmailMessage? = null) : UiNotification()
    data class Error(val message: String) : UiNotification()
    data class Info(val message: String) : UiNotification()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val apiService = YetiForceApiService()
    val locationHelper = LocationHelper(application)

    val repository = AlmaForceRepository(
        database = database,
        preferencesManager = preferencesManager,
        apiService = apiService,
        locationHelper = locationHelper
    )

    val serverConfig: StateFlow<ServerConfig> = repository.serverConfig
    val tableConfig: StateFlow<TableConfig> = repository.tableConfig
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn

    val currentUser: StateFlow<User?> = repository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = User(
                id = "usr_101",
                userName = preferencesManager.getSavedUsername(),
                firstName = preferencesManager.getSavedFullName(),
                lastName = "",
                email = preferencesManager.getSavedUserEmail(),
                roleName = "Consultor Comercial YetiForce",
                status = "Ativo (vtiger_users)",
                department = "Vendas & Suporte AlmaForce"
            )
        )

    val currentLocation: StateFlow<GpsLocation> = repository.currentLocation
    val isGpsRefreshing: StateFlow<Boolean> = locationHelper.isRefreshing

    val allOpportunities: StateFlow<List<Opportunity>> = repository.allOpportunities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAttendance: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayAttendance: StateFlow<List<Attendance>> = repository.getTodayAttendance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allClients: StateFlow<List<Client>> = repository.allClients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _notifications = MutableSharedFlow<UiNotification>()
    val notifications: SharedFlow<UiNotification> = _notifications.asSharedFlow()

    init {
        // Initial setup and seed check
        viewModelScope.launch {
            repository.syncClients()
            repository.refreshGps()
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun login(username: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginError.value = null
            when (val res = repository.login(username, pass)) {
                is AuthResult.Success -> {
                    _isAuthenticating.value = false
                    _notifications.emit(UiNotification.Success("Bem-vindo, ${res.user.fullName}!"))
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _isAuthenticating.value = false
                    _loginError.value = res.errorMessage
                    _notifications.emit(UiNotification.Error(res.errorMessage))
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        viewModelScope.launch {
            _notifications.emit(UiNotification.Info("Sessão terminada."))
        }
    }

    fun saveServerConfig(config: ServerConfig) {
        repository.saveServerConfig(config)
        viewModelScope.launch {
            _notifications.emit(UiNotification.Success("Configurações do Servidor guardadas com sucesso!"))
        }
    }

    fun saveTableConfig(config: TableConfig) {
        repository.saveTableConfig(config)
        viewModelScope.launch {
            _notifications.emit(UiNotification.Success("Configurações de Tabelas e Emails guardadas!"))
        }
    }

    suspend fun testConnection(config: ServerConfig): ConnectionTestResult {
        return repository.testServerConnection(config)
    }

    fun refreshGps() {
        viewModelScope.launch {
            val loc = repository.refreshGps()
            _notifications.emit(UiNotification.Info("GPS atualizado: ${loc.streetWithNumber}"))
        }
    }

    fun saveOpportunity(
        type: String,
        client: Client?,
        leadCompany: String,
        subject: String,
        customSubject: String?,
        notes: String,
        onComplete: (EmailMessage) -> Unit
    ) {
        viewModelScope.launch {
            val gps = repository.refreshGps()
            val user = currentUser.value
            val personalEmail = user?.email ?: preferencesManager.getSavedUserEmail()
            val userName = user?.fullName ?: preferencesManager.getSavedFullName()

            val opportunity = Opportunity(
                type = type,
                clientId = client?.id,
                clientName = client?.accountName,
                leadCompany = leadCompany.ifBlank { null },
                subject = subject,
                customSubject = customSubject?.ifBlank { null },
                observations = notes,
                latitude = gps.latitude,
                longitude = gps.longitude,
                streetAddress = gps.streetWithNumber,
                userEmail = personalEmail,
                userName = userName,
                timestamp = System.currentTimeMillis()
            )

            val (_, emailMsg) = repository.saveOpportunity(opportunity)
            _notifications.emit(
                UiNotification.Success(
                    message = "Oportunidade guardada com sucesso e enviada para $personalEmail!",
                    emailMessage = emailMsg
                )
            )
            onComplete(emailMsg)
        }
    }

    fun registerAttendancePunch(onComplete: (Attendance, EmailMessage) -> Unit) {
        viewModelScope.launch {
            val todayPunches = repository.getTodayPunchesSync()
            val user = currentUser.value
            val collaboratorName = user?.fullName ?: preferencesManager.getSavedFullName()
            val collaboratorId = user?.id ?: "usr_101"

            val isFirstPunch = todayPunches.isEmpty() || todayPunches.size % 2 == 0
            val type = if (isFirstPunch) "ENTRADA" else "SAIDA"

            val entryTimestamp = if (!isFirstPunch) {
                todayPunches.lastOrNull { it.type == "ENTRADA" }?.timestamp
            } else {
                null
            }

            val (punch, emailMsg) = repository.registerAttendance(
                type = type,
                collaboratorName = collaboratorName,
                collaboratorId = collaboratorId,
                entryTimestamp = entryTimestamp
            )

            val actionDesc = if (type == "ENTRADA") "Entrada registada" else "Saída registada"
            _notifications.emit(
                UiNotification.Success(
                    message = "$actionDesc às ${punch.formattedTimestamp}! Email enviado para ${tableConfig.value.companyNotificationEmail}.",
                    emailMessage = emailMsg
                )
            )
            onComplete(punch, emailMsg)
        }
    }
}
