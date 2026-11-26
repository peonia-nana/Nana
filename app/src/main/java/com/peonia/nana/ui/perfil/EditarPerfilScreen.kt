package com.peonia.nana.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.data.FaseMaternidade
import com.peonia.nana.data.Interesse
import com.peonia.nana.data.Perfil
import com.peonia.nana.data.QuantidadeFilhos
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peonia.nana.data.FotoPerfil
import com.peonia.nana.ui.components.AvatarPerfil
import kotlinx.coroutines.launch
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.CabecalhoVoltar
import com.peonia.nana.ui.components.CampoTexto
import com.peonia.nana.ui.components.ChipNana
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores

/**
 * Editar perfil.
 *
 * Mexe só nos campos que a usuária já informou no cadastro. A autodeclaração
 * racial e a idade não aparecem aqui porque o protótipo não as inclui nesta
 * tela; elas continuam guardadas como foram informadas.
 *
 * O protótipo também mostra "Alterar foto". O app não guarda foto de perfil,
 * e isso precisa ser decidido pela equipe antes de ser implementado, por
 * envolver dado pessoal sensível. Por isso não foi incluído.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarPerfilScreen(
    perfil: Perfil,
    aoSalvar: (Perfil) -> Unit,
    aoCancelar: () -> Unit,
    aoAvisar: (Int) -> Unit,
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val versaoFoto by FotoPerfil.versao.collectAsStateWithLifecycle()
    val temFoto = remember(versaoFoto) { FotoPerfil.existe(contexto) }

    // Seletor de fotos do Android: não pede permissão de acesso à galeria,
    // e só a imagem escolhida chega ao aplicativo.
    val seletorDeFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            escopo.launch {
                if (!FotoPerfil.salvar(contexto, uri)) aoAvisar(R.string.foto_erro)
            }
        }
    }

    var nome by rememberSaveable { mutableStateOf(perfil.nome) }
    // Nome é obrigatório (RegrasCadastro), também ao editar.
    var nomeComErro by rememberSaveable { mutableStateOf(false) }
    var cidade by rememberSaveable { mutableStateOf(perfil.cidade) }
    var faseNome by rememberSaveable { mutableStateOf(perfil.fase?.name ?: "") }
    var filhosNome by rememberSaveable { mutableStateOf(perfil.quantidadeFilhos?.name ?: "") }
    var interessesNomes by rememberSaveable {
        mutableStateOf(perfil.interesses.joinToString(",") { it.name })
    }

    val interesses = interessesNomes.split(",").filter { it.isNotBlank() }.toSet()

    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(
                    texto = stringResource(R.string.edi_salvar),
                    onClick = {
                        if (nome.isBlank()) {
                            nomeComErro = true
                            return@BotaoPrincipal
                        }
                        aoSalvar(
                            perfil.copy(
                                nome = nome.trim(),
                                cidade = cidade.trim(),
                                fase = FaseMaternidade.entries.firstOrNull { it.name == faseNome },
                                quantidadeFilhos = QuantidadeFilhos.entries.firstOrNull { it.name == filhosNome },
                                interesses = Interesse.entries.filter { it.name in interesses }.toSet(),
                            ),
                        )
                    },
                )
                TextButton(onClick = aoCancelar, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(R.string.edi_cancelar),
                        color = NanaCores.Ameixa,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            },
        ) {
            CabecalhoVoltar(stringResource(R.string.edi_titulo), aoCancelar)
            Spacer(Modifier.height(16.dp))
            AvatarPerfil(
                nome = nome,
                tamanho = 80,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            TextButton(
                onClick = {
                    seletorDeFoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(R.string.foto_alterar),
                    color = NanaCores.Ameixa,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            if (temFoto) {
                TextButton(
                    onClick = {
                        escopo.launch {
                            FotoPerfil.remover(contexto)
                            aoAvisar(R.string.foto_removida)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        stringResource(R.string.foto_remover),
                        color = NanaCores.TextoSuave,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                stringResource(R.string.foto_aviso),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.edi_nome))
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                nome,
                {
                    nome = it.take(60)
                    nomeComErro = false
                },
                stringResource(R.string.cad_nome_placeholder),
                erro = if (nomeComErro) stringResource(R.string.erro_campo_obrigatorio) else null,
            )
            Spacer(Modifier.height(16.dp))

            TituloSecao(stringResource(R.string.edi_cidade))
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                cidade,
                { cidade = it.take(60) },
                stringResource(R.string.cad_cidade_placeholder),
                acao = ImeAction.Done,
            )
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.edi_momento))
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FaseMaternidade.entries.forEach { fase ->
                    ChipNana(
                        stringResource(fase.rotulo),
                        selecionado = faseNome == fase.name,
                        onClick = { faseNome = fase.name },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.edi_filhos))
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuantidadeFilhos.entries.forEach { q ->
                    ChipNana(
                        stringResource(q.rotulo),
                        selecionado = filhosNome == q.name,
                        onClick = { filhosNome = if (filhosNome == q.name) "" else q.name },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            TituloSecao(stringResource(R.string.edi_procura))
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Interesse.entries.forEach { i ->
                    ChipNana(
                        stringResource(i.rotulo),
                        selecionado = i.name in interesses,
                        multipla = true,
                        onClick = {
                            val novos = if (i.name in interesses) interesses - i.name else interesses + i.name
                            interessesNomes = novos.joinToString(",")
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
