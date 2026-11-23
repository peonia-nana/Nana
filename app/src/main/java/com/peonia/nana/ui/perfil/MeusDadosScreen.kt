package com.peonia.nana.ui.perfil

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peonia.nana.R
import com.peonia.nana.data.ChatRepository
import com.peonia.nana.data.ConsentimentoRepository
import com.peonia.nana.data.DiarioRepository
import com.peonia.nana.data.FinalidadeConsentimento
import com.peonia.nana.data.FotoPerfil
import com.peonia.nana.data.Nuvem
import com.peonia.nana.data.Perfil
import com.peonia.nana.data.PerfilRepository
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.CabecalhoVoltar
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Meus dados: tudo o que o Nanã guarda sobre a mãe, num lugar só.
 *
 * LGPD, art. 18, I e II: confirmação da existência do tratamento e acesso
 * aos dados. Art. 8º, §5º: o consentimento pode ser retirado a qualquer
 * momento, por procedimento tão fácil quanto o de dar.
 */
class MeusDadosViewModel(app: Application) : AndroidViewModel(app) {

    private val uid = Nuvem.uid

    val email: String = Nuvem.auth.currentUser?.email.orEmpty()
    val contaCriadaEm: Instant? = Nuvem.auth.currentUser?.metadata?.creationTimestamp
        ?.let { Instant.ofEpochMilli(it) }
    val temFoto: Boolean = FotoPerfil.existe(app)

    val racaConsentida: StateFlow<Boolean> = ConsentimentoRepository().concedidos(uid)
        .map { FinalidadeConsentimento.RACA_AUTODECLARADA in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val registrosDiario: StateFlow<Int?> = DiarioRepository().registros(uid)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _chats = MutableStateFlow<Int?>(null)
    val chats: StateFlow<Int?> = _chats.asStateFlow()

    init {
        if (uid != null) {
            viewModelScope.launch {
                _chats.value = runCatching { ChatRepository().meusDados(uid).size }.getOrNull()
            }
        }
    }

    /**
     * Retira o consentimento da autodeclaração.
     *
     * A ordem importa: primeiro apaga a resposta do perfil, depois registra a
     * retirada. Ao contrário, o servidor ficaria com um dado sensível guardado
     * sem consentimento vigente, e recusaria a gravação do perfil.
     */
    fun retirarConsentimentoRaca(perfil: Perfil, aoAvisar: (Int) -> Unit) {
        val uid = uid ?: return
        viewModelScope.launch {
            val ok = runCatching {
                val repo = PerfilRepository()
                repo.salvar(uid, perfil.copy(autodeclaracao = null))
                ConsentimentoRepository().registrar(uid, FinalidadeConsentimento.RACA_AUTODECLARADA, false)
                repo.registrarAuditoria(uid, "CONSENTIMENTO_ALTERADO", listOf("autodeclaracao"))
            }.isSuccess
            aoAvisar(if (ok) R.string.md_consentimento_retirado else R.string.sync_erro)
        }
    }
}

@Composable
fun MeusDadosScreen(
    perfil: Perfil,
    aoVoltar: () -> Unit,
    aoEditar: () -> Unit,
    aoBaixarDados: () -> Unit,
    aoAvisar: (Int) -> Unit,
    vm: MeusDadosViewModel = viewModel(),
) {
    val racaConsentida by vm.racaConsentida.collectAsStateWithLifecycle()
    val registros by vm.registrosDiario.collectAsStateWithLifecycle()
    val chats by vm.chats.collectAsStateWithLifecycle()
    var confirmandoRetirada by remember { mutableStateOf(false) }

    val naoInformado = stringResource(R.string.md_nao_informado)
    val carregando = stringResource(R.string.md_carregando)

    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(stringResource(R.string.md_editar), aoEditar)
                TextButton(onClick = aoBaixarDados, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(R.string.pri_baixar),
                        color = NanaCores.Ameixa,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            },
        ) {
            CabecalhoVoltar(titulo = stringResource(R.string.per_meus_dados), aoVoltar = aoVoltar)
            Text(
                stringResource(R.string.md_apoio),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )

            Spacer(Modifier.height(20.dp))
            TituloSecao(stringResource(R.string.md_conta))
            Spacer(Modifier.height(8.dp))
            Cartao {
                LinhaDado(stringResource(R.string.md_email), vm.email.ifBlank { naoInformado })
                Divisor()
                LinhaDado(
                    stringResource(R.string.md_criada_em),
                    vm.contaCriadaEm?.let(::dataLonga) ?: naoInformado,
                )
            }

            Spacer(Modifier.height(20.dp))
            TituloSecao(stringResource(R.string.md_cadastro))
            Spacer(Modifier.height(8.dp))
            Cartao {
                LinhaDado(stringResource(R.string.edi_nome), perfil.nome.ifBlank { naoInformado })
                Divisor()
                LinhaDado(stringResource(R.string.cad_idade_label), perfil.idade?.toString() ?: naoInformado)
                Divisor()
                LinhaDado(stringResource(R.string.edi_cidade), perfil.cidade.ifBlank { naoInformado })
                Divisor()
                LinhaDado(
                    stringResource(R.string.edi_momento),
                    perfil.fase?.let { stringResource(it.rotulo) } ?: naoInformado,
                )
                Divisor()
                LinhaDado(
                    stringResource(R.string.cad_filhos_label),
                    perfil.quantidadeFilhos?.let { stringResource(it.rotulo) } ?: naoInformado,
                )
                Divisor()
                LinhaDado(stringResource(R.string.cad_bebe_label), perfil.nomesBebes.ifBlank { naoInformado })
                Divisor()
                LinhaDado(
                    stringResource(R.string.md_autodeclaracao),
                    perfil.autodeclaracao?.let { stringResource(it.rotulo) } ?: naoInformado,
                )
                Divisor()
                val interesses = perfil.interesses.sortedBy { it.ordinal }.map { stringResource(it.rotulo) }
                LinhaDado(
                    stringResource(R.string.edi_procura),
                    interesses.joinToString(", ").ifBlank { naoInformado },
                )
            }

            Spacer(Modifier.height(20.dp))
            TituloSecao(stringResource(R.string.md_consentimentos))
            Spacer(Modifier.height(8.dp))
            Cartao {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.md_consentimento_raca),
                        style = MaterialTheme.typography.titleSmall,
                        color = NanaCores.AmeixaEscura,
                    )
                    Text(
                        stringResource(
                            if (racaConsentida) R.string.md_consentido else R.string.md_nao_consentido,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.TextoSuave,
                    )
                    if (racaConsentida) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { confirmandoRetirada = true },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.5.dp, NanaCores.Ameixa),
                            modifier = Modifier.heightIn(min = 44.dp),
                        ) {
                            Text(
                                stringResource(R.string.md_retirar),
                                color = NanaCores.Ameixa,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            TituloSecao(stringResource(R.string.md_outros))
            Spacer(Modifier.height(8.dp))
            Cartao {
                LinhaDado(
                    stringResource(R.string.md_diario),
                    registros?.let { pluralStringResource(R.plurals.md_registros, it, it) } ?: carregando,
                )
                Divisor()
                LinhaDado(
                    stringResource(R.string.md_chats),
                    chats?.let { pluralStringResource(R.plurals.md_chats_qtd, it, it) } ?: carregando,
                )
                Divisor()
                LinhaDado(
                    stringResource(R.string.md_foto),
                    stringResource(if (vm.temFoto) R.string.md_foto_aparelho else R.string.md_foto_nenhuma),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.md_rodape),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmandoRetirada) {
        AlertDialog(
            onDismissRequest = { confirmandoRetirada = false },
            title = { Text(stringResource(R.string.md_retirar_titulo)) },
            text = { Text(stringResource(R.string.md_retirar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoRetirada = false
                    vm.retirarConsentimentoRaca(perfil, aoAvisar)
                }) {
                    Text(stringResource(R.string.md_retirar), color = NanaCores.Ameixa)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoRetirada = false }) {
                    Text(stringResource(R.string.cancelar), color = NanaCores.TextoSuave)
                }
            },
            containerColor = NanaCores.Branco,
        )
    }
}

@Composable
private fun Cartao(conteudo: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
    ) {
        Column { conteudo() }
    }
}

@Composable
private fun Divisor() {
    HorizontalDivider(color = NanaCores.Fundo, thickness = 1.dp)
}

@Composable
private fun LinhaDado(rotulo: String, valor: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = NanaCores.TextoSuave,
            modifier = Modifier.weight(0.45f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            color = NanaCores.Texto,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f),
        )
    }
}

private val formatoDataLonga = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

private fun dataLonga(instante: Instant): String =
    instante.atZone(ZoneId.systemDefault()).format(formatoDataLonga)
