package com.peonia.nana.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.peonia.nana.R
import com.peonia.nana.data.Nuvem
import com.peonia.nana.data.PreferenciasRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date

/**
 * Confere, a cada 15 minutos, se há algo para avisar:
 *   - mensagens novas nos chats em que a mãe participa;
 *   - conteúdos novos publicados;
 *   - uma dica de bem-estar por dia (entre 10h e 19h).
 *
 * Com "Não incomodar" ligado, nada é avisado das 22h às 7h. Como o que já
 * foi avisado fica anotado, o que chegou à noite é avisado na primeira
 * conferência depois das 7h.
 */
class VerificarNovidadesWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val estado = EstadoAvisos(context)

    override suspend fun doWork(): Result {
        val uid = Nuvem.uid ?: return Result.success()
        val prefs = PreferenciasRepository(applicationContext).preferencias.first()
        if (prefs.naoIncomodar && Agendador.ehNoite()) return Result.success()
        if (!Avisos.podeAvisar(applicationContext)) return Result.success()

        runCatching { if (prefs.respostasApoio) conferirChats(uid) }
        runCatching { if (prefs.novidadesDireitos) conferirConteudos() }
        runCatching { if (prefs.dicasBemEstar) talvezDica() }
        return Result.success()
    }

    private suspend fun conferirChats(uid: String) {
        val chats = Nuvem.chats().get().await().documents
        for (chat in chats) {
            val membro = Nuvem.membros(chat.id).document(uid).get().await()
            if (!membro.exists()) continue

            val lidoEm = membro.getTimestamp("lidoEm")?.toDate()?.time ?: 0L
            val desde = maxOf(lidoEm, estado.ultimoAvisoChat(chat.id))

            val docs = Nuvem.mensagens(chat.id)
                .whereGreaterThan("enviadaEm", Timestamp(Date(desde)))
                .orderBy("enviadaEm", Query.Direction.ASCENDING)
                .limit(50)
                .get().await().documents
            if (docs.isEmpty()) continue

            val maisRecente = docs.mapNotNull { it.getTimestamp("enviadaEm")?.toDate()?.time }.maxOrNull()
            val novas = docs.count { it.getString("autora") != uid }
            if (novas > 0) {
                val nome = chat.getString("nome").orEmpty()
                Avisos.mostrar(
                    applicationContext,
                    Canal.APOIO,
                    ID_CHAT_BASE + (chat.id.hashCode() and 0xFFFF),
                    nome,
                    applicationContext.resources.getQuantityString(R.plurals.aviso_chat, novas, novas, nome),
                )
            }
            if (maisRecente != null) estado.marcarAvisoChat(chat.id, maisRecente)
        }
    }

    private suspend fun conferirConteudos() {
        val docs = Nuvem.bd.collection("conteudos")
            .whereEqualTo("status", "publicado")
            .get().await().documents
        val ids = docs.map { it.id }.toSet()
        val vistos = estado.conteudosVistos()
        estado.marcarConteudosVistos(ids)
        // Na primeira conferência só anota o que já existe, sem avisar.
        if (vistos == null) return

        val novos = docs.filter { it.id !in vistos }
        if (novos.isEmpty()) return
        val texto = if (novos.size == 1) {
            novos.first().getString("titulo").orEmpty()
        } else {
            applicationContext.getString(R.string.aviso_conteudos_varios, novos.size)
        }
        Avisos.mostrar(
            applicationContext,
            Canal.CONTEUDOS,
            ID_CONTEUDOS,
            applicationContext.getString(R.string.aviso_conteudos_titulo),
            texto,
        )
    }

    private fun talvezDica() {
        val hoje = LocalDate.now()
        val agora = LocalTime.now()
        if (estado.ultimaDica() == hoje.toString()) return
        if (agora < LocalTime.of(10, 0) || agora >= LocalTime.of(19, 0)) return

        val dicas = applicationContext.resources.getStringArray(R.array.dicas_bem_estar)
        if (dicas.isEmpty()) return
        val dica = dicas[hoje.dayOfYear % dicas.size]
        Avisos.mostrar(
            applicationContext,
            Canal.BEM_ESTAR,
            ID_DICA,
            applicationContext.getString(R.string.aviso_dica_titulo),
            dica,
        )
        estado.marcarDica(hoje.toString())
    }

    companion object {
        const val ID_CONTEUDOS = 2001
        const val ID_DICA = 3001
        const val ID_CHAT_BASE = 10_000
    }
}
