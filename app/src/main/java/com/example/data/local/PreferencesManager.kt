package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ServerConfig
import com.example.model.SmtpConfig
import com.example.model.TableConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("almaforce_prefs", Context.MODE_PRIVATE)

    private val _serverConfigFlow = MutableStateFlow(loadServerConfig())
    val serverConfigFlow: StateFlow<ServerConfig> = _serverConfigFlow.asStateFlow()

    private val _tableConfigFlow = MutableStateFlow(loadTableConfig())
    val tableConfigFlow: StateFlow<TableConfig> = _tableConfigFlow.asStateFlow()

    private val _smtpConfigFlow = MutableStateFlow(loadSmtpConfig())
    val smtpConfigFlow: StateFlow<SmtpConfig> = _smtpConfigFlow.asStateFlow()

    private val _isLoggedInFlow = MutableStateFlow(isUserLoggedIn())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    fun loadServerConfig(): ServerConfig {
        return ServerConfig(
            ip = prefs.getString(KEY_IP, "192.168.1.100") ?: "192.168.1.100",
            port = prefs.getString(KEY_PORT, "80") ?: "80",
            databaseName = prefs.getString(KEY_DB_NAME, "yetiforce") ?: "yetiforce",
            dbUser = prefs.getString(KEY_DB_USER, "yetiforce_user") ?: "yetiforce_user",
            dbPassword = prefs.getString(KEY_DB_PASS, "") ?: "",
            useHttps = prefs.getBoolean(KEY_USE_HTTPS, false),
            apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        )
    }

    fun saveServerConfig(config: ServerConfig) {
        prefs.edit()
            .putString(KEY_IP, config.ip)
            .putString(KEY_PORT, config.port)
            .putString(KEY_DB_NAME, config.databaseName)
            .putString(KEY_DB_USER, config.dbUser)
            .putString(KEY_DB_PASS, config.dbPassword)
            .putBoolean(KEY_USE_HTTPS, config.useHttps)
            .putString(KEY_API_KEY, config.apiKey)
            .apply()
        _serverConfigFlow.value = config
    }

    fun loadTableConfig(): TableConfig {
        return TableConfig(
            userTable = prefs.getString(KEY_TBL_USER, "vtiger_users") ?: "vtiger_users",
            userNameField = prefs.getString(KEY_FLD_USER_NAME, "first_name, last_name") ?: "first_name, last_name",
            userEmailField = prefs.getString(KEY_FLD_USER_EMAIL, "email1") ?: "email1",
            clientTable = prefs.getString(KEY_TBL_CLIENT, "vtiger_account") ?: "vtiger_account",
            clientNameField = prefs.getString(KEY_FLD_CLIENT_NAME, "accountname") ?: "accountname",
            companyNotificationEmail = prefs.getString(KEY_COMPANY_EMAIL, "rh@almaforce.pt") ?: "rh@almaforce.pt"
        )
    }

    fun saveTableConfig(config: TableConfig) {
        prefs.edit()
            .putString(KEY_TBL_USER, config.userTable)
            .putString(KEY_FLD_USER_NAME, config.userNameField)
            .putString(KEY_FLD_USER_EMAIL, config.userEmailField)
            .putString(KEY_TBL_CLIENT, config.clientTable)
            .putString(KEY_FLD_CLIENT_NAME, config.clientNameField)
            .putString(KEY_COMPANY_EMAIL, config.companyNotificationEmail)
            .apply()
        _tableConfigFlow.value = config
    }

    fun loadSmtpConfig(): SmtpConfig {
        return SmtpConfig(
            senderName = prefs.getString(KEY_SMTP_SENDER_NAME, "AlmaForce CRM") ?: "AlmaForce CRM",
            senderEmail = prefs.getString(KEY_SMTP_SENDER_EMAIL, "notificacoes@almaforce.pt") ?: "notificacoes@almaforce.pt",
            host = prefs.getString(KEY_SMTP_HOST, "smtp.almaforce.pt") ?: "smtp.almaforce.pt",
            port = prefs.getString(KEY_SMTP_PORT, "587") ?: "587",
            requireAuth = prefs.getBoolean(KEY_SMTP_REQ_AUTH, true),
            securityType = prefs.getString(KEY_SMTP_SECURITY, "TLS") ?: "TLS",
            username = prefs.getString(KEY_SMTP_USER, "") ?: "",
            password = prefs.getString(KEY_SMTP_PASS, "") ?: ""
        )
    }

    fun saveSmtpConfig(config: SmtpConfig) {
        prefs.edit()
            .putString(KEY_SMTP_SENDER_NAME, config.senderName)
            .putString(KEY_SMTP_SENDER_EMAIL, config.senderEmail)
            .putString(KEY_SMTP_HOST, config.host)
            .putString(KEY_SMTP_PORT, config.port)
            .putBoolean(KEY_SMTP_REQ_AUTH, config.requireAuth)
            .putString(KEY_SMTP_SECURITY, config.securityType)
            .putString(KEY_SMTP_USER, config.username)
            .putString(KEY_SMTP_PASS, config.password)
            .apply()
        _smtpConfigFlow.value = config
    }

    fun setLoggedIn(isLoggedIn: Boolean, username: String = "", userEmail: String = "", fullName: String = "") {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .putString(KEY_SAVED_USERNAME, username)
            .putString(KEY_SAVED_USER_EMAIL, userEmail)
            .putString(KEY_SAVED_FULL_NAME, fullName)
            .apply()
        _isLoggedInFlow.value = isLoggedIn
    }

    fun isUserLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getSavedUsername(): String = prefs.getString(KEY_SAVED_USERNAME, "") ?: ""
    fun getSavedUserEmail(): String = prefs.getString(KEY_SAVED_USER_EMAIL, "") ?: ""
    fun getSavedFullName(): String = prefs.getString(KEY_SAVED_FULL_NAME, "") ?: ""

    companion object {
        private const val KEY_IP = "pref_ip"
        private const val KEY_PORT = "pref_port"
        private const val KEY_DB_NAME = "pref_db_name"
        private const val KEY_DB_USER = "pref_db_user"
        private const val KEY_DB_PASS = "pref_db_pass"
        private const val KEY_USE_HTTPS = "pref_use_https"
        private const val KEY_API_KEY = "pref_api_key"

        private const val KEY_TBL_USER = "pref_tbl_user"
        private const val KEY_FLD_USER_NAME = "pref_fld_user_name"
        private const val KEY_FLD_USER_EMAIL = "pref_fld_user_email"
        private const val KEY_TBL_CLIENT = "pref_tbl_client"
        private const val KEY_FLD_CLIENT_NAME = "pref_fld_client_name"
        private const val KEY_COMPANY_EMAIL = "pref_company_email"

        private const val KEY_SMTP_SENDER_NAME = "pref_smtp_sender_name"
        private const val KEY_SMTP_SENDER_EMAIL = "pref_smtp_sender_email"
        private const val KEY_SMTP_HOST = "pref_smtp_host"
        private const val KEY_SMTP_PORT = "pref_smtp_port"
        private const val KEY_SMTP_REQ_AUTH = "pref_smtp_req_auth"
        private const val KEY_SMTP_SECURITY = "pref_smtp_security"
        private const val KEY_SMTP_USER = "pref_smtp_user"
        private const val KEY_SMTP_PASS = "pref_smtp_pass"

        private const val KEY_IS_LOGGED_IN = "pref_is_logged_in"
        private const val KEY_SAVED_USERNAME = "pref_saved_username"
        private const val KEY_SAVED_USER_EMAIL = "pref_saved_user_email"
        private const val KEY_SAVED_FULL_NAME = "pref_saved_full_name"
    }
}

