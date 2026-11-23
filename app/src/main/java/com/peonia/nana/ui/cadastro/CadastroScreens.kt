package com.peonia.nana.ui.cadastro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peonia.nana.R
import com.peonia.nana.data.Autodeclaracao
import com.peonia.nana.data.CampoCadastro
import com.peonia.nana.data.FaseMaternidade
import com.peonia.nana.data.Interesse
import com.peonia.nana.data.QuantidadeFilhos
import com.peonia.nana.data.RegrasCadastro
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.BotaoVoltar
import com.peonia.nana.ui.components.CampoTexto
import com.peonia.nana.ui.components.CartaoAviso
import com.peonia.nana.ui.components.ChipNana
import com.peonia.nana.ui.components.IndicadorPaginas
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.RotuloCampo
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.onboarding.TOTAL_PAGINAS_JORNADA
import com.peonia.nana.ui.theme.NanaCores

private fun opcional(campo: CampoCadastro) = !RegrasCadastro.eObrigatorio(campo)

/** Cabeçalho comum dos passos: voltar, título e "Passo X de 3". */
@Composable
private fun CabecalhoPasso(titulo: String, passo: PassoCadastro, aoVoltar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BotaoVoltar(aoVoltar)
        Text(
            stringResource(R.string.passo_x_de_y, passo.numero, PassoCadastro.total),
            style = MaterialTheme.typography.bodySmall,
            color = NanaCores.TextoSuave,
        )
    }
    Text(
        titulo,
        style = MaterialTheme.typography.titleLarge,
        color = NanaCores.Ameixa,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun RodapePasso(textoBotao: String, aoAvancar: () -> Unit) {
    BotaoPrincipal(textoBotao, aoAvancar)
    Spacer(Modifier.height(20.dp))
    IndicadorPaginas(TOTAL_PAGINAS_JORNADA, 3)
}

@Composable
private fun MensagemErro(texto: String) {
    Text(
        texto,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .padding(top = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

// ---------------------------------------------------------------------------
// 04a – Nome, idade e cidade (+ aviso de privacidade e do que será pedido)
// ---------------------------------------------------------------------------
@Composable
fun CadastroNomeScreen(vm: CadastroViewModel, aoAvancar: () -> Unit, aoVoltar: () -> Unit) {
    val s by vm.estado.collectAsStateWithLifecycle()
    val erroObrigatorio = stringResource(R.string.erro_campo_obrigatorio)
    TelaNana {
        LayoutComRodape(
            rodape = {
                RodapePasso(stringResource(R.string.proximo)) {
                    if (vm.validarPasso(PassoCadastro.NOME)) aoAvancar()
                }
            },
        ) {
            CabecalhoPasso(stringResource(R.string.cad_nome_titulo), PassoCadastro.NOME, aoVoltar)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.cad_nome_texto), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            CartaoAviso(
                titulo = stringResource(R.string.cad_privacidade_titulo),
                texto = stringResource(R.string.cad_privacidade_texto),
                icone = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = NanaCores.Ameixa) },
            )
            Spacer(Modifier.height(20.dp))

            RotuloCampo(
                stringResource(R.string.cad_nome_label),
                stringResource(R.string.cad_nome_ajuda),
                opcional(CampoCadastro.NOME),
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                s.nome, vm::alterarNome, stringResource(R.string.cad_nome_placeholder),
                erro = if (CampoCadastro.NOME in s.camposComErro) erroObrigatorio else null,
            )
            Spacer(Modifier.height(16.dp))

            RotuloCampo(
                stringResource(R.string.cad_idade_label),
                stringResource(R.string.cad_idade_ajuda),
                opcional(CampoCadastro.IDADE),
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                s.idade, vm::alterarIdade, stringResource(R.string.cad_idade_placeholder),
                tipoTeclado = KeyboardType.Number,
                erro = if (CampoCadastro.IDADE in s.camposComErro) erroObrigatorio else null,
            )
            Spacer(Modifier.height(16.dp))

            RotuloCampo(
                stringResource(R.string.cad_cidade_label),
                stringResource(R.string.cad_cidade_ajuda),
                opcional(CampoCadastro.CIDADE),
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                s.cidade, vm::alterarCidade, stringResource(R.string.cad_cidade_placeholder),
                acao = ImeAction.Done,
                erro = if (CampoCadastro.CIDADE in s.camposComErro) erroObrigatorio else null,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 04b – Fase da maternidade
// ---------------------------------------------------------------------------
@Composable
fun CadastroMomentoScreen(vm: CadastroViewModel, aoAvancar: () -> Unit, aoVoltar: () -> Unit) {
    val s by vm.estado.collectAsStateWithLifecycle()
    TelaNana {
        LayoutComRodape(
            rodape = {
                RodapePasso(stringResource(R.string.proximo)) {
                    if (vm.validarPasso(PassoCadastro.MOMENTO)) aoAvancar()
                }
            },
        ) {
            CabecalhoPasso(stringResource(R.string.cad_momento_titulo), PassoCadastro.MOMENTO, aoVoltar)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.cad_momento_ajuda) +
                    if (opcional(CampoCadastro.FASE)) " " + stringResource(R.string.opcional) else "",
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FaseMaternidade.entries.forEach { fase ->
                    OpcaoFase(
                        texto = stringResource(fase.rotulo),
                        selecionada = s.fase == fase,
                        aoSelecionar = { vm.escolherFase(fase) },
                    )
                }
            }
            if (CampoCadastro.FASE in s.camposComErro) {
                MensagemErro(stringResource(R.string.erro_fase))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OpcaoFase(texto: String, selecionada: Boolean, aoSelecionar: () -> Unit) {
    val forma = RoundedCornerShape(24.dp)
    Surface(
        shape = forma,
        color = if (selecionada) NanaCores.CartaoDestaque else NanaCores.Branco,
        border = if (selecionada) BorderStroke(1.5.dp, NanaCores.Ameixa) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(forma)
            .selectable(selected = selecionada, role = Role.RadioButton, onClick = aoSelecionar),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selecionada,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = NanaCores.Ameixa),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Normal,
                color = NanaCores.Texto,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 04c – Interesses, bebê, filhos e autodeclaração racial
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CadastroPerfilScreen(
    vm: CadastroViewModel,
    aoConcluir: () -> Unit,
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
) {
    val s by vm.estado.collectAsStateWithLifecycle()
    val erroObrigatorio = stringResource(R.string.erro_campo_obrigatorio)
    TelaNana {
        LayoutComRodape(
            rodape = {
                RodapePasso(stringResource(R.string.cad_entrar)) {
                    vm.concluir(aoConcluir = aoConcluir, aoFalhar = aoAvisar)
                }
            },
        ) {
            CabecalhoPasso(stringResource(R.string.cad_perfil_titulo), PassoCadastro.PERFIL, aoVoltar)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.cad_interesses_ajuda) +
                    if (opcional(CampoCadastro.INTERESSES)) " " + stringResource(R.string.opcional) else "",
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Interesse.entries.forEach { i ->
                    ChipNana(
                        stringResource(i.rotulo),
                        selecionado = i in s.interesses,
                        multipla = true,
                        onClick = { vm.alternarInteresse(i) },
                    )
                }
            }
            if (CampoCadastro.INTERESSES in s.camposComErro) MensagemErro(erroObrigatorio)
            Spacer(Modifier.height(20.dp))

            RotuloCampo(
                stringResource(R.string.cad_bebe_label),
                stringResource(R.string.cad_bebe_ajuda),
                opcional(CampoCadastro.NOME_BEBE),
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                s.nomesBebes, vm::alterarNomesBebes, stringResource(R.string.cad_bebe_placeholder),
                acao = ImeAction.Done,
                erro = if (CampoCadastro.NOME_BEBE in s.camposComErro) erroObrigatorio else null,
            )
            Spacer(Modifier.height(16.dp))

            RotuloCampo(
                stringResource(R.string.cad_filhos_label),
                stringResource(R.string.cad_filhos_ajuda),
                opcional(CampoCadastro.QUANTIDADE_FILHOS),
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuantidadeFilhos.entries.forEach { q ->
                    ChipNana(
                        stringResource(q.rotulo),
                        selecionado = s.quantidadeFilhos == q,
                        onClick = { vm.alternarQuantidadeFilhos(q) },
                    )
                }
            }
            if (CampoCadastro.QUANTIDADE_FILHOS in s.camposComErro) MensagemErro(erroObrigatorio)
            Spacer(Modifier.height(20.dp))

            RotuloCampo(
                stringResource(R.string.cad_raca_label),
                stringResource(R.string.cad_raca_ajuda),
                opcional(CampoCadastro.AUTODECLARACAO),
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Autodeclaracao.entries.forEach { a ->
                    ChipNana(
                        stringResource(a.rotulo),
                        selecionado = s.autodeclaracao == a,
                        onClick = { vm.alternarAutodeclaracao(a) },
                    )
                }
            }
            if (s.autodeclaracao != null) {
                Spacer(Modifier.height(10.dp))
                CaixaDeAceite(
                    texto = stringResource(R.string.cad_raca_consentir),
                    marcada = s.consentiuRaca,
                    aoMudar = vm::alternarConsentimentoRaca,
                )
            }
            if (CampoCadastro.AUTODECLARACAO in s.camposComErro) {
                MensagemErro(stringResource(R.string.cad_raca_exigida))
            }
            Spacer(Modifier.height(20.dp))

            CartaoAviso(titulo = null, texto = stringResource(R.string.cad_perfil_nota))
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Caixa de aceite para um dado sensível.
 *
 * Fica junto do campo, e não escondida numa tela de termos, porque a LGPD
 * exige que o consentimento para dado sensível seja específico e destacado
 * (art. 11, I). Um aceite genérico lá no começo não serviria.
 */
@Composable
private fun CaixaDeAceite(texto: String, marcada: Boolean, aoMudar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = { aoMudar() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = marcada,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = NanaCores.Ameixa),
        )
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = NanaCores.Texto)
    }
}
