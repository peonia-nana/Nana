package com.peonia.nana.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Direitos da mãe sobre os próprios dados.
 *
 * LGPD, art. 18, II e V: acesso e portabilidade, em formato aberto.
 * LGPD, art. 18, VI: eliminação dos dados.
 */
object MeusDados {

    /**
     * Monta um arquivo JSON com tudo o que o Nanã guarda sobre a mãe e
     * devolve o endereço para compartilhar.
     */
    suspend fun exportar(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val uid = Nuvem.uid ?: return@withContext null
            val conta = Nuvem.auth.currentUser

            val perfil = PerfilRepository().perfil(uid).first()
            val diario = DiarioRepository().registros(uid).first()
            val prefsLocais = PreferenciasRepository(context).preferencias.first()
            val chats = ChatRepository().meusDados(uid)

            val raiz = JSONObject().apply {
                put("aplicativo", "Nanã")
                put("geradoEm", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                put(
                    "aviso",
                    "Este arquivo contém seus dados pessoais no aplicativo Nanã. " +
                        "Guarde em local seguro e não compartilhe com terceiros.",
                )
                put(
                    "conta",
                    JSONObject().apply {
                        put("identificador", uid)
                        put("email", conta?.email ?: JSONObject.NULL)
                        put("emailVerificado", conta?.isEmailVerified ?: false)
                    },
                )
                put("perfil", perfilEmJson(perfil))
                put("diario", diarioEmJson(diario))
                put("chats", chatsEmJson(chats))
                put("preferenciasDoAparelho", preferenciasEmJson(prefsLocais))
                put(
                    "fotoDePerfil",
                    if (FotoPerfil.existe(context)) "sim, guardada apenas neste aparelho" else "não",
                )
            }

            val pasta = File(context.cacheDir, "exportacoes").apply { mkdirs() }
            // Limpa exportações antigas: não convém deixar cópias de dados
            // pessoais acumulando no cache.
            pasta.listFiles()?.forEach { it.delete() }

            val arquivo = File(pasta, "meus-dados-nana-${LocalDate.now()}.json")
            arquivo.writeText(raiz.toString(2))

            FileProvider.getUriForFile(context, "${context.packageName}.arquivos", arquivo)
        } catch (e: Exception) {
            null
        }
    }

    fun intencaoDeCompartilhar(endereco: Uri, titulo: String): Intent {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, endereco)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(envio, titulo)
    }

    /**
     * Apaga a conta e os dados pessoais.
     *
     * A ordem importa. O Firestore **não** apaga subcoleção junto com o
     * documento pai: se a conta fosse encerrada primeiro, o diário ficaria
     * para trás sem ninguém com permissão para alcançá-lo. Então:
     *
     *   1. mensagens nos chats e participação neles
     *   2. diário e preferências (os documentos de dentro)
     *   3. perfil de acolhimento
     *   4. cadastro da mãe
     *   5. dados guardados neste aparelho
     *   6. conta de acesso, por último
     *
     * Os registros de consentimento e auditoria permanecem: não guardam dado
     * pessoal, só finalidade e data, e são a prova do que foi aceito
     * (art. 8º, §1º). Para removê-los também, a equipe roda o script
     * `admin/excluir-conta.js`.
     */
    suspend fun apagarTudo(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = Nuvem.uid ?: return@withContext false

            ChatRepository().apagarMeusDados(uid)
            apagarColecao(Nuvem.diario(uid))
            apagarColecao(Nuvem.preferencias(uid))
            runCatching { Nuvem.perfil(uid).delete().await() }
            runCatching { Nuvem.mae(uid).delete().await() }

            PreferenciasRepository(context).apagarTudo()
            FotoPerfil.remover(context)
            File(context.cacheDir, "exportacoes").deleteRecursively()

            runCatching { Nuvem.auth.currentUser?.delete()?.await() }
            Nuvem.auth.signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun apagarColecao(
        colecao: com.google.firebase.firestore.CollectionReference,
    ) {
        runCatching {
            val docs = colecao.get().await()
            docs.documents.forEach { runCatching { it.reference.delete().await() } }
        }
    }

    private fun perfilEmJson(perfil: Perfil?): Any {
        if (perfil == null) return JSONObject.NULL
        return JSONObject().apply {
            put("nome", perfil.nome.ifBlank { JSONObject.NULL })
            put("idade", perfil.idade ?: JSONObject.NULL)
            put("cidade", perfil.cidade.ifBlank { JSONObject.NULL })
            put("fasePosParto", perfil.fase?.name ?: JSONObject.NULL)
            put("interesses", JSONArray(perfil.interesses.map { it.name }))
            put("nomeBebe", perfil.nomesBebes.ifBlank { JSONObject.NULL })
            put("quantidadeFilhos", perfil.quantidadeFilhos?.name ?: JSONObject.NULL)
            put("autodeclaracao", perfil.autodeclaracao?.name ?: JSONObject.NULL)
        }
    }

    private fun diarioEmJson(registros: Map<LocalDate, Humor>): JSONArray {
        val lista = JSONArray()
        registros.toSortedMap().forEach { (data, humor) ->
            lista.put(
                JSONObject().apply {
                    put("dataRegistro", data.toString())
                    put("sentimento", humor.name)
                },
            )
        }
        return lista
    }

    private fun chatsEmJson(chats: List<Pair<String, List<Mensagem>>>): JSONArray {
        val lista = JSONArray()
        chats.forEach { (nome, mensagens) ->
            lista.put(
                JSONObject().apply {
                    put("chat", nome)
                    put(
                        "mensagensEscritas",
                        JSONArray(
                            mensagens.map { m ->
                                JSONObject().apply {
                                    put("texto", m.texto)
                                    put("enviadaEm", m.enviadaEm.toString())
                                    put("curtidas", m.curtidas.size)
                                }
                            },
                        ),
                    )
                },
            )
        }
        return lista
    }

    private fun preferenciasEmJson(p: PreferenciasApp): JSONObject = JSONObject().apply {
        put("lembreteDiario", p.lembreteDiario)
        put("novidadesDireitos", p.novidadesDireitos)
        put("respostasApoio", p.respostasApoio)
        put("dicasBemEstar", p.dicasBemEstar)
        put("horarioLembrete", p.horarioLembrete)
        put("naoIncomodar", p.naoIncomodar)
        put("duasEtapas", p.duasEtapas)
        put("bloqueioApp", p.bloqueioApp)
        put("nomeNosChats", p.nomeNosChats)
    }
}
