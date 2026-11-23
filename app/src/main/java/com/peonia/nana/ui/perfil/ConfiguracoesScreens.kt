package com.peonia.nana.ui.perfil

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peonia.nana.R
import com.peonia.nana.data.PreferenciasApp
import com.peonia.nana.data.PreferenciasRepository
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.CabecalhoVoltar
import com.peonia.nana.ui.components.CampoBusca
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// =============================================================================
// ViewModel compartilhado pelas telas de Notificações e Privacidade
// =============================================================================

class PreferenciasViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = PreferenciasRepository(app)

    val salvas: StateFlow<PreferenciasApp?> = repositorio.preferencias
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Cópia em edição: só vai para o disco quando a usuária salva. */
    private val _rascunho = MutableStateFlow<PreferenciasApp?>(null)
    val rascunho: StateFlow<PreferenciasApp?> = _rascunho.asStateFlow()

    fun comecarEdicao(atuais: PreferenciasApp) {
        if (_rascunho.value == null) _rascunho.value = atuais
    }

    fun alterar(bloco: (PreferenciasApp) -> PreferenciasApp) {
        _rascunho.value = _rascunho.value?.let(bloco)
    }

    fun salvar(aoConcluir: () -> Unit) {
        val p = _rascunho.value ?: return
        viewModelScope.launch {
            repositorio.salvar(p)
            aoConcluir()
        }
    }

    fun descartar() {
        _rascunho.value = null
    }
}

// =============================================================================
// Peças comuns
// =============================================================================

@Composable
private fun LinhaInterruptor(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    marcado: Boolean,
    aoMudar: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeArredondado { Icon(icone, contentDescription = null, tint = NanaCores.Ameixa) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = NanaCores.AmeixaEscura)
            Text(descricao, style = MaterialTheme.typography.bodySmall, color = NanaCores.TextoSuave)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = marcado,
            onCheckedChange = aoMudar,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NanaCores.Branco,
                checkedTrackColor = NanaCores.Ameixa,
            ),
        )
    }
}

@Composable
private fun LinhaAcao(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    aoTocar: () -> Unit,
    valor: String? = null,
    destrutiva: Boolean = false,
) {
    Surface(onClick = aoTocar, color = NanaCores.Branco) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconeArredondado {
                Icon(
                    icone,
                    contentDescription = null,
                    tint = if (destrutiva) NanaCores.Erro else NanaCores.Ameixa,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (destrutiva) NanaCores.Erro else NanaCores.AmeixaEscura,
                )
                Text(descricao, style = MaterialTheme.typography.bodySmall, color = NanaCores.TextoSuave)
            }
            if (valor != null) {
                Text(valor, style = MaterialTheme.typography.bodyMedium, color = NanaCores.Ameixa)
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NanaCores.Ameixa,
            )
        }
    }
}

@Composable
private fun CartaoDeLinhas(conteudo: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
    ) {
        Column { conteudo() }
    }
}

@Composable
private fun Separador() {
    HorizontalDivider(color = NanaCores.Fundo, thickness = 1.dp)
}

@Composable
private fun RodapeSalvarCancelar(aoSalvar: () -> Unit, aoCancelar: () -> Unit) {
    BotaoPrincipal(texto = stringResource(R.string.salvar_alteracoes), onClick = aoSalvar)
    TextButton(onClick = aoCancelar, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(
            stringResource(R.string.cancelar),
            color = NanaCores.Ameixa,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// =============================================================================
// Notificações
// =============================================================================

/**
 * As escolhas ficam guardadas, mas o aplicativo ainda **não envia** nenhum
 * aviso. Entregar notificação de verdade precisa de permissão do sistema e de
 * agendamento em segundo plano, que é um trabalho à parte. O rodapé da tela
 * avisa isso à usuária, para a tela não prometer o que não cumpre.
 */
@Composable
fun NotificacoesScreen(
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
    vm: PreferenciasViewModel = viewModel(),
) {
    val salvas by vm.salvas.collectAsStateWithLifecycle()
    val rascunho by vm.rascunho.collectAsStateWithLifecycle()
    LaunchedEffect(salvas) { salvas?.let { vm.comecarEdicao(it) } }
    val p = rascunho ?: return

    TelaNana {
        LayoutComRodape(
            rodape = {
                RodapeSalvarCancelar(
                    aoSalvar = {
                        vm.salvar {
                            aoAvisar(R.string.salvo)
                            aoVoltar()
                        }
                    },
                    aoCancelar = { vm.descartar(); aoVoltar() },
                )
            },
        ) {
            CabecalhoVoltar(
                titulo = stringResource(R.string.not_titulo),
                aoVoltar = { vm.descartar(); aoVoltar() },
            )
            Text(
                stringResource(R.string.not_apoio),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.not_grupo_lembretes))
            Spacer(Modifier.height(8.dp))
            CartaoDeLinhas {
                LinhaInterruptor(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    stringResource(R.string.not_diario),
                    stringResource(R.string.not_diario_desc),
                    p.lembreteDiario,
                ) { v -> vm.alterar { it.copy(lembreteDiario = v) } }
                Separador()
                LinhaInterruptor(
                    Icons.Outlined.Balance,
                    stringResource(R.string.not_direitos),
                    stringResource(R.string.not_direitos_desc),
                    p.novidadesDireitos,
                ) { v -> vm.alterar { it.copy(novidadesDireitos = v) } }
                Separador()
                LinhaInterruptor(
                    Icons.Outlined.ChatBubbleOutline,
                    stringResource(R.string.not_apoio_chats),
                    stringResource(R.string.not_apoio_chats_desc),
                    p.respostasApoio,
                ) { v -> vm.alterar { it.copy(respostasApoio = v) } }
                Separador()
                LinhaInterruptor(
                    Icons.Outlined.FavoriteBorder,
                    stringResource(R.string.not_bemestar),
                    stringResource(R.string.not_bemestar_desc),
                    p.dicasBemEstar,
                ) { v -> vm.alterar { it.copy(dicasBemEstar = v) } }
            }

            Spacer(Modifier.height(20.dp))
            TituloSecao(stringResource(R.string.not_grupo_horarios))
            Spacer(Modifier.height(8.dp))
            CartaoDeLinhas {
                LinhaAcao(
                    icone = Icons.Outlined.AccessTime,
                    titulo = stringResource(R.string.not_horario),
                    descricao = stringResource(R.string.not_horario_desc),
                    valor = p.horarioLembrete,
                    aoTocar = { aoAvisar(R.string.not_pendente) },
                )
                Separador()
                LinhaInterruptor(
                    Icons.Outlined.Bedtime,
                    stringResource(R.string.not_naoincomodar),
                    stringResource(R.string.not_naoincomodar_desc),
                    p.naoIncomodar,
                ) { v -> vm.alterar { it.copy(naoIncomodar = v) } }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.not_rodape),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.not_pendente),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.Ameixa,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// =============================================================================
// Privacidade e segurança
// =============================================================================

@Composable
fun PrivacidadeScreen(
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
    aoBaixarDados: () -> Unit,
    aoExcluirConta: () -> Unit,
    vm: PreferenciasViewModel = viewModel(),
) {
    val salvas by vm.salvas.collectAsStateWithLifecycle()
    val rascunho by vm.rascunho.collectAsStateWithLifecycle()
    LaunchedEffect(salvas) { salvas?.let { vm.comecarEdicao(it) } }
    val p = rascunho ?: return

    var confirmandoExclusao by remember { mutableStateOf(false) }

    TelaNana {
        LayoutComRodape(
            rodape = {
                RodapeSalvarCancelar(
                    aoSalvar = {
                        vm.salvar {
                            aoAvisar(R.string.salvo)
                            aoVoltar()
                        }
                    },
                    aoCancelar = { vm.descartar(); aoVoltar() },
                )
            },
        ) {
            CabecalhoVoltar(
                titulo = stringResource(R.string.pri_titulo),
                aoVoltar = { vm.descartar(); aoVoltar() },
            )
            Text(
                stringResource(R.string.pri_apoio),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.pri_grupo_conta))
            Spacer(Modifier.height(8.dp))
            CartaoDeLinhas {
                // Depende de login, que o app ainda não tem.
                LinhaAcao(
                    icone = Icons.Outlined.Key,
                    titulo = stringResource(R.string.pri_senha),
                    descricao = stringResource(R.string.pri_senha_desc),
                    aoTocar = { aoAvisar(R.string.pri_precisa_conta) },
                )
                Separador()
                LinhaInterruptor(
                    Icons.Outlined.Shield,
                    stringResource(R.string.pri_duas_etapas),
                    stringResource(R.string.pri_duas_etapas_desc),
                    p.duasEtapas,
                ) { v ->
                    vm.alterar { it.copy(duasEtapas = v) }
                    aoAvisar(R.string.pri_precisa_conta)
                }
                Separador()
                LinhaInterruptor(
                    Icons.Outlined.PhoneAndroid,
                    stringResource(R.string.pri_bloqueio),
                    stringResource(R.string.pri_bloqueio_desc),
                    p.bloqueioApp,
                ) { v -> vm.alterar { it.copy(bloqueioApp = v) } }
            }

            Spacer(Modifier.height(20.dp))
            TituloSecao(stringResource(R.string.pri_grupo_dados))
            Spacer(Modifier.height(8.dp))
            CartaoDeLinhas {
                LinhaInterruptor(
                    Icons.Outlined.Visibility,
                    stringResource(R.string.pri_nome_chats),
                    stringResource(R.string.pri_nome_chats_desc),
                    p.nomeNosChats,
                ) { v -> vm.alterar { it.copy(nomeNosChats = v) } }
                Separador()
                LinhaAcao(
                    icone = Icons.Outlined.Download,
                    titulo = stringResource(R.string.pri_baixar),
                    descricao = stringResource(R.string.pri_baixar_desc),
                    aoTocar = aoBaixarDados,
                )
                Separador()
                LinhaAcao(
                    icone = Icons.Outlined.Delete,
                    titulo = stringResource(R.string.pri_excluir),
                    descricao = stringResource(R.string.pri_excluir_desc),
                    aoTocar = { confirmandoExclusao = true },
                    destrutiva = true,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = NanaCores.TextoSuave,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.pri_rodape),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.TextoSuave,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmandoExclusao) {
        AlertDialog(
            onDismissRequest = { confirmandoExclusao = false },
            title = { Text(stringResource(R.string.pri_excluir_titulo)) },
            text = { Text(stringResource(R.string.pri_excluir_texto)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoExclusao = false
                        aoExcluirConta()
                    },
                ) {
                    Text(stringResource(R.string.pri_excluir_confirmar), color = NanaCores.Erro)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoExclusao = false }) {
                    Text(stringResource(R.string.cancelar), color = NanaCores.Ameixa)
                }
            },
            containerColor = NanaCores.Branco,
        )
    }
}

// =============================================================================
// Ajuda e contato
// =============================================================================

private data class Pergunta(val titulo: Int, val resposta: Int)

private val perguntas = listOf(
    Pergunta(R.string.aju_q1, R.string.aju_r1),
    Pergunta(R.string.aju_q2, R.string.aju_r2),
    Pergunta(R.string.aju_q3, R.string.aju_r3),
    Pergunta(R.string.aju_q4, R.string.aju_r4),
)

@Composable
fun AjudaScreen(
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
    aoEnviarEmail: () -> Unit,
    aoLigar: (String) -> Unit,
) {
    var busca by remember { mutableStateOf("") }
    var abertaIndice by remember { mutableStateOf(-1) }

    val textos = perguntas.map { stringResource(it.titulo) + " " + stringResource(it.resposta) }
    val termo = busca.trim()
    val visiveis = perguntas.indices.filter {
        termo.isEmpty() || textos[it].contains(termo, ignoreCase = true)
    }

    TelaNana {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            CabecalhoVoltar(stringResource(R.string.aju_titulo), aoVoltar)
            Text(
                stringResource(R.string.aju_apoio),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(16.dp))
            CampoBusca(busca, { busca = it }, stringResource(R.string.aju_busca))
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.aju_faq))
            Spacer(Modifier.height(8.dp))

            if (visiveis.isEmpty()) {
                Text(
                    stringResource(R.string.aju_sem_resultado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanaCores.TextoSuave,
                )
            } else {
                CartaoDeLinhas {
                    visiveis.forEachIndexed { posicao, indice ->
                        if (posicao > 0) Separador()
                        PerguntaExpansivel(
                            pergunta = perguntas[indice],
                            aberta = abertaIndice == indice,
                            aoTocar = { abertaIndice = if (abertaIndice == indice) -1 else indice },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            TituloSecao(stringResource(R.string.aju_fale))
            Spacer(Modifier.height(8.dp))
            CartaoDeLinhas {
                LinhaAcao(
                    icone = Icons.Outlined.ChatBubbleOutline,
                    titulo = stringResource(R.string.aju_mensagem),
                    descricao = stringResource(R.string.aju_mensagem_desc),
                    aoTocar = { aoAvisar(R.string.pri_precisa_conta) },
                )
                Separador()
                LinhaAcao(
                    icone = Icons.Outlined.HelpOutline,
                    titulo = stringResource(R.string.aju_email),
                    descricao = stringResource(R.string.aju_email_endereco),
                    aoTocar = aoEnviarEmail,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.aju_crise_titulo),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = NanaCores.AmeixaEscura,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                TextButton(onClick = { aoLigar("188") }) {
                    Text(
                        stringResource(R.string.aju_crise_cvv),
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.Ameixa,
                    )
                }
                TextButton(onClick = { aoLigar("180") }) {
                    Text(
                        stringResource(R.string.aju_crise_mulher),
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.Ameixa,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PerguntaExpansivel(pergunta: Pergunta, aberta: Boolean, aoTocar: () -> Unit) {
    Surface(onClick = aoTocar, color = NanaCores.Branco) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeArredondado {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = NanaCores.Ameixa)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(pergunta.titulo),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = NanaCores.AmeixaEscura,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NanaCores.Ameixa,
                )
            }
            if (aberta) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(pergunta.resposta),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.Texto,
                )
            }
        }
    }
}
