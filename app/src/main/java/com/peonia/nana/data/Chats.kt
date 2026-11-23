package com.peonia.nana.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Chats em grupo da aba Apoio.
 *
 * Estrutura no Firestore (ver firestore.rules, "Conversas e Mensagens"):
 *
 *   chats/{chatId}                    nome, participantes
 *   ├── membros/{uid}                 quem participa
 *   ├── mensagens/{uid_entrada_n}     o que foi escrito
 *   └── digitando/{uid}               o "..." de quem está escrevendo
 *
 * Os chats são criados pela equipe (admin/criar-chat.js), nunca pelo app.
 * Não há inteligência artificial nem resposta automática: toda mensagem é
 * escrita por uma pessoa.
 */
data class Chat(
    val id: String,
    val nome: String,
    val participantes: Int,
)

/** O documento membros/{uid} da própria mãe num chat. */
data class Participacao(
    val nome: String?,
    val entrouEm: Timestamp?,
    val lidoEm: Timestamp?,
    val enviadas: Long,
)

data class Mensagem(
    val id: String,
    val autora: String,
    val texto: String,
    val enviadaEm: Instant,
    val curtidas: List<String>,
)

class ChatRepository {

    fun chats(): Flow<List<Chat>> = callbackFlow {
        val registro = Nuvem.chats().addSnapshotListener { instantaneo, _ ->
            val lista = instantaneo?.documents.orEmpty().mapNotNull { doc ->
                val nome = doc.getString("nome") ?: return@mapNotNull null
                Chat(doc.id, nome, (doc.getLong("participantes") ?: 0L).toInt().coerceAtLeast(0))
            }.sortedBy { it.nome.lowercase() }
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    fun chat(chatId: String): Flow<Chat?> = callbackFlow {
        val registro = Nuvem.chat(chatId).addSnapshotListener { doc, _ ->
            val nome = doc?.getString("nome")
            trySend(
                if (doc == null || nome == null) null
                else Chat(doc.id, nome, (doc.getLong("participantes") ?: 0L).toInt().coerceAtLeast(0)),
            )
        }
        awaitClose { registro.remove() }
    }

    /** null quando a mãe não participa do chat. */
    fun participacao(chatId: String, uid: String): Flow<Participacao?> = callbackFlow {
        val registro = Nuvem.membros(chatId).document(uid).addSnapshotListener { doc, erro ->
            if (erro != null) return@addSnapshotListener
            trySend(
                if (doc == null || !doc.exists()) null
                else Participacao(
                    nome = doc.getString("nome"),
                    // Enquanto a gravação não chega ao servidor, a hora ainda
                    // não existe: fica null e o envio espera.
                    entrouEm = doc.getTimestamp("entrouEm"),
                    lidoEm = doc.getTimestamp(
                        "lidoEm",
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ),
                    enviadas = doc.getLong("enviadas") ?: 0L,
                ),
            )
        }
        awaitClose { registro.remove() }
    }

    /** Nome exibido de cada participante: uid → nome (null = não mostrar). */
    fun nomes(chatId: String): Flow<Map<String, String?>> = callbackFlow {
        val registro = Nuvem.membros(chatId).addSnapshotListener { instantaneo, _ ->
            trySend(instantaneo?.documents.orEmpty().associate { it.id to it.getString("nome") })
        }
        awaitClose { registro.remove() }
    }

    /** As mensagens mais recentes, da mais antiga para a mais nova. */
    fun mensagens(chatId: String, limite: Long = 200): Flow<List<Mensagem>> = callbackFlow {
        val registro = Nuvem.mensagens(chatId)
            .orderBy("enviadaEm", Query.Direction.DESCENDING)
            .limit(limite)
            .addSnapshotListener { instantaneo, _ ->
                val lista = instantaneo?.documents.orEmpty()
                    .mapNotNull { paraMensagem(it) }
                    .sortedBy { it.enviadaEm }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    fun ultimaMensagem(chatId: String): Flow<Mensagem?> = callbackFlow {
        val registro = Nuvem.mensagens(chatId)
            .orderBy("enviadaEm", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { instantaneo, _ ->
                trySend(instantaneo?.documents?.firstOrNull()?.let { paraMensagem(it) })
            }
        awaitClose { registro.remove() }
    }

    /** Quantas mensagens chegaram depois da última vez que a mãe abriu o chat. */
    suspend fun naoLidas(chatId: String, lidoEm: Timestamp?): Int {
        if (lidoEm == null) return 0
        return runCatching {
            Nuvem.mensagens(chatId)
                .whereGreaterThan("enviadaEm", lidoEm)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
                .toInt()
        }.getOrDefault(0)
    }

    /** uid → hora da última tecla de cada participante. */
    fun digitando(chatId: String): Flow<Map<String, Instant>> = callbackFlow {
        val registro = Nuvem.digitando(chatId).addSnapshotListener { instantaneo, _ ->
            trySend(
                instantaneo?.documents.orEmpty().mapNotNull { doc ->
                    val em = doc.getTimestamp("em", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    em?.let { doc.id to it.toDate().toInstant() }
                }.toMap(),
            )
        }
        awaitClose { registro.remove() }
    }

    /**
     * Entrar no chat. A participação e o contador de participantes vão na
     * mesma gravação: as regras recusam um sem o outro.
     */
    suspend fun entrar(chatId: String, uid: String, nome: String?) {
        val lote = Nuvem.bd.batch()
        lote.set(
            Nuvem.membros(chatId).document(uid),
            mapOf(
                "nome" to nome,
                "entrouEm" to FieldValue.serverTimestamp(),
                "lidoEm" to FieldValue.serverTimestamp(),
                "ultimaMensagemEm" to null,
                "enviadas" to 0L,
            ),
        )
        lote.update(Nuvem.chat(chatId), "participantes", FieldValue.increment(1))
        lote.commit().await()
    }

    suspend fun sair(chatId: String, uid: String) {
        val lote = Nuvem.bd.batch()
        lote.delete(Nuvem.membros(chatId).document(uid))
        lote.delete(Nuvem.digitando(chatId).document(uid))
        lote.update(Nuvem.chat(chatId), "participantes", FieldValue.increment(-1))
        lote.commit().await()
    }

    /**
     * Envia uma mensagem.
     *
     * O id é "{uid}_{entrada}_{n}", com n = mensagens já enviadas + 1. As
     * regras conferem essa sequência: é o que impede alguém de despejar
     * centenas de mensagens numa gravação só.
     */
    suspend fun enviar(chatId: String, uid: String, participacao: Participacao, texto: String) {
        val entrada = participacao.entrouEm ?: error("participação ainda não confirmada")
        val n = participacao.enviadas + 1
        val id = "${uid}_${entrada.toDate().time}_$n"

        val lote = Nuvem.bd.batch()
        lote.set(
            Nuvem.mensagens(chatId).document(id),
            mapOf(
                "autora" to uid,
                "texto" to texto,
                "enviadaEm" to FieldValue.serverTimestamp(),
                "curtidas" to emptyList<String>(),
            ),
        )
        lote.update(
            Nuvem.membros(chatId).document(uid),
            mapOf(
                "enviadas" to n,
                "ultimaMensagemEm" to FieldValue.serverTimestamp(),
                "lidoEm" to FieldValue.serverTimestamp(),
            ),
        )
        lote.delete(Nuvem.digitando(chatId).document(uid))
        lote.commit().await()
    }

    suspend fun curtir(chatId: String, mensagemId: String, uid: String, curtir: Boolean) {
        Nuvem.mensagens(chatId).document(mensagemId).update(
            "curtidas",
            if (curtir) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid),
        ).await()
    }

    suspend fun marcarLido(chatId: String, uid: String) {
        Nuvem.membros(chatId).document(uid)
            .update("lidoEm", FieldValue.serverTimestamp())
            .await()
    }

    suspend fun marcarDigitando(chatId: String, uid: String) {
        Nuvem.digitando(chatId).document(uid)
            .set(mapOf("em" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun atualizarNome(chatId: String, uid: String, nome: String?) {
        Nuvem.membros(chatId).document(uid).update("nome", nome).await()
    }

    /**
     * Tudo o que a mãe tem nos chats, para a exportação de dados.
     * Mensagens de outras participantes não entram: são dados delas.
     */
    suspend fun meusDados(uid: String): List<Pair<String, List<Mensagem>>> {
        val chats = Nuvem.chats().get().await().documents
        return chats.mapNotNull { chat ->
            val minhas = runCatching {
                Nuvem.mensagens(chat.id).whereEqualTo("autora", uid).get().await()
                    .documents.mapNotNull { paraMensagem(it) }
            }.getOrDefault(emptyList())
            val participa = runCatching {
                Nuvem.membros(chat.id).document(uid).get().await().exists()
            }.getOrDefault(false)
            if (!participa && minhas.isEmpty()) null
            else (chat.getString("nome") ?: chat.id) to minhas.sortedBy { it.enviadaEm }
        }
    }

    /**
     * Exclusão de conta: apaga as mensagens que a mãe escreveu e a tira de
     * todos os chats. Precisa rodar ANTES de encerrar a conta de acesso.
     */
    suspend fun apagarMeusDados(uid: String) {
        val chats = Nuvem.chats().get().await().documents
        for (chat in chats) {
            runCatching {
                val minhas = Nuvem.mensagens(chat.id).whereEqualTo("autora", uid).get().await()
                minhas.documents.chunked(400).forEach { parte ->
                    val lote = Nuvem.bd.batch()
                    parte.forEach { lote.delete(it.reference) }
                    lote.commit().await()
                }
            }
            runCatching {
                if (Nuvem.membros(chat.id).document(uid).get().await().exists()) {
                    sair(chat.id, uid)
                }
            }
        }
    }

    private fun paraMensagem(doc: DocumentSnapshot): Mensagem? {
        val autora = doc.getString("autora") ?: return null
        val texto = doc.getString("texto") ?: return null
        // Mensagem recém-enviada ainda sem a hora do servidor: usa a estimativa
        // local para já aparecer na tela, no lugar certo.
        val em = doc.getTimestamp("enviadaEm", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.toInstant() ?: Instant.now()
        @Suppress("UNCHECKED_CAST")
        val curtidas = (doc.get("curtidas") as? List<String>).orEmpty()
        return Mensagem(doc.id, autora, texto, em, curtidas)
    }
}
