package com.peonia.nana.ui.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.data.Humor
import com.peonia.nana.data.Perfil
import com.peonia.nana.ui.components.ChipNana
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.temImagem
import com.peonia.nana.ui.diario.cor
import com.peonia.nana.ui.diario.imagem
import com.peonia.nana.ui.theme.NanaCores

/**
 * 05 – Tela inicial personalizada.
 *
 * O documento define só a chegada a esta tela: destaque para conteúdos da fase
 * informada e acesso fácil ao acolhimento emocional e ao diário. O que acontece
 * dentro de cada área pertence à próxima etapa da jornada. Por isso, tocar nessas
 * áreas mostra só um aviso, sem criar telas novas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InicioScreen(
    perfil: Perfil,
    humorDeHoje: Humor?,
    aoRegistrarHumor: (Humor) -> Unit,
    aoAvisar: () -> Unit,
    aoAbrirDireitos: () -> Unit,
) {
    val mostrarAviso: () -> Unit = aoAvisar

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
            Text(
                text = if (perfil.nome.isNotBlank()) {
                    stringResource(R.string.ini_ola_nome, perfil.nome)
                } else {
                    stringResource(R.string.ini_ola)
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                color = NanaCores.Ameixa,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ini_texto), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            // Acolhimento emocional: como você está hoje?
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Humor.entries.forEach { h ->
                    OpcaoHumor(
                        humor = h,
                        selecionado = humorDeHoje == h,
                        aoSelecionar = { aoRegistrarHumor(h) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (humorDeHoje != null) {
                Text(
                    stringResource(R.string.ini_humor_resposta),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.AmeixaEscura,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Spacer(Modifier.height(24.dp))

            // Destaque principal: conteúdos da fase informada no cadastro
            val faseTexto = perfil.fase?.let { stringResource(it.rotulo) }
            CartaoArea(
                icone = Icons.AutoMirrored.Outlined.MenuBook,
                titulo = stringResource(R.string.ini_momento_titulo),
                descricao = faseTexto?.let { stringResource(R.string.ini_momento_desc, it) }
                    ?: stringResource(R.string.func_conteudos_desc),
                destaque = true,
                aoTocar = mostrarAviso,
            )
            if (perfil.interesses.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.ini_seus_interesses),
                    style = MaterialTheme.typography.titleSmall,
                    color = NanaCores.Ameixa,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    perfil.interesses.sortedBy { it.ordinal }.forEach {
                        ChipNana(stringResource(it.rotulo), selecionado = true)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            CartaoArea(
                icone = Icons.Outlined.Groups,
                titulo = stringResource(R.string.ini_comunidade_titulo),
                descricao = stringResource(R.string.ini_comunidade_desc),
                aoTocar = mostrarAviso,
            )
            Spacer(Modifier.height(12.dp))
            CartaoArea(
                icone = Icons.Outlined.Balance,
                titulo = stringResource(R.string.ini_direitos_titulo),
                descricao = stringResource(R.string.ini_direitos_desc),
                // Este cartão leva para a aba Direitos, que já existe no app.
                aoTocar = aoAbrirDireitos,
            )
    }
}

@Composable
private fun CartaoArea(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    aoTocar: () -> Unit,
    destaque: Boolean = false,
) {
    Surface(
        onClick = aoTocar,
        color = if (destaque) NanaCores.CartaoDestaque else NanaCores.CartaoRosa,
        border = if (destaque) BorderStroke(1.5.dp, NanaCores.Ameixa) else null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconeArredondado { Icon(icone, contentDescription = null, tint = NanaCores.Ameixa) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NanaCores.Ameixa,
                )
                Text(descricao, style = MaterialTheme.typography.bodySmall, color = NanaCores.Texto)
            }
        }
    }
}

@Composable
private fun OpcaoHumor(
    humor: Humor,
    selecionado: Boolean,
    aoSelecionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(14.dp)
    Surface(
        shape = forma,
        color = if (selecionado) NanaCores.CartaoDestaque else NanaCores.Branco,
        border = if (selecionado) BorderStroke(1.5.dp, NanaCores.Ameixa) else null,
        modifier = modifier
            .clip(forma)
            .selectable(selected = selecionado, role = Role.RadioButton, onClick = aoSelecionar),
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Ícone enviado pela equipe; o texto do sentimento já é lido pelo leitor de tela.
            if (temImagem(humor.imagem)) {
                ImagemNana(humor.imagem, descricao = null, modifier = Modifier.size(36.dp))
            } else {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NanaCores.IconeFundo),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(humor.cor),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(humor.rotulo),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = NanaCores.Texto,
            )
        }
    }
}
