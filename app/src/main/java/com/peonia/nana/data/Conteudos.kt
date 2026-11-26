package com.peonia.nana.data

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peonia.nana.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Conteúdos educativos (entidade "Conteúdos" do documento do banco).
 *
 * Escritos pela equipe a partir de fontes científicas e carregados com
 * nana-api/admin/importar-conteudos.js. O app só lê, e só o que está com
 * status "publicado" (Regra 6). Cada conteúdo traz as próprias fontes.
 */
enum class CategoriaConteudo(@StringRes val rotulo: Int) {
    SAUDE_NA_GESTACAO(R.string.cat_gestacao),
    POS_PARTO(R.string.cat_pos_parto),
    SAUDE_MENTAL(R.string.cat_saude_mental),
    MEUS_DIREITOS(R.string.cat_meus_direitos);

    companion object {
        fun porNome(nome: String?): CategoriaConteudo? = entries.firstOrNull { it.name == nome }

        /** Categoria que combina com a fase informada no cadastro. */
        fun daFase(fase: FaseMaternidade?): CategoriaConteudo? = when (fase) {
            null -> null
            FaseMaternidade.GESTACAO -> SAUDE_NA_GESTACAO
            else -> POS_PARTO
        }
    }
}

data class FonteConteudo(
    val referencia: String,
    val tipo: String,
    val url: String?,
)

data class Conteudo(
    val id: String,
    val titulo: String,
    val descricao: String,
    /** Parágrafos do texto, na ordem. */
    val paragrafos: List<String>,
    val categoria: CategoriaConteudo,
    val fontes: List<FonteConteudo>,
    val ordem: Long,
)

class ConteudoRepository {

    /**
     * Conteúdos publicados, em ordem. O filtro por status é obrigatório: as
     * regras do servidor recusam a consulta sem ele.
     */
    fun publicados(): Flow<List<Conteudo>> = callbackFlow {
        val registro = Nuvem.bd.collection(COLECAO)
            .whereEqualTo("status", "publicado")
            .addSnapshotListener { instantaneo, erro ->
                if (erro != null) {
                    close(erro)
                    return@addSnapshotListener
                }
                val lista = instantaneo?.documents.orEmpty().mapNotNull { doc ->
                    val categoria = CategoriaConteudo.porNome(doc.getString("categoria"))
                    val titulo = doc.getString("titulo")
                    val texto = doc.getString("texto")
                    if (categoria == null || titulo == null || texto == null) return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val fontes = (doc.get("fontes") as? List<Map<String, Any?>>).orEmpty().mapNotNull { f ->
                        val ref = f["referencia"] as? String ?: return@mapNotNull null
                        FonteConteudo(ref, f["tipo"] as? String ?: "", f["url"] as? String)
                    }
                    Conteudo(
                        id = doc.id,
                        titulo = titulo,
                        descricao = doc.getString("descricao").orEmpty(),
                        paragrafos = texto.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() },
                        categoria = categoria,
                        fontes = fontes,
                        ordem = doc.getLong("ordem") ?: Long.MAX_VALUE,
                    )
                }.sortedBy { it.ordem }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    companion object {
        const val COLECAO = "conteudos"
    }
}

/** Estado da lista: null = carregando; [erro] = não deu para buscar. */
data class EstadoConteudos(
    val lista: List<Conteudo>? = null,
    val erro: Boolean = false,
)

class ConteudosViewModel : ViewModel() {
    val estado: StateFlow<EstadoConteudos> = ConteudoRepository().publicados()
        .map { EstadoConteudos(lista = it) }
        .catch { emit(EstadoConteudos(erro = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoConteudos())
}
