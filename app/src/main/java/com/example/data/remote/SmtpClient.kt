package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.model.SmtpConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

sealed class SmtpResult {
    data class Success(val message: String, val details: String = "") : SmtpResult()
    data class Error(val errorMessage: String, val details: String = "") : SmtpResult()
}

class SmtpClient {

    companion object {
        private const val TAG = "SmtpClient"
        private const val TIMEOUT_MS = 10000 // 10 seconds timeout
    }

    suspend fun sendEmail(
        config: SmtpConfig,
        recipient: String,
        subject: String,
        body: String
    ): SmtpResult = withContext(Dispatchers.IO) {
        if (config.host.isBlank()) {
            return@withContext SmtpResult.Error(
                errorMessage = "Host SMTP não configurado",
                details = "Por favor preencha o Host SMTP nas Configurações da aplicação."
            )
        }
        if (recipient.isBlank()) {
            return@withContext SmtpResult.Error(
                errorMessage = "Destinatário em branco",
                details = "O endereço de email de destino não pode estar vazio."
            )
        }

        val portInt = config.port.toIntOrNull() ?: if (config.securityType == "SSL") 465 else 587
        val host = config.host.trim()

        var rawSocket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            Log.d(TAG, "Connecting to SMTP $host:$portInt (Security: ${config.securityType})...")

            // 1. Initial Socket Connection
            if (config.securityType == "SSL") {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket() as SSLSocket
                sslSocket.connect(InetSocketAddress(host, portInt), TIMEOUT_MS)
                sslSocket.soTimeout = TIMEOUT_MS
                sslSocket.startHandshake()
                rawSocket = sslSocket
            } else {
                val plainSocket = Socket()
                plainSocket.connect(InetSocketAddress(host, portInt), TIMEOUT_MS)
                plainSocket.soTimeout = TIMEOUT_MS
                rawSocket = plainSocket
            }

            reader = BufferedReader(InputStreamReader(rawSocket.getInputStream(), StandardCharsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(rawSocket.getOutputStream(), StandardCharsets.UTF_8), true)

            // Read banner (220)
            val banner = readSmtpResponse(reader)
            if (!banner.startsWith("220")) {
                throw Exception("Servidor SMTP respondeu com erro inicial: $banner")
            }

            // EHLO
            writer.print("EHLO localhost\r\n")
            writer.flush()
            val ehloResp = readSmtpResponse(reader)
            if (!ehloResp.startsWith("250")) {
                // Fallback to HELO
                writer.print("HELO localhost\r\n")
                writer.flush()
                readSmtpResponse(reader)
            }

            // STARTTLS if configured
            if (config.securityType == "TLS") {
                writer.print("STARTTLS\r\n")
                writer.flush()
                val tlsResp = readSmtpResponse(reader)
                if (tlsResp.startsWith("220")) {
                    val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    val sslSocket = sslFactory.createSocket(rawSocket, host, portInt, true) as SSLSocket
                    sslSocket.soTimeout = TIMEOUT_MS
                    sslSocket.startHandshake()

                    rawSocket = sslSocket
                    reader = BufferedReader(InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8))
                    writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true)

                    // Re-send EHLO after TLS upgrade
                    writer.print("EHLO localhost\r\n")
                    writer.flush()
                    readSmtpResponse(reader)
                } else {
                    Log.w(TAG, "STARTTLS not accepted by server ($tlsResp), proceeding...")
                }
            }

            // AUTH if required
            if (config.requireAuth && config.username.isNotBlank()) {
                writer.print("AUTH LOGIN\r\n")
                writer.flush()
                val authLoginResp = readSmtpResponse(reader)

                if (authLoginResp.startsWith("334")) {
                    // Send Base64 Username
                    val userB64 = Base64.encodeToString(config.username.trim().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                    writer.print("$userB64\r\n")
                    writer.flush()
                    val userResp = readSmtpResponse(reader)

                    if (userResp.startsWith("334")) {
                        // Send Base64 Password
                        val passB64 = Base64.encodeToString(config.password.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                        writer.print("$passB64\r\n")
                        writer.flush()
                        val passResp = readSmtpResponse(reader)

                        if (!passResp.startsWith("235")) {
                            throw Exception("Falha na autenticação SMTP: $passResp (Verifique Utilizador/Password)")
                        }
                    } else {
                        throw Exception("Erro ao submeter utilizador SMTP: $userResp")
                    }
                } else if (!authLoginResp.startsWith("235")) {
                    // Try AUTH PLAIN as fallback
                    val plainAuthStr = "\u0000${config.username.trim()}\u0000${config.password}"
                    val plainB64 = Base64.encodeToString(plainAuthStr.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                    writer.print("AUTH PLAIN $plainB64\r\n")
                    writer.flush()
                    val plainResp = readSmtpResponse(reader)
                    if (!plainResp.startsWith("235")) {
                        throw Exception("Autenticação recusada pelo servidor SMTP: $plainResp")
                    }
                }
            }

            // MAIL FROM
            val fromEmail = if (config.senderEmail.isNotBlank()) config.senderEmail.trim() else config.username.trim().ifBlank { "noreply@almaforce.pt" }
            writer.print("MAIL FROM:<$fromEmail>\r\n")
            writer.flush()
            val mailFromResp = readSmtpResponse(reader)
            if (!mailFromResp.startsWith("250")) {
                throw Exception("Erro no endereço do remetente ($fromEmail): $mailFromResp")
            }

            // RCPT TO
            val cleanRecipient = recipient.trim()
            writer.print("RCPT TO:<$cleanRecipient>\r\n")
            writer.flush()
            val rcptResp = readSmtpResponse(reader)
            if (!rcptResp.startsWith("250") && !rcptResp.startsWith("251")) {
                throw Exception("Erro no endereço do destinatário ($cleanRecipient): $rcptResp")
            }

            // DATA
            writer.print("DATA\r\n")
            writer.flush()
            val dataResp = readSmtpResponse(reader)
            if (!dataResp.startsWith("354")) {
                throw Exception("Servidor rejeitou comando DATA: $dataResp")
            }

            // Send Email Content / MIME Headers
            val dateHeader = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val encodedSubject = "=?UTF-8?B?" + Base64.encodeToString(subject.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP) + "?="
            val senderDisplay = if (config.senderName.isNotBlank()) "${config.senderName} <$fromEmail>" else fromEmail

            writer.print("Date: $dateHeader\r\n")
            writer.print("From: $senderDisplay\r\n")
            writer.print("To: <$cleanRecipient>\r\n")
            writer.print("Subject: $encodedSubject\r\n")
            writer.print("MIME-Version: 1.0\r\n")
            writer.print("Content-Type: text/plain; charset=UTF-8\r\n")
            writer.print("Content-Transfer-Encoding: 8bit\r\n")
            writer.print("X-Mailer: AlmaForce CRM Android v1.0\r\n")
            writer.print("\r\n") // End of headers

            // Normalize line endings in body
            val normalizedBody = body.replace("\r\n", "\n").replace("\n", "\r\n")
            writer.print(normalizedBody)
            writer.print("\r\n.\r\n") // End of message marker
            writer.flush()

            val endResp = readSmtpResponse(reader)
            if (!endResp.startsWith("250")) {
                throw Exception("Falha ao entregar conteúdo do email: $endResp")
            }

            // QUIT
            try {
                writer.print("QUIT\r\n")
                writer.flush()
                readSmtpResponse(reader)
            } catch (_: Exception) {}

            Log.i(TAG, "Email successfully sent via SMTP to $cleanRecipient")
            SmtpResult.Success(
                message = "Email enviado com sucesso via SMTP para $cleanRecipient",
                details = "Servidor: $host:$portInt (${config.securityType}) • Resposta: $endResp"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email via SMTP", e)
            val msg = e.localizedMessage ?: "Erro desconhecido ao comunicar com servidor SMTP."
            SmtpResult.Error(
                errorMessage = "Falha no envio SMTP para $recipient",
                details = msg
            )
        } finally {
            try { writer?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            try { rawSocket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun testSmtpConnection(
        config: SmtpConfig,
        testRecipient: String
    ): SmtpResult = withContext(Dispatchers.IO) {
        val testSubj = "[AlmaForce] Teste de Ligação SMTP Concluído"
        val testBody = buildString {
            appendLine("=== TESTE DE CONFIGURAÇÃO SMTP ALMAFORCE APP ===")
            appendLine()
            appendLine("A ligação ao servidor SMTP e a autenticação foram concluídas com sucesso!")
            appendLine()
            appendLine("• Host SMTP: ${config.host}:${config.port}")
            appendLine("• Criptografia: ${config.securityType}")
            appendLine("• Autenticação: ${if (config.requireAuth) "Ativa (${config.username})" else "Desativada"}")
            appendLine("• Remetente: ${config.senderName} (${config.senderEmail})")
            appendLine("• Data do Teste: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("AlmaForce APP - YetiForce CRM Integration")
        }

        val targetEmail = if (testRecipient.isNotBlank()) testRecipient else config.senderEmail.ifBlank { config.username }
        if (targetEmail.isBlank()) {
            return@withContext SmtpResult.Error(
                errorMessage = "Destinatário de teste não definido",
                details = "Introduza um email de teste ou configure o Email Remetente."
            )
        }

        sendEmail(config, targetEmail, testSubj, testBody)
    }

    private fun readSmtpResponse(reader: BufferedReader): String {
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line).append("\n")
            // In SMTP multiline responses: "250-..." indicates more lines, "250 ..." indicates last line
            if (line!!.length >= 4 && line!![3] == ' ') {
                break
            }
            if (line!!.length == 3) {
                break
            }
        }
        return response.toString().trim()
    }
}
