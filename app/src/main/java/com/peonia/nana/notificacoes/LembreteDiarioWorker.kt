package com.peonia.nana.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.peonia.nana.R
import com.peonia.nana.data.Nuvem
import com.peonia.nana.data.PreferenciasRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Lembrete do diário, no horário escolhido pela mãe.
 *
 * Só avisa se ela ainda não registrou nada hoje. O "Não incomodar" não se
 * aplica aqui: o horário foi ela mesma quem escolheu. Depois de rodar,
 * agenda o lembrete do dia seguinte.
 */
class LembreteDiarioWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferenciasRepository(applicationContext).preferencias.first()
        val uid = Nuvem.uid
        if (uid == null || !prefs.lembreteDiario) return Result.success()

        val jaRegistrou = runCatching {
            Nuvem.diario(uid).document(LocalDate.now().toString()).get().await().exists()
        }.getOrDefault(false)

        if (!jaRegistrou) {
            Avisos.mostrar(
                applicationContext,
                Canal.DIARIO,
                ID_LEMBRETE,
                applicationContext.getString(R.string.aviso_diario_titulo),
                applicationContext.getString(R.string.aviso_diario_texto),
            )
        }
        Agendador.agendarLembrete(applicationContext, prefs.horarioLembrete)
        return Result.success()
    }

    companion object {
        const val ID_LEMBRETE = 1001
    }
}
