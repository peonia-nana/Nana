package com.peonia.nana.ui.apoio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peonia.nana.R
import com.peonia.nana.data.Chat
import com.peonia.nana.data.ChatRepository
import com.peonia.nana.data.Mensagem
import com.peonia.nana.data.Nuvem
import com.peonia.nana.data.Participacao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class EstadoChat(
    val carregando: Boolean = true,
    val chat: Chat? = null,
    val participacao: Participacao? = null,
    val mensagens: List<Mensagem> = emptyList(),
    /** uid → nome exibido; null ou ausente = "Participante". */
    val nomes: Map<String, String?> = emptyMap(),
    /** Outras participantes digitando agora. */
    val digitando: List<String> = emptyList(),
    val meuUid: String = "",
    val enviando: Boolean = false,
)

class ChatViewModel(estadoSalvo: SavedStateHandle) : ViewModel() {

    val chatId: String = checkNotNull(estadoSalvo["chatId"])
    private val repo = ChatRepository()
    private val uid = Nuvem.uid.orEmpty()

    private val enviando = MutableStateFlow(false)

    /** Relógio de 2 em 2 segundos, para o "..." sumir sozinho. */
    private val relogio: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(2_000)
        }
    }

    private val participacao = if (uid.isEmpty()) flowOf(null) else repo.participacao(chatId, uid)

    private val digitandoAgora: Flow<List<String>> =
        combine(repo.digitando(chatId), relogio) { mapa, agora ->
            mapa.filter { (quem, em) -> quem != uid && em.isAfter(agora.minusSeconds(JANELA_DIGITANDO)) }
                .keys.toList()
        }

    private val principal = combine(
        repo.chat(chatId),
        participacao,
        repo.mensagens(chatId),
        repo.nomes(chatId),
    ) { chat, p, mensagens, nomes ->
        EstadoChat(
            carregando = false,
            chat = chat,
            participacao = p,
            mensagens = mensagens,
            nomes = nomes,
            meuUid = uid,
        )
    }

    val estado: StateFlow<EstadoChat> =
        combine(principal, digitandoAgora, enviando) { base, digitando, enviandoAgora ->
            base.copy(digitando = digitando, enviando = enviandoAgora)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoChat(meuUid = uid))

    private var ultimoEnvio: Instant = Instant.EPOCH
    private var ultimoDigitando: Instant = Instant.EPOCH

    /**
     * Marca o chat como lido quando chega mensagem nova com a tela aberta.
     * Só grava se houver mesmo algo depois da última leitura.
     */
    fun marcarLidoSePreciso() {
        val atual = estado.value
        val p = atual.participacao ?: return
        val ultima = atual.mensagens.lastOrNull() ?: return
        val lidoEm = p.lidoEm?.toDate()?.toInstant()
        if (lidoEm == null || ultima.enviadaEm.isAfter(lidoEm)) {
            viewModelScope.launch { runCatching { repo.marcarLido(chatId, uid) } }
        }
    }

    /** Chamado a cada tecla. Grava no máximo a cada 4 segundos. */
    fun digitou() {
        if (estado.value.participacao == null) return
        val agora = Instant.now()
        if (agora.isBefore(ultimoDigitando.plusSeconds(4))) return
        ultimoDigitando = agora
        viewModelScope.launch { runCatching { repo.marcarDigitando(chatId, uid) } }
    }

    /**
     * Envia a mensagem. O campo é limpo na hora; se a gravação falhar, o
     * texto volta para o campo por [aoFalhar], para a mãe não perder o que
     * escreveu.
     */
    fun enviar(
        texto: String,
        aoLimpar: () -> Unit,
        aoFalhar: (String) -> Unit,
        aoAvisar: (Int) -> Unit,
    ) {
        val limpo = texto.trim()
        if (limpo.isEmpty() || enviando.value) return
        if (limpo.length > LIMITE_TEXTO) {
            aoAvisar(R.string.chat_texto_longo)
            return
        }
        val p = estado.value.participacao
        if (p?.entrouEm == null) {
            aoAvisar(R.string.chat_aguarde)
            return
        }
        enviando.value = true
        aoLimpar()
        viewModelScope.launch {
            // O servidor aceita uma mensagem a cada 2 segundos. Em vez de
            // deixar a gravação falhar, espera o tempo que falta.
            val espera = ultimoEnvio.plusMillis(INTERVALO_ENVIO_MS).toEpochMilli() - Instant.now().toEpochMilli()
            if (espera > 0) delay(espera)
            runCatching { repo.enviar(chatId, uid, p, limpo) }
                .onSuccess {
                    ultimoEnvio = Instant.now()
                    ultimoDigitando = Instant.EPOCH
                }
                .onFailure {
                    aoFalhar(texto)
                    aoAvisar(R.string.chat_erro_enviar)
                }
            enviando.value = false
        }
    }

    fun alternarCurtida(mensagem: Mensagem) {
        if (estado.value.participacao == null) return
        val curtiu = uid in mensagem.curtidas
        viewModelScope.launch {
            runCatching { repo.curtir(chatId, mensagem.id, uid, !curtiu) }
        }
    }

    fun sair(aoSair: () -> Unit, aoAvisar: (Int) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.sair(chatId, uid) }
                .onSuccess { aoSair() }
                .onFailure { aoAvisar(R.string.chat_erro_sair) }
        }
    }

    companion object {
        const val LIMITE_TEXTO = 1000
        private const val INTERVALO_ENVIO_MS = 2_100L
        private const val JANELA_DIGITANDO = 6L
    }
}
