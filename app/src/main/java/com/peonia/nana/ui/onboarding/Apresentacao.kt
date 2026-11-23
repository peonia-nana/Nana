package com.peonia.nana.ui.onboarding

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peonia.nana.R
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.BotaoVoltar
import com.peonia.nana.ui.components.CartaoAviso
import com.peonia.nana.ui.components.ChipNana
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.components.IndicadorPaginas
import com.peonia.nana.ui.components.LayoutComRodape
import com.peonia.nana.ui.components.TelaNana
import com.peonia.nana.ui.components.temImagem
import com.peonia.nana.ui.components.TextoCentral
import com.peonia.nana.ui.components.textoComDestaques
import com.peonia.nana.ui.theme.NanaCores

/** Total de pontos do indicador: 3 telas de apresentação + cadastro. */
const val TOTAL_PAGINAS_JORNADA = 4

// ---------------------------------------------------------------------------
// 01 – Boas-vindas
// ---------------------------------------------------------------------------
@Composable
fun BoasVindasScreen(aoComecar: () -> Unit, aoJaTenhoConta: () -> Unit) {
    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(stringResource(R.string.bv_comecar), aoComecar)
                TextButton(onClick = aoJaTenhoConta, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        stringResource(R.string.acc_ir_entrar),
                        color = NanaCores.Verde,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                IndicadorPaginas(TOTAL_PAGINAS_JORNADA, 0)
            },
        ) {
            Spacer(Modifier.height(32.dp))
            ImagemNana(
                nome = Imagens.BOAS_VINDAS,
                descricao = stringResource(R.string.ilustracao_boas_vindas),
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.bv_marca),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(20.dp))
            TextoCentral(
                textoComDestaques(
                    texto = stringResource(R.string.bv_texto),
                    destaques = stringArrayResource(R.array.bv_destaques).toList(),
                ),
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 02 – Propósito
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropositoScreen(aoContinuar: () -> Unit, aoVoltar: () -> Unit) {
    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(stringResource(R.string.continuar), aoContinuar)
                Spacer(Modifier.height(20.dp))
                IndicadorPaginas(TOTAL_PAGINAS_JORNADA, 1)
            },
        ) {
            BotaoVoltar(aoVoltar)
            ImagemNana(
                // Enquanto não houver uma ilustração própria para esta tela,
                // usa a mesma das boas-vindas, como no protótipo.
                nome = if (temImagem(Imagens.PROPOSITO)) Imagens.PROPOSITO else Imagens.BOAS_VINDAS,
                descricao = stringResource(R.string.ilustracao_proposito),
                modifier = Modifier
                    .size(170.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            val primeiraLinha = stringResource(R.string.prop_titulo_1)
            val segundaLinha = textoComDestaques(
                texto = stringResource(R.string.prop_titulo_2),
                destaques = stringArrayResource(R.array.prop_destaques).toList(),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = NanaCores.Texto)) { append(primeiraLinha) }
                    append("\n")
                    // A cor roxa vale para a linha toda; as palavras em verde
                    // trazem a própria cor e sobrescrevem este trecho.
                    withStyle(SpanStyle(color = NanaCores.Ameixa)) { append(segundaLinha) }
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.prop_texto), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(0.75f),
            ) {
                listOf(
                    R.string.prop_acolhimento,
                    R.string.prop_informacao,
                    R.string.prop_comunidade,
                    R.string.prop_profissionais,
                    R.string.prop_direitos,
                ).forEach { ChipNana(stringResource(it), selecionado = false) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 03 – Áreas principais (Conteúdos, Comunidade, Profissionais, Diário)
// ---------------------------------------------------------------------------
private data class Area(val icone: ImageVector, val titulo: Int, val descricao: Int)

private val areas = listOf(
    Area(Icons.AutoMirrored.Outlined.MenuBook, R.string.func_conteudos, R.string.func_conteudos_desc),
    Area(Icons.Outlined.Groups, R.string.func_comunidade, R.string.func_comunidade_desc),
    Area(Icons.Outlined.MedicalServices, R.string.func_profissionais, R.string.func_profissionais_desc),
    Area(Icons.Outlined.EditNote, R.string.func_diario, R.string.func_diario_desc),
)

@Composable
fun FuncionalidadesScreen(aoCadastrar: () -> Unit, aoVoltar: () -> Unit) {
    TelaNana {
        LayoutComRodape(
            rodape = {
                BotaoPrincipal(stringResource(R.string.func_cadastrar), aoCadastrar)
                Spacer(Modifier.height(20.dp))
                IndicadorPaginas(TOTAL_PAGINAS_JORNADA, 2)
            },
        ) {
            BotaoVoltar(aoVoltar)
            Text(
                stringResource(R.string.func_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = NanaCores.AmeixaEscura,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(20.dp))
            areas.forEach { area ->
                CartaoArea(area)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(4.dp))
            CartaoAviso(
                titulo = stringResource(R.string.func_limites_titulo),
                texto = stringResource(R.string.func_limites_texto),
                icone = { Icon(Icons.Outlined.Info, contentDescription = null, tint = NanaCores.Ameixa) },
            )
        }
    }
}

@Composable
private fun CartaoArea(area: Area) {
    Surface(
        color = NanaCores.CartaoRosa,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconeArredondado {
                Icon(area.icone, contentDescription = null, tint = NanaCores.Ameixa)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(area.titulo),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NanaCores.Ameixa,
                )
                Text(
                    stringResource(area.descricao),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.Texto,
                )
            }
        }
    }
}
