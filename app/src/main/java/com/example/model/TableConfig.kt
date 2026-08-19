package com.example.model

data class TableConfig(
    val userTable: String = "vtiger_users",
    val userNameField: String = "first_name, last_name",
    val userEmailField: String = "email1",
    val clientTable: String = "vtiger_account",
    val clientNameField: String = "accountname",
    val companyNotificationEmail: String = "notificacoes@empresa.com"
)
