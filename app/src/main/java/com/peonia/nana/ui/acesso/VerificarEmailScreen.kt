package com.peonia.nana.ui.acesso

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.data.AutenticacaoRepository
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.CartaoAviso
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.theme.NanaCores
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Passo entre criar a conta e usar o app.
 *
 * As regras do servidor só aceitam gravação com o e-mail verificado, então
 * sem este passo o cadastro seria recusado. A tela explica o motivo em vez de
 * só mandar confirmar: verificar o endereço é o que impede alguém de criar
 * uma conta com o e-mail de outra pessoa.
 */
@Composable
fun VerificarEmailScreen(
    email: String,
    aoVerificar: () -> Unit,
    aoSair: () -> Unit,
    aoAvisar: (Int) -> Unit,
) {
    val autenticacao = remember { AutenticacaoRepository() }
    val escopo = rememberCoroutineScope()
    var conferindo by remember { mutableStateOf(false) }

    // Confere sozinho a cada 4 segundos: quem toca no link do e-mail e volta
    // para o app segue em frente sem precisar apertar nada.
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            if (!conferindo && autenticacao.conferirVerificacao()) break
        }
    }

    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(
                    texto = stringResource(R.string.ver_ja_confirmei),
                    onClick = {
                        if (!conferindo) {
                            conferindo = true
                            escopo.launch {
                                val ok = autenticacao.conferirVerificacao()
                                conferindo = false
                                if (ok) aoVerificar() else aoAvisar(R.string.ver_ainda_nao)
                            }
                        }
                    },
                )
                TextButton(
                    onClick = {
                        escopo.launch {
                            autenticacao.reenviarVerificacao()
                            aoAvisar(R.string.ver_reenviado)
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(R.string.ver_reenviar),
                        color = NanaCores.Ameixa,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                TextButton(onClick = aoSair, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(R.string.ver_sair),
                        color = NanaCores.TextoSuave,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
        ) {
            Spacer(Modifier.height(24.dp))
            ImagemNana(
                nome = Imagens.BOAS_VINDAS,
                descricao = null,
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.ver_titulo),
                style = MaterialTheme.typography.headlineSmall,
                color = NanaCores.AmeixaEscura,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.ver_texto, email),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.Texto,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            CartaoAviso(titulo = null, texto = stringResource(R.string.ver_motivo))
            Spacer(Modifier.height(16.dp))
        }
    }
}
