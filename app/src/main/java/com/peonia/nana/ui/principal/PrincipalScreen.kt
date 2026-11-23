package com.peonia.nana.ui.principal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.peonia.nana.R
import com.peonia.nana.data.Perfil
import com.peonia.nana.data.TemaDireito
import com.peonia.nana.ui.apoio.ApoioScreen
import com.peonia.nana.ui.components.FundoNana
import com.peonia.nana.ui.diario.DiarioScreen
import com.peonia.nana.ui.diario.DiarioViewModel
import com.peonia.nana.ui.direitos.DireitoDetalheScreen
import com.peonia.nana.ui.direitos.DireitosScreen
import com.peonia.nana.ui.inicio.InicioScreen
import com.peonia.nana.ui.perfil.PerfilScreen
import com.peonia.nana.ui.theme.NanaCores
import java.time.LocalDate

/** Abas da barra inferior, na ordem do protótipo. */
private enum class Aba(val rota: String, val rotulo: Int, val icone: ImageVector) {
    INICIO("inicio", R.string.nav_inicio, Icons.Filled.Home),
    DIARIO("diario", R.string.nav_diario, Icons.Outlined.EditNote),
    DIREITOS("direitos", R.string.nav_direitos, Icons.Outlined.Balance),
    APOIO("apoio", R.string.nav_apoio, Icons.Outlined.ChatBubbleOutline),
    PERFIL("perfil", R.string.nav_perfil, Icons.Outlined.Person),
}

private const val ROTA_DIREITO_DETALHE = "direito/{tema}"

/**
 * Área principal do app: o fundo, a barra inferior e as telas de cada aba.
 * As telas que não têm barra inferior (Chat, Editar perfil, Notificações,
 * Privacidade, Ajuda) ficam fora daqui, em [com.peonia.nana.ui.NanaApp].
 */
@Composable
fun PrincipalScreen(
    perfil: Perfil,
    aoAvisar: (Int) -> Unit,
    aoEditarPerfil: () -> Unit,
    aoAbrirNotificacoes: () -> Unit,
    aoAbrirPrivacidade: () -> Unit,
    aoAbrirAjuda: () -> Unit,
    aoAbrirMeusDados: () -> Unit,
    aoSair: () -> Unit,
    aoAbrirChat: (String) -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    val entrada by nav.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val abaAtual = when (rotaAtual) {
        ROTA_DIREITO_DETALHE -> Aba.DIREITOS
        else -> Aba.entries.firstOrNull { it.rota == rotaAtual } ?: Aba.INICIO
    }

    val irPara: (Aba) -> Unit = { aba ->
        if (aba != abaAtual || rotaAtual == ROTA_DIREITO_DETALHE) {
            nav.navigate(aba.rota) {
                popUpTo(Aba.INICIO.rota) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FundoNana()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = NanaCores.Branco) {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = aba == abaAtual,
                            onClick = { irPara(aba) },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(stringResource(aba.rotulo)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NanaCores.Ameixa,
                                selectedTextColor = NanaCores.Ameixa,
                                indicatorColor = NanaCores.IconeFundo,
                                unselectedIconColor = NanaCores.TextoSuave,
                                unselectedTextColor = NanaCores.TextoSuave,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Aba.INICIO.rota,
                modifier = Modifier.padding(padding),
            ) {
                composable(Aba.INICIO.rota) {
                    // O check-in da tela inicial grava no mesmo diário, para
                    // as duas telas nunca mostrarem respostas diferentes.
                    val diarioVm: DiarioViewModel = viewModel()
                    val registros by diarioVm.registros.collectAsStateWithLifecycle()
                    InicioScreen(
                        perfil = perfil,
                        humorDeHoje = registros[LocalDate.now()],
                        aoRegistrarHumor = { humor -> diarioVm.registrarHoje(humor, aoAvisar) },
                        aoAvisar = { aoAvisar(R.string.proxima_etapa_aviso) },
                        aoAbrirDireitos = { irPara(Aba.DIREITOS) },
                    )
                }
                composable(Aba.DIARIO.rota) {
                    DiarioScreen(aoAvisar = aoAvisar)
                }
                composable(Aba.DIREITOS.rota) {
                    DireitosScreen(aoAbrirTema = { tema -> nav.navigate("direito/${tema.name}") })
                }
                composable(ROTA_DIREITO_DETALHE) { entradaDetalhe ->
                    val tema = TemaDireito.porNome(entradaDetalhe.arguments?.getString("tema"))
                    if (tema == null) {
                        DireitosScreen(aoAbrirTema = { nav.navigate("direito/${it.name}") })
                    } else {
                        DireitoDetalheScreen(tema = tema, aoVoltar = { nav.popBackStack() })
                    }
                }
                composable(Aba.APOIO.rota) {
                    ApoioScreen(aoAbrirChat = aoAbrirChat, aoAvisar = aoAvisar)
                }
                composable(Aba.PERFIL.rota) {
                    PerfilScreen(
                        perfil = perfil,
                        aoEditar = aoEditarPerfil,
                        aoAbrirNotificacoes = aoAbrirNotificacoes,
                        aoAbrirPrivacidade = aoAbrirPrivacidade,
                        aoAbrirAjuda = aoAbrirAjuda,
                        aoAbrirMeusDados = aoAbrirMeusDados,
                        aoSair = aoSair,
                        aoAvisar = aoAvisar,
                    )
                }
            }
        }
    }
}
