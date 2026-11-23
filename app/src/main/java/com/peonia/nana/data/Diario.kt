package com.peonia.nana.data

import androidx.annotation.StringRes
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.peonia.nana.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Sentimentos do diário e do check-in da tela inicial.
 * A mesma lista serve aos dois lugares, para não haver duas verdades.
 */
enum class Humor(@StringRes val rotulo: Int) {
    BEM(R.string.humor_bem),
    CALMA(R.string.humor_calma),
    ANSIOSA(R.string.humor_ansiosa),
    CANSADA(R.string.humor_cansada),
    TRISTE(R.string.humor_triste);

    companion object {
        fun porNome(nome: String?): Humor? = entries.firstOrNull { it.name == nome }
    }
}

/**
 * Diário de cuidado, guardado em maes/{uid}/diario_cuidado.
 *
 * O identificador de cada registro é a própria data, no formato 2026-11-22.
 * Isso garante um registro por dia sem precisar consultar o banco antes de
 * gravar: registrar de novo no mesmo dia simplesmente substitui o anterior,
 * porque o sentimento pode mudar ao longo do dia.
 *
 * O campo `sintomas` existe no modelo e é dado de saúde: as regras do servidor
 * só o aceitam com o consentimento `dados_de_saude`. O app ainda não coleta
 * sintomas, então aqui só grava o sentimento.
 */
class DiarioRepository {

    fun registros(uid: String?): Flow<Map<LocalDate, Humor>> {
        if (uid == null) return flowOf(emptyMap())
        return callbackFlow {
            var registro: ListenerRegistration? = null
            registro = Nuvem.diario(uid).addSnapshotListener { instantaneo, _ ->
                val mapa = instantaneo?.documents.orEmpty().mapNotNull { doc ->
                    val data = runCatching { LocalDate.parse(doc.id) }.getOrNull()
                    val humor = Humor.porNome(doc.getString("sentimento"))
                    if (data != null && humor != null) data to humor else null
                }.toMap()
                trySend(mapa)
            }
            awaitClose { registro?.remove() }
        }
    }

    suspend fun registrar(uid: String, data: LocalDate, humor: Humor) {
        val ref = Nuvem.diario(uid).document(data.toString())
        val existente = runCatching { ref.get().await() }.getOrNull()
        val criadoEm = existente?.get("criadoEm") ?: FieldValue.serverTimestamp()
        ref.set(
            mapOf(
                "dataRegistro" to data.toString(),
                "sentimento" to humor.name,
                "criadoEm" to criadoEm,
                "atualizadoEm" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun apagar(uid: String, data: LocalDate) {
        Nuvem.diario(uid).document(data.toString()).delete().await()
    }
}
