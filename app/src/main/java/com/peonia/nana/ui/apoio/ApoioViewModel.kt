package com.peonia.nana.ui.apoio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peonia.nana.R
import com.peonia.nana.data.Chat
import com.peonia.nana.data.ChatRepository
import com.peonia.nana.data.Mensagem
import com.peonia.nana.data.Nuvem
import com.peonia.nana.data.Participacao
import com.peonia.nana.data.PerfilRepository
import com.peonia.nana.data.PreferenciasRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Um chat na lista, com o que a mãe precisa ver dele. */
data class ResumoChat(
    val chat: Chat,
    val participacao: Participacao?,
    val ultima: Mensagem?,
    val naoLidas: Int,
)

data class EstadoApoio(
    val carregando: Boolean = true,
    val seus: List<ResumoChat> = emptyList(),
    val descobrir: List<Chat> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ApoioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository()
    private val uid = Nuvem.uid

    /**
     * Nome que as outras participantes veem. Só aparece se a mãe ligou
     * "Mostrar meu nome nos chats" em Privacidade; senão fica nulo e o app
     * mostra "Participante".
     */
    private val nomeExibido: Flow<String?> = combine(
        PerfilRepository().perfil(uid),
        PreferenciasRepository(app).preferencias,
    ) { perfil, prefs ->
        if (!prefs.nomeNosChats) null
        else perfil?.nome?.trim()?.take(60)?.ifBlank { null }
    }.distinctUntilChanged()

    private val resumos: Flow<List<ResumoChat>> =
        if (uid == null) flowOf(emptyList())
        else repo.chats().flatMapLatest { chats ->
            if (chats.isEmpty()) flowOf(emptyList())
            else combine(chats.map { resumo(it, uid) }) { it.toList() }
        }

    val estado: StateFlow<EstadoApoio> = resumos
        .map { lista ->
            EstadoApoio(
                carregando = false,
                seus = lista.filter { it.participacao != null }
                    .sortedByDescending { it.ultima?.enviadaEm },
                descobrir = lista.filter { it.participacao == null }.map { it.chat },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoApoio())

    init {
        // Mantém o nome exibido em dia em todos os chats: muda quando a mãe
        // edita o perfil ou liga/desliga a opção em Privacidade.
        if (uid != null) {
            viewModelScope.launch {
                combine(resumos, nomeExibido) { lista, nome -> lista to nome }
                    .collect { (lista, nome) ->
                        lista.filter { it.participacao != null && it.participacao.nome != nome }
                            .forEach { runCatching { repo.atualizarNome(it.chat.id, uid, nome) } }
                    }
            }
        }
    }

    private fun resumo(chat: Chat, uid: String): Flow<ResumoChat> =
        repo.participacao(chat.id, uid).flatMapLatest { p ->
            if (p == null) flowOf(ResumoChat(chat, null, null, 0))
            else repo.ultimaMensagem(chat.id).mapLatest { ultima ->
                ResumoChat(chat, p, ultima, repo.naoLidas(chat.id, p.lidoEm))
            }
        }

    fun entrar(chat: Chat, aoEntrar: (String) -> Unit, aoAvisar: (Int) -> Unit) {
        val uid = uid ?: return
        viewModelScope.launch {
            runCatching {
                repo.entrar(chat.id, uid, nomeExibido.first())
            }
                .onSuccess { aoEntrar(chat.id) }
                .onFailure { aoAvisar(R.string.chat_erro_entrar) }
        }
    }
}
