package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.model.Attendance
import com.example.model.Opportunity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailDispatcher {

    fun generateOpportunityEmailContent(opportunity: Opportunity): EmailMessage {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date(opportunity.timestamp))
        val mapLink = "https://www.google.com/maps/search/?api=1&query=${opportunity.latitude},${opportunity.longitude}"

        val subject = "[AlmaForce CRM] Nova Oportunidade: ${opportunity.displayEntityName} - ${opportunity.finalSubject}"

        val body = buildString {
            appendLine("=== ALMAFORCE CRM - REGISTO DE OPORTUNIDADE ===")
            appendLine()
            appendLine("📅 Data & Hora: $dateStr")
            appendLine("👤 Utilizador: ${opportunity.userName}")
            appendLine("📋 Tipo: ${opportunity.type}")
            appendLine("🏢 ${if (opportunity.type == "Cliente") "Cliente" else "Empresa (Lead)"}: ${opportunity.displayEntityName}")
            appendLine("🎯 Assunto: ${opportunity.finalSubject}")
            appendLine()
            appendLine("📝 Observações:")
            appendLine(opportunity.observations.ifBlank { "(Sem observações adicionais)" })
            appendLine()
            appendLine("📍 LOCALIZAÇÃO GPS:")
            appendLine("• Rua / Morada: ${opportunity.streetAddress}")
            appendLine("• Coordenadas: ${opportunity.latitude}, ${opportunity.longitude}")
            appendLine("• Ver no Mapa: $mapLink")
            appendLine()
            appendLine("──────────────────────────────────────────")
            appendLine("Enviado automaticamente pela AlmaForce APP.")
        }

        return EmailMessage(
            recipient = opportunity.userEmail,
            subject = subject,
            body = body,
            mapsUrl = mapLink
        )
    }

    fun generateAttendanceEmailContent(attendance: Attendance): EmailMessage {
        val punchType = if (attendance.type == "ENTRADA") "REGISTO DE ENTRADA" else "REGISTO DE SAÍDA"
        val mapLink = "https://www.google.com/maps/search/?api=1&query=${attendance.latitude},${attendance.longitude}"

        val subject = "[AlmaForce Assiduidade] $punchType - ${attendance.collaboratorName} (${attendance.formattedTimestamp})"

        val body = buildString {
            appendLine("=== ALMAFORCE CRM - CONTROLO DE ASSIDUIDADE ===")
            appendLine()
            appendLine("👤 Nome do Colaborador: ${attendance.collaboratorName}")
            appendLine("⚡ Tipo de Picagem: $punchType")
            appendLine("⏰ Data de ${if (attendance.type == "ENTRADA") "Entrada" else "Saída"}: ${attendance.formattedTimestamp}")
            if (attendance.type == "SAIDA" && attendance.formattedEntryTimestamp != null) {
                appendLine("🕒 Data de Entrada correspondente: ${attendance.formattedEntryTimestamp}")
            }
            appendLine()
            appendLine("📍 LOCALIZAÇÃO GPS DA PICAGEM:")
            appendLine("• Nome da Rua / Morada: ${attendance.streetAddress}")
            appendLine("• Coordenadas: ${attendance.latitude}, ${attendance.longitude}")
            appendLine("• Ver no Mapa: $mapLink")
            appendLine()
            appendLine("──────────────────────────────────────────")
            appendLine("Registo submetido com autenticação YetiForce CRM via AlmaForce APP.")
        }

        return EmailMessage(
            recipient = attendance.companyEmail,
            subject = subject,
            body = body,
            mapsUrl = mapLink
        )
    }

    fun dispatchEmailIntent(context: Context, emailMessage: EmailMessage): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(emailMessage.recipient)}")
                putExtra(Intent.EXTRA_SUBJECT, emailMessage.subject)
                putExtra(Intent.EXTRA_TEXT, emailMessage.body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar Notificação por Email").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            Log.e("EmailDispatcher", "Error launching email intent", e)
            false
        }
    }
}

data class EmailMessage(
    val recipient: String,
    val subject: String,
    val body: String,
    val mapsUrl: String
)
