package com.peonia.nana.data

import androidx.annotation.StringRes
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.peonia.nana.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.Instant
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
/**
 * O registro de um dia: o sentimento tocado no Início e/ou o texto escrito no
 * Diário. Qualquer um dos dois pode faltar, mas não os dois.
 */
data class RegistroDiario(
    val data: LocalDate,
    val humor: Humor?,
    val texto: String?,
    val atualizadoEm: Instant?,
)

class DiarioRepository {

    fun registros(uid: String?): Flow<Map<LocalDate, RegistroDiario>> {
        if (uid == null) return flowOf(emptyMap())
        return callbackFlow {
            var registro: ListenerRegistration? = null
            registro = Nuvem.diario(uid).addSnapshotListener { instantaneo, _ ->
                val mapa = instantaneo?.documents.orEmpty().mapNotNull { doc ->
                    val data = runCatching { LocalDate.parse(doc.id) }.getOrNull()
                        ?: return@mapNotNull null
                    val humor = Humor.porNome(doc.getString("sentimento"))
                    val texto = doc.getString("descricao")?.takeIf { it.isNotBlank() }
                    if (humor == null && texto == null) return@mapNotNull null
                    val em = doc.getTimestamp(
                        "atualizadoEm",
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )?.toDate()?.toInstant()
                    data to RegistroDiario(data, humor, texto, em)
                }.toMap()
                trySend(mapa)
            }
            awaitClose { registro?.remove() }
        }
    }

    /**
     * Grava o sentimento do dia (check-in do Início). Usa merge para não
     * apagar o texto que a mãe já tenha escrito no mesmo dia.
     */
    suspend fun registrar(uid: String, data: LocalDate, humor: Humor) {
        gravar(uid, data, mapOf("sentimento" to humor.name))
    }

    /** Grava o texto do dia, sem mexer no sentimento. Até 2000 caracteres. */
    suspend fun salvarTexto(uid: String, data: LocalDate, texto: String) {
        gravar(uid, data, mapOf("descricao" to texto.trim().take(LIMITE_TEXTO)))
    }

    /**
     * Apaga só o texto do dia. Se não sobrar nada no registro (nenhum
     * sentimento), apaga o registro inteiro: as regras recusam registro vazio.
     */
    suspend fun apagarTexto(uid: String, data: LocalDate) {
        val ref = Nuvem.diario(uid).document(data.toString())
        val atual = ref.get().await()
        if (atual.getString("sentimento") == null) {
            ref.delete().await()
        } else {
            ref.update(
                mapOf(
                    "descricao" to FieldValue.delete(),
                    "atualizadoEm" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    suspend fun apagar(uid: String, data: LocalDate) {
        Nuvem.diario(uid).document(data.toString()).delete().await()
    }

    private suspend fun gravar(uid: String, data: LocalDate, campos: Map<String, Any>) {
        val ref = Nuvem.diario(uid).document(data.toString())
        val existente = runCatching { ref.get().await() }.getOrNull()
        val criadoEm = existente?.takeIf { it.exists() }?.get("criadoEm")
            ?: FieldValue.serverTimestamp()
        ref.set(
            campos + mapOf(
                "dataRegistro" to data.toString(),
                "criadoEm" to criadoEm,
                "atualizadoEm" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    companion object {
        const val LIMITE_TEXTO = 2000
    }
}
