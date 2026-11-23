package com.peonia.nana.ui.apoio

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.peonia.nana.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBR = Locale("pt", "BR")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", ptBR)
private val formatoDiaCurto = DateTimeFormatter.ofPattern("dd/MM", ptBR)
private val formatoDiaLongo = DateTimeFormatter.ofPattern("d 'de' MMMM", ptBR)

fun Instant.dia(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()

fun hora(instante: Instant): String =
    instante.atZone(ZoneId.systemDefault()).format(formatoHora)

/** Hora se for hoje, "Ontem", ou a data curta. Usado na lista de chats. */
@Composable
fun horaCurta(instante: Instant): String {
    val dia = instante.dia()
    val hoje = LocalDate.now()
    return when (dia) {
        hoje -> hora(instante)
        hoje.minusDays(1) -> stringResource(R.string.chat_ontem)
        else -> dia.format(formatoDiaCurto)
    }
}

/** Rótulo do separador de dias dentro da conversa. */
@Composable
fun rotuloDoDia(dia: LocalDate): String {
    val hoje = LocalDate.now()
    return when (dia) {
        hoje -> stringResource(R.string.chat_hoje)
        hoje.minusDays(1) -> stringResource(R.string.chat_ontem)
        else -> dia.format(formatoDiaLongo)
    }
}
