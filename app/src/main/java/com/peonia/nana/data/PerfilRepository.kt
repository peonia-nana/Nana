package com.peonia.nana.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Perfil da mãe, guardado no Firestore.
 *
 * O modelo do documento "Banco de Dados – Nanã" separa as informações em três
 * lugares, e este repositório junta tudo numa [Perfil] só para as telas:
 *
 *   maes/{uid}                               cadastro: nome, idade, status
 *   maes/{uid}/perfil_acolhimento/principal  fase, bebê, cidade, autodeclaração
 *   maes/{uid}/preferencias_conteudo/{CAT}   assuntos de interesse
 *
 * O Firestore mantém um cache no aparelho, então as telas continuam
 * funcionando sem internet e a gravação sobe quando a conexão voltar.
 */
class PerfilRepository {

    /**
     * Emite o perfil da mãe logada, e null enquanto o cadastro não existir.
     * Atualiza sozinho quando algo muda no banco.
     */
    fun perfil(uid: String?): Flow<Perfil?> {
        if (uid == null) return flowOf(null)
        return combine(
            documento(uid),
            documentoPerfil(uid),
            categoriasAtivas(uid),
        ) { mae, acolhimento, interesses ->
            if (mae == null) {
                null
            } else {
                Perfil(
                    nome = mae["nome"] as? String ?: "",
                    idade = (mae["idade"] as? Number)?.toInt(),
                    cidade = acolhimento?.get("cidade") as? String ?: "",
                    fase = enumOuNulo<FaseMaternidade>(acolhimento?.get("fasePosParto") as? String),
                    interesses = interesses,
                    autodeclaracao = enumOuNulo<Autodeclaracao>(acolhimento?.get("autodeclaracao") as? String),
                    nomesBebes = acolhimento?.get("nomeBebe") as? String ?: "",
                    quantidadeFilhos = enumOuNulo<QuantidadeFilhos>(
                        acolhimento?.get("quantidadeFilhos") as? String,
                    ),
                )
            }
        }
    }

    /**
     * Grava o perfil inteiro de uma vez.
     *
     * Usa transação porque precisa ler antes de escrever: a data de cadastro
     * é imutável pelas regras do servidor, então numa atualização ela tem que
     * ser reenviada igual. Numa criação, vem do relógio do servidor.
     */
    suspend fun salvar(uid: String, perfil: Perfil) {
        val refMae = Nuvem.mae(uid)
        val refAcolhimento = Nuvem.perfil(uid)

        Nuvem.bd.runTransaction { transacao ->
            val atual = transacao.get(refMae)

            val dadosMae = mutableMapOf<String, Any>(
                "status" to "ativa",
                "atualizadoEm" to FieldValue.serverTimestamp(),
                "versaoPolitica" to VERSAO_POLITICA,
                "dataCadastro" to
                    (atual.get("dataCadastro") ?: FieldValue.serverTimestamp()),
            )
            if (perfil.nome.isNotBlank()) dadosMae["nome"] = perfil.nome.trim()
            perfil.idade?.let { dadosMae["idade"] = it }

            val dadosAcolhimento = mutableMapOf<String, Any>(
                "atualizadoEm" to FieldValue.serverTimestamp(),
            )
            perfil.fase?.let { dadosAcolhimento["fasePosParto"] = it.name }
            if (perfil.cidade.isNotBlank()) dadosAcolhimento["cidade"] = perfil.cidade.trim()
            if (perfil.nomesBebes.isNotBlank()) {
                dadosAcolhimento["nomeBebe"] = perfil.nomesBebes.trim()
            }
            perfil.quantidadeFilhos?.let { dadosAcolhimento["quantidadeFilhos"] = it.name }
            perfil.autodeclaracao?.let { dadosAcolhimento["autodeclaracao"] = it.name }

            transacao.set(refMae, dadosMae)
            transacao.set(refAcolhimento, dadosAcolhimento)

            // Grava as seis categorias, ligadas ou desligadas. Guardar também
            // as desligadas é o que permite distinguir "não quero" de "ainda
            // não escolhi", que é o espírito da Regra 4 do documento.
            Interesse.entries.forEach { interesse ->
                transacao.set(
                    Nuvem.preferencias(uid).document(interesse.name),
                    mapOf(
                        "categoria" to interesse.name,
                        "ativo" to (interesse in perfil.interesses),
                        "atualizadoEm" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            null
        }.await()
    }

    /** Registra uma ação no diário de auditoria, sem guardar valores. */
    suspend fun registrarAuditoria(uid: String, acao: String, campos: List<String> = emptyList()) {
        runCatching {
            Nuvem.auditoria(uid).add(
                mapOf(
                    "acao" to acao,
                    "campos" to campos,
                    "origem" to "android",
                    "registradoEm" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    // -------------------------------------------------------------------------

    private fun documento(uid: String): Flow<Map<String, Any>?> = callbackFlow {
        var registro: ListenerRegistration? = null
        registro = Nuvem.mae(uid).addSnapshotListener { instantaneo, _ ->
            trySend(if (instantaneo != null && instantaneo.exists()) instantaneo.data else null)
        }
        awaitClose { registro?.remove() }
    }

    private fun documentoPerfil(uid: String): Flow<Map<String, Any>?> = callbackFlow {
        var registro: ListenerRegistration? = null
        registro = Nuvem.perfil(uid).addSnapshotListener { instantaneo, _ ->
            trySend(if (instantaneo != null && instantaneo.exists()) instantaneo.data else null)
        }
        awaitClose { registro?.remove() }
    }

    private fun categoriasAtivas(uid: String): Flow<Set<Interesse>> = callbackFlow {
        var registro: ListenerRegistration? = null
        registro = Nuvem.preferencias(uid).addSnapshotListener { instantaneo, _ ->
            val ativas = instantaneo?.documents.orEmpty()
                .filter { it.getBoolean("ativo") == true }
                .mapNotNull { enumOuNulo<Interesse>(it.id) }
                .toSet()
            trySend(ativas)
        }
        awaitClose { registro?.remove() }
    }

    private inline fun <reified T : Enum<T>> enumOuNulo(nome: String?): T? =
        nome?.let { valor -> enumValues<T>().firstOrNull { it.name == valor } }
}
