package com.peonia.nana.ui.direitos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.data.TemaDireito
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.components.temImagem
import com.peonia.nana.ui.components.CampoBusca
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores

/**
 * 07 – Direitos: busca e lista de temas.
 *
 * A busca procura no título, na descrição e no texto dos cards de cada tema.
 */
@Composable
fun DireitosScreen(aoAbrirTema: (TemaDireito) -> Unit) {
    var busca by rememberSaveable { mutableStateOf("") }

    // Pré-carrega os textos para que a busca também encontre o conteúdo dos cards.
    // Usa laços simples porque stringResource só pode ser chamado em contexto Composable.
    val temas = TemaDireito.entries
    val textos = ArrayList<String>(temas.size)
    for (tema in temas) {
        val sb = StringBuilder()
        sb.append(stringResource(tema.titulo)).append(' ')
        sb.append(stringResource(tema.descricao))
        for (card in tema.cards) {
            sb.append(' ').append(stringResource(card.titulo))
            sb.append(' ').append(stringResource(card.selo))
            sb.append(' ').append(stringResource(card.texto))
        }
        textos.add(sb.toString())
    }

    val termo = busca.trim()
    val filtrados = if (termo.isEmpty()) {
        temas
    } else {
        temas.filterIndexed { i, _ -> textos[i].contains(termo, ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.dir_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = NanaCores.AmeixaEscura,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.dir_apoio),
            style = MaterialTheme.typography.bodySmall,
            color = NanaCores.TextoSuave,
        )
        Spacer(Modifier.height(16.dp))
        CampoBusca(busca, { busca = it }, stringResource(R.string.dir_busca))
        Spacer(Modifier.height(20.dp))
        TituloSecao(stringResource(R.string.dir_lista_temas))
        Spacer(Modifier.height(10.dp))

        if (filtrados.isEmpty()) {
            Text(
                stringResource(R.string.dir_sem_resultado),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.TextoSuave,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filtrados.forEach { tema ->
                    CartaoTema(tema = tema, aoTocar = { aoAbrirTema(tema) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Nome do arquivo de foto que a equipe envia para cada tema. */
val TemaDireito.foto: String
    get() = when (this) {
        TemaDireito.ACOMPANHANTE -> Imagens.DIREITO_ACOMPANHANTE
        TemaDireito.DIGNIDADE -> Imagens.DIREITO_DIGNIDADE
        TemaDireito.VINCULO -> Imagens.DIREITO_VINCULO
        TemaDireito.POS_PARTO -> Imagens.DIREITO_POS_PARTO
    }

/**
 * Card de um tema: a linha com ícone e título e, embaixo, a foto enviada pela
 * equipe. Enquanto o arquivo não existir, o card fica só com a linha.
 */
@Composable
private fun CartaoTema(tema: TemaDireito, aoTocar: () -> Unit) {
    val titulo = stringResource(tema.titulo)
    Surface(
        onClick = aoTocar,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
    ) {
        Column {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconeArredondado {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = NanaCores.Ameixa,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = NanaCores.AmeixaEscura,
                    )
                    Text(
                        stringResource(tema.descricao),
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.TextoSuave,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NanaCores.Ameixa,
                )
            }
            if (temImagem(tema.foto)) {
                ImagemNana(
                    nome = tema.foto,
                    // A foto é decorativa: o título ao lado já diz tudo, e
                    // repetir a descrição só atrapalha quem usa leitor de tela.
                    descricao = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Mesma proporção do card no Figma (353 x 209).
                        .aspectRatio(353f / 209f)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                    escala = ContentScale.Crop,
                )
            }
        }
    }
}
