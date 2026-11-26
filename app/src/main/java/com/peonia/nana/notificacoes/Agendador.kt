package com.peonia.nana.notificacoes

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.peonia.nana.data.Nuvem
import com.peonia.nana.data.PreferenciasApp
import com.peonia.nana.data.PreferenciasRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Liga e desliga os avisos conforme as escolhas da tela Notificações.
 *
 * Não há servidor enviando avisos (o plano gratuito do Firebase não tem
 * Cloud Functions). Por isso:
 *   - o lembrete do diário é agendado no próprio celular, no horário escolhido;
 *   - mensagens do chat, conteúdos novos e dicas são conferidos pelo celular
 *     a cada 15 minutos, que é o intervalo mínimo que o Android permite.
 *     O aviso pode chegar com alguns minutos de atraso.
 */
object Agendador {

    private const val LEMBRETE = "lembrete_diario"
    private const val NOVIDADES = "verificar_novidades"

    /** Recalcula tudo a partir das preferências salvas. */
    suspend fun aplicar(context: Context) {
        val prefs = PreferenciasRepository(context).preferencias.first()
        aplicar(context, prefs)
    }

    fun aplicar(context: Context, prefs: PreferenciasApp) {
        val wm = WorkManager.getInstance(context)
        if (Nuvem.uid == null) {
            cancelarTudo(context)
            return
        }
        Avisos.criarCanais(context)

        if (prefs.lembreteDiario) agendarLembrete(context, prefs.horarioLembrete)
        else wm.cancelUniqueWork(LEMBRETE)

        if (prefs.respostasApoio || prefs.novidadesDireitos || prefs.dicasBemEstar) {
            val pedido = PeriodicWorkRequestBuilder<VerificarNovidadesWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            wm.enqueueUniquePeriodicWork(NOVIDADES, ExistingPeriodicWorkPolicy.UPDATE, pedido)
        } else {
            wm.cancelUniqueWork(NOVIDADES)
        }
    }

    /** Agenda o próximo lembrete para o horário escolhido (hoje ou amanhã). */
    fun agendarLembrete(context: Context, horario: String) {
        val hora = runCatching { LocalTime.parse(horario) }.getOrDefault(LocalTime.of(20, 0))
        val agora = LocalDateTime.now()
        var proximo = agora.toLocalDate().atTime(hora)
        if (!proximo.isAfter(agora)) proximo = proximo.plusDays(1)
        val espera = Duration.between(agora, proximo).toMillis()

        val pedido = OneTimeWorkRequestBuilder<LembreteDiarioWorker>()
            .setInitialDelay(espera, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(LEMBRETE, ExistingWorkPolicy.REPLACE, pedido)
    }

    /** Ao sair da conta ou apagar tudo: nenhum aviso deve continuar chegando. */
    fun cancelarTudo(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEMBRETE)
        wm.cancelUniqueWork(NOVIDADES)
        EstadoAvisos(context).limpar()
        Avisos.limparTudo(context)
    }

    /** Não incomodar: das 22h às 7h. */
    fun ehNoite(agora: LocalTime = LocalTime.now()): Boolean =
        agora >= LocalTime.of(22, 0) || agora < LocalTime.of(7, 0)
}
