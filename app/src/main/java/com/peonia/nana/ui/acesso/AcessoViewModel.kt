package com.peonia.nana.ui.acesso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peonia.nana.R
import com.peonia.nana.data.AutenticacaoRepository
import com.peonia.nana.data.ErroAcesso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModoAcesso { ENTRAR, CRIAR_CONTA }

data class AcessoUiState(
    val modo: ModoAcesso = ModoAcesso.ENTRAR,
    val email: String = "",
    val senha: String = "",
    val senhaVisivel: Boolean = false,
    val privacidadeAceita: Boolean = false,
    val ocupada: Boolean = false,
    val erro: Int? = null,
)

class AcessoViewModel : ViewModel() {

    private val autenticacao = AutenticacaoRepository()

    private val _estado = MutableStateFlow(AcessoUiState())
    val estado: StateFlow<AcessoUiState> = _estado.asStateFlow()

    fun comecarEm(modo: ModoAcesso) {
        _estado.update { it.copy(modo = modo) }
    }

    fun trocarModo() = _estado.update {
        it.copy(
            modo = if (it.modo == ModoAcesso.ENTRAR) ModoAcesso.CRIAR_CONTA else ModoAcesso.ENTRAR,
            erro = null,
        )
    }

    fun alterarEmail(v: String) = _estado.update { it.copy(email = v.take(120), erro = null) }
    fun alterarSenha(v: String) = _estado.update { it.copy(senha = v.take(72), erro = null) }
    fun alternarSenhaVisivel() = _estado.update { it.copy(senhaVisivel = !it.senhaVisivel) }
    fun alternarPrivacidade() = _estado.update {
        it.copy(privacidadeAceita = !it.privacidadeAceita, erro = null)
    }

    fun enviar(aoAvisar: (Int) -> Unit) {
        val s = _estado.value
        if (s.ocupada) return

        if (s.email.isBlank() || !s.email.contains("@")) {
            _estado.update { it.copy(erro = R.string.acc_erro_email) }
            return
        }
        if (s.modo == ModoAcesso.CRIAR_CONTA && !s.privacidadeAceita) {
            _estado.update { it.copy(erro = R.string.acc_privacidade_exigida) }
            return
        }
        if (s.senha.length < 8 && s.modo == ModoAcesso.CRIAR_CONTA) {
            _estado.update { it.copy(erro = R.string.acc_erro_senha_fraca) }
            return
        }

        _estado.update { it.copy(ocupada = true, erro = null) }
        viewModelScope.launch {
            val erro = if (s.modo == ModoAcesso.CRIAR_CONTA) {
                autenticacao.criarConta(s.email, s.senha)
            } else {
                autenticacao.entrar(s.email, s.senha)
            }
            _estado.update { it.copy(ocupada = false, erro = erro?.let(::mensagem)) }
            // Quando dá certo, quem troca de tela é o estado de login lá no
            // NanaApp: não há navegação manual aqui.
        }
    }

    fun recuperarSenha(aoAvisar: (Int) -> Unit) {
        val email = _estado.value.email
        if (email.isBlank() || !email.contains("@")) {
            _estado.update { it.copy(erro = R.string.acc_informe_email) }
            return
        }
        viewModelScope.launch {
            autenticacao.recuperarSenha(email)
            // A mensagem é a mesma exista a conta ou não: dizer "esse e-mail
            // não está cadastrado" permitiria descobrir quem usa o Nanã.
            aoAvisar(R.string.acc_recuperar_enviado)
        }
    }

    private fun mensagem(erro: ErroAcesso): Int = when (erro) {
        ErroAcesso.EMAIL_INVALIDO -> R.string.acc_erro_email
        ErroAcesso.SENHA_FRACA -> R.string.acc_erro_senha_fraca
        ErroAcesso.EMAIL_JA_USADO -> R.string.acc_erro_email_usado
        ErroAcesso.CREDENCIAIS_INVALIDAS -> R.string.acc_erro_credenciais
        ErroAcesso.SEM_INTERNET -> R.string.acc_erro_internet
        ErroAcesso.DESCONHECIDO -> R.string.acc_erro_desconhecido
    }
}
