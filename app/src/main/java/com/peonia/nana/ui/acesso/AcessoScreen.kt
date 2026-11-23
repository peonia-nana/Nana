package com.peonia.nana.ui.acesso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peonia.nana.R
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.BotaoVoltar
import com.peonia.nana.ui.components.CampoTexto
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.theme.NanaCores

/**
 * Tela de acesso: entrar numa conta existente ou criar uma nova.
 *
 * A caixa de aceite de privacidade aparece só ao **criar conta**, que é onde
 * ela tem valor: é ali que a mãe concorda com o tratamento dos dados, e o
 * aceite fica registrado. Repetir a caixa a cada login não acrescentaria
 * nada — vira um clique automático que ninguém lê.
 */
@Composable
fun AcessoScreen(
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
    vm: AcessoViewModel = viewModel(),
) {
    val s by vm.estado.collectAsStateWithLifecycle()
    val criando = s.modo == ModoAcesso.CRIAR_CONTA

    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(
                    texto = stringResource(if (criando) R.string.acc_criar else R.string.acc_entrar),
                    onClick = { vm.enviar(aoAvisar) },
                )
                TextButton(
                    onClick = { vm.trocarModo() },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(
                            if (criando) R.string.acc_ir_entrar else R.string.acc_ir_criar,
                        ),
                        color = NanaCores.Ameixa,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            },
        ) {
            BotaoVoltar(aoVoltar)

            ImagemNana(
                nome = Imagens.BOAS_VINDAS,
                descricao = null,
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(if (criando) R.string.acc_titulo_criar else R.string.acc_titulo_entrar),
                style = MaterialTheme.typography.headlineSmall,
                color = NanaCores.AmeixaEscura,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (criando) R.string.acc_apoio_criar else R.string.acc_apoio_entrar),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.TextoSuave,
            )
            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.acc_email),
                style = MaterialTheme.typography.titleSmall,
                color = NanaCores.Ameixa,
            )
            Spacer(Modifier.height(8.dp))
            CampoTexto(
                valor = s.email,
                aoMudar = vm::alterarEmail,
                placeholder = stringResource(R.string.acc_email_dica),
                tipoTeclado = KeyboardType.Email,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.acc_senha),
                style = MaterialTheme.typography.titleSmall,
                color = NanaCores.Ameixa,
            )
            Spacer(Modifier.height(8.dp))
            CampoSenha(
                valor = s.senha,
                aoMudar = vm::alterarSenha,
                visivel = s.senhaVisivel,
                aoAlternar = vm::alternarSenhaVisivel,
            )

            if (criando) {
                Spacer(Modifier.height(20.dp))
                CaixaDePrivacidade(
                    marcada = s.privacidadeAceita,
                    aoMudar = { vm.alternarPrivacidade() },
                )
            } else {
                TextButton(onClick = { vm.recuperarSenha(aoAvisar) }) {
                    Text(
                        stringResource(R.string.acc_esqueci),
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.Ameixa,
                    )
                }
            }

            if (s.erro != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(s.erro!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            if (s.ocupada) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterHorizontally),
                    color = NanaCores.Ameixa,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CampoSenha(
    valor: String,
    aoMudar: (String) -> Unit,
    visivel: Boolean,
    aoAlternar: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CampoTexto(
            valor = valor,
            aoMudar = aoMudar,
            placeholder = stringResource(R.string.acc_senha_dica),
            modifier = Modifier.weight(1f),
            tipoTeclado = KeyboardType.Password,
            acao = ImeAction.Done,
            transformacao = if (visivel) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = aoAlternar) {
            Icon(
                if (visivel) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = stringResource(
                    if (visivel) R.string.acc_ocultar_senha else R.string.acc_mostrar_senha,
                ),
                tint = NanaCores.Ameixa,
            )
        }
    }
}

/**
 * Aceite do tratamento de dados.
 *
 * O resumo fica visível junto da caixa, em vez de escondido atrás de um link:
 * a LGPD pede informação clara e acessível (art. 9º), e ninguém lê política
 * que exige sair da tela para encontrar.
 */
@Composable
private fun CaixaDePrivacidade(marcada: Boolean, aoMudar: () -> Unit) {
    val rotulo = stringResource(R.string.acc_privacidade_aceite)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.acc_privacidade_resumo),
                style = MaterialTheme.typography.bodySmall,
                color = NanaCores.TextoSuave,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = marcada,
                        role = Role.Checkbox,
                        onValueChange = { aoMudar() },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = marcada,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = NanaCores.Ameixa),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    rotulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanaCores.Texto,
                )
            }
        }
    }
}
