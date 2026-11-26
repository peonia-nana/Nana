package com.peonia.nana.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.peonia.nana.notificacoes.Agendador
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.peonia.nana.R
import com.peonia.nana.data.AutenticacaoRepository
import com.peonia.nana.data.EstadoAcesso
import com.peonia.nana.data.MeusDados
import com.peonia.nana.data.Perfil
import com.peonia.nana.data.PerfilRepository
import com.peonia.nana.ui.acesso.AcessoScreen
import com.peonia.nana.ui.acesso.AcessoViewModel
import com.peonia.nana.ui.acesso.ModoAcesso
import com.peonia.nana.ui.acesso.VerificarEmailScreen
import com.peonia.nana.ui.cadastro.CadastroMomentoScreen
import com.peonia.nana.ui.cadastro.CadastroNomeScreen
import com.peonia.nana.ui.cadastro.CadastroPerfilScreen
import com.peonia.nana.ui.cadastro.CadastroViewModel
import com.peonia.nana.ui.onboarding.BoasVindasScreen
import com.peonia.nana.ui.onboarding.FuncionalidadesScreen
import com.peonia.nana.ui.onboarding.PropositoScreen
import com.peonia.nana.ui.apoio.ChatScreen
import com.peonia.nana.ui.perfil.AjudaScreen
import com.peonia.nana.ui.perfil.EditarPerfilScreen
import com.peonia.nana.ui.perfil.MeusDadosScreen
import com.peonia.nana.ui.perfil.NotificacoesScreen
import com.peonia.nana.ui.perfil.PrivacidadeScreen
import com.peonia.nana.ui.principal.PrincipalScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Em que fase o aplicativo está. */
sealed interface EstadoApp {
    data object Carregando : EstadoApp
    data object Deslogada : EstadoApp
    data class VerificandoEmail(val email: String) : EstadoApp
    data object SemCadastro : EstadoApp
    data class Pronta(val perfil: Perfil) : EstadoApp
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val autenticacao = AutenticacaoRepository()
    private val perfis = PerfilRepository()

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<EstadoApp> = autenticacao.estado
        .flatMapLatest { acesso ->
            when (acesso) {
                EstadoAcesso.Carregando -> flowOf(EstadoApp.Carregando)
                EstadoAcesso.Deslogada -> flowOf(EstadoApp.Deslogada)
                is EstadoAcesso.EmailNaoVerificado ->
                    flowOf(EstadoApp.VerificandoEmail(acesso.email))
                is EstadoAcesso.Dentro -> perfilOuCadastro(acesso.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.Carregando)

    /**
     * Enquanto o Firestore não respondeu, mantém "Carregando". Sem isso, a
     * tela de cadastro apareceria por um instante para quem já tem perfil.
     */
    private fun perfilOuCadastro(uid: String): Flow<EstadoApp> =
        perfis.perfil(uid)
            .map { p -> if (p == null) EstadoApp.SemCadastro else EstadoApp.Pronta(p) }
            .onStart { emit(EstadoApp.Carregando) }

    fun salvarPerfil(perfil: Perfil, aoFalhar: (Int) -> Unit) {
        val uid = com.peonia.nana.data.Nuvem.uid ?: return
        viewModelScope.launch {
            val ok = runCatching {
                perfis.salvar(uid, perfil)
                perfis.registrarAuditoria(uid, "PERFIL_ATUALIZADO")
            }.isSuccess
            if (!ok) aoFalhar(R.string.sync_erro)
        }
    }

    fun sair() {
        Agendador.cancelarTudo(getApplication())
        viewModelScope.launch { autenticacao.sair() }
    }

    fun apagarTudo(aoConcluir: (Boolean) -> Unit) {
        Agendador.cancelarTudo(getApplication())
        viewModelScope.launch {
            aoConcluir(MeusDados.apagarTudo(getApplication()))
        }
    }
}

@Composable
fun NanaApp(appVm: AppViewModel = viewModel()) {
    val estado by appVm.estado.collectAsStateWithLifecycle()

    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // Um só lugar para as mensagens curtas, servindo todas as telas.
    val avisar: (Int) -> Unit = { textoId ->
        escopo.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(contexto.getString(textoId))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val e = estado) {
            EstadoApp.Carregando -> Unit

            EstadoApp.Deslogada -> FluxoDeEntrada(aoAvisar = avisar)

            is EstadoApp.VerificandoEmail -> VerificarEmailScreen(
                email = e.email,
                // A troca de tela acontece sozinha: ao confirmar, o estado de
                // login muda e o app recompõe.
                aoVerificar = { avisar(R.string.salvo) },
                aoSair = { appVm.sair() },
                aoAvisar = avisar,
            )

            EstadoApp.SemCadastro -> FluxoDeCadastro(aoAvisar = avisar)

            is EstadoApp.Pronta -> AppLogado(
                perfil = e.perfil,
                appVm = appVm,
                aoAvisar = avisar,
            )
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------------------
// Antes de entrar: apresentação do Nanã e tela de acesso
// ---------------------------------------------------------------------------

private object RotasEntrada {
    const val BOAS_VINDAS = "boas_vindas"
    const val PROPOSITO = "proposito"
    const val FUNCIONALIDADES = "funcionalidades"
    const val ACESSO = "acesso"
}

@Composable
private fun FluxoDeEntrada(
    aoAvisar: (Int) -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    val acessoVm: AcessoViewModel = viewModel()

    NavHost(navController = nav, startDestination = RotasEntrada.BOAS_VINDAS) {
        composable(RotasEntrada.BOAS_VINDAS) {
            BoasVindasScreen(
                aoComecar = { nav.navigate(RotasEntrada.PROPOSITO) },
                aoJaTenhoConta = {
                    acessoVm.comecarEm(ModoAcesso.ENTRAR)
                    nav.navigate(RotasEntrada.ACESSO)
                },
            )
        }
        composable(RotasEntrada.PROPOSITO) {
            PropositoScreen(
                aoContinuar = { nav.navigate(RotasEntrada.FUNCIONALIDADES) },
                aoVoltar = { nav.popBackStack() },
            )
        }
        composable(RotasEntrada.FUNCIONALIDADES) {
            FuncionalidadesScreen(
                aoCadastrar = {
                    acessoVm.comecarEm(ModoAcesso.CRIAR_CONTA)
                    nav.navigate(RotasEntrada.ACESSO)
                },
                aoVoltar = { nav.popBackStack() },
            )
        }
        composable(RotasEntrada.ACESSO) {
            AcessoScreen(
                aoVoltar = { nav.popBackStack() },
                aoAvisar = aoAvisar,
                vm = acessoVm,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Logada, mas sem cadastro: os 3 passos
// ---------------------------------------------------------------------------

private object RotasCadastro {
    const val NOME = "cadastro_nome"
    const val MOMENTO = "cadastro_momento"
    const val PERFIL = "cadastro_perfil"
}

@Composable
private fun FluxoDeCadastro(
    aoAvisar: (Int) -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    // Um único ViewModel guarda as respostas entre os 3 passos, para que a
    // mãe possa voltar e revisar sem perder o que já preencheu.
    val cadastroVm: CadastroViewModel = viewModel()

    NavHost(navController = nav, startDestination = RotasCadastro.NOME) {
        composable(RotasCadastro.NOME) {
            CadastroNomeScreen(
                vm = cadastroVm,
                aoAvancar = { nav.navigate(RotasCadastro.MOMENTO) },
                aoVoltar = { nav.popBackStack() },
            )
        }
        composable(RotasCadastro.MOMENTO) {
            CadastroMomentoScreen(
                vm = cadastroVm,
                aoAvancar = { nav.navigate(RotasCadastro.PERFIL) },
                aoVoltar = { nav.popBackStack() },
            )
        }
        composable(RotasCadastro.PERFIL) {
            CadastroPerfilScreen(
                vm = cadastroVm,
                // Ao salvar, o perfil aparece no Firestore e o app troca de
                // fase sozinho. Nada de navegação manual aqui.
                aoConcluir = { },
                aoVoltar = { nav.popBackStack() },
                aoAvisar = aoAvisar,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Logada e com cadastro: o app em si
// ---------------------------------------------------------------------------

private object RotasApp {
    const val PRINCIPAL = "principal"
    const val EDITAR_PERFIL = "editar_perfil"
    const val NOTIFICACOES = "notificacoes"
    const val PRIVACIDADE = "privacidade"
    const val AJUDA = "ajuda"
    const val MEUS_DADOS = "meus_dados"
    const val CHAT = "chat/{chatId}"
    fun chat(chatId: String) = "chat/$chatId"
}

@Composable
private fun AppLogado(
    perfil: Perfil,
    appVm: AppViewModel,
    aoAvisar: (Int) -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    // Avisos: liga conforme as preferências e, uma única vez, pede a
    // autorização do Android 13+. Depois disso, só pela tela Notificações.
    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        Agendador.aplicar(contexto)
        val sp = contexto.getSharedPreferences("avisos_estado", Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !sp.getBoolean("permissao_pedida", false) &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            sp.edit().putBoolean("permissao_pedida", true).apply()
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // LGPD, art. 18, II e V: cópia dos dados em formato aberto. Usado em
    // Meus dados e em Privacidade e segurança.
    val baixarDados: () -> Unit = {
        aoAvisar(R.string.pri_exportando)
        escopo.launch {
            val endereco = MeusDados.exportar(contexto)
            if (endereco == null) {
                aoAvisar(R.string.pri_exportar_erro)
            } else {
                contexto.startActivity(
                    MeusDados.intencaoDeCompartilhar(
                        endereco,
                        contexto.getString(R.string.pri_exportar_titulo),
                    ),
                )
            }
        }
    }

    NavHost(navController = nav, startDestination = RotasApp.PRINCIPAL) {

        composable(RotasApp.PRINCIPAL) {
            PrincipalScreen(
                perfil = perfil,
                aoAvisar = aoAvisar,
                aoEditarPerfil = { nav.navigate(RotasApp.EDITAR_PERFIL) },
                aoAbrirNotificacoes = { nav.navigate(RotasApp.NOTIFICACOES) },
                aoAbrirPrivacidade = { nav.navigate(RotasApp.PRIVACIDADE) },
                aoAbrirAjuda = { nav.navigate(RotasApp.AJUDA) },
                aoAbrirMeusDados = { nav.navigate(RotasApp.MEUS_DADOS) },
                aoSair = { appVm.sair() },
                aoAbrirChat = { chatId -> nav.navigate(RotasApp.chat(chatId)) },
            )
        }

        composable(RotasApp.MEUS_DADOS) {
            MeusDadosScreen(
                perfil = perfil,
                aoVoltar = { nav.popBackStack() },
                aoEditar = { nav.navigate(RotasApp.EDITAR_PERFIL) },
                aoBaixarDados = baixarDados,
                aoAvisar = aoAvisar,
            )
        }

        composable(RotasApp.CHAT) {
            // O ChatViewModel lê o chatId da rota pelo SavedStateHandle.
            ChatScreen(aoVoltar = { nav.popBackStack() }, aoAvisar = aoAvisar)
        }

        composable(RotasApp.EDITAR_PERFIL) {
            EditarPerfilScreen(
                perfil = perfil,
                aoSalvar = {
                    appVm.salvarPerfil(it, aoAvisar)
                    nav.popBackStack()
                },
                aoCancelar = { nav.popBackStack() },
                aoAvisar = aoAvisar,
            )
        }

        composable(RotasApp.NOTIFICACOES) {
            NotificacoesScreen(aoVoltar = { nav.popBackStack() }, aoAvisar = aoAvisar)
        }

        composable(RotasApp.PRIVACIDADE) {
            PrivacidadeScreen(
                aoVoltar = { nav.popBackStack() },
                aoAvisar = aoAvisar,
                aoBaixarDados = baixarDados,
                aoExcluirConta = {
                    appVm.apagarTudo { ok ->
                        // Dando certo, a conta some e o app volta sozinho
                        // para a tela de boas-vindas.
                        if (!ok) aoAvisar(R.string.sync_erro)
                    }
                },
            )
        }

        composable(RotasApp.AJUDA) {
            AjudaScreen(
                aoVoltar = { nav.popBackStack() },
                aoAvisar = aoAvisar,
                aoEnviarEmail = {
                    val email = contexto.getString(R.string.aju_email_endereco)
                    abrir(contexto, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) {
                        aoAvisar(R.string.aju_sem_email)
                    }
                },
                aoLigar = { numero ->
                    // ACTION_DIAL só abre o teclado com o número já digitado:
                    // quem decide ligar é a usuária, e não precisa permissão.
                    abrir(contexto, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))) {
                        aoAvisar(R.string.aju_sem_email)
                    }
                },
            )
        }
    }
}

private inline fun abrir(
    contexto: android.content.Context,
    intencao: Intent,
    aoFalhar: () -> Unit,
) {
    try {
        contexto.startActivity(intencao)
    } catch (e: ActivityNotFoundException) {
        aoFalhar()
    }
}
