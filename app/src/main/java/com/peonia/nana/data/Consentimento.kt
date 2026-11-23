package com.peonia.nana.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Consentimento para tratar dados sensíveis.
 *
 * A LGPD classifica origem racial e dados de saúde como dado pessoal sensível
 * (art. 5º, II) e exige consentimento específico e destacado para tratá-los
 * (art. 11, I). As regras do servidor recusam esses campos sem um registro
 * concedido aqui — não é uma formalidade da interface, é uma trava real.
 *
 * Revogar é gravar de novo com concedido = false, o que a lei exige que seja
 * tão simples quanto consentir (art. 8º, §5º).
 */
class ConsentimentoRepository {

    /** Emite quais finalidades estão consentidas neste momento. */
    fun concedidos(uid: String?): Flow<Set<FinalidadeConsentimento>> {
        if (uid == null) return flowOf(emptySet())
        return callbackFlow {
            var registro: ListenerRegistration? = null
            registro = Nuvem.consentimentos(uid).addSnapshotListener { instantaneo, _ ->
                val ativos = instantaneo?.documents.orEmpty()
                    .filter { it.getBoolean("concedido") == true }
                    .mapNotNull { doc ->
                        FinalidadeConsentimento.entries.firstOrNull { it.chave == doc.id }
                    }
                    .toSet()
                trySend(ativos)
            }
            awaitClose { registro?.remove() }
        }
    }

    /**
     * Registra a decisão e guarda uma cópia imutável no histórico.
     *
     * O histórico é o que comprova o que foi aceito e quando, como exige o
     * art. 8º, §1º. Nem a própria dona consegue alterá-lo depois.
     */
    suspend fun registrar(
        uid: String,
        finalidade: FinalidadeConsentimento,
        concedido: Boolean,
    ) {
        val corpo = mapOf(
            "finalidade" to finalidade.chave,
            "concedido" to concedido,
            "versaoTexto" to VERSAO_POLITICA,
            "registradoEm" to FieldValue.serverTimestamp(),
        )
        Nuvem.consentimentos(uid).document(finalidade.chave).set(corpo).await()
        runCatching { Nuvem.consentimentosHistorico(uid).add(corpo).await() }
    }
}
