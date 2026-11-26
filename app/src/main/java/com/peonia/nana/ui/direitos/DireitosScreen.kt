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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.PregnantWoman
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.peonia.nana.data.CategoriaConteudo
import com.peonia.nana.data.Conteudo
import com.peonia.nana.data.EstadoConteudos
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
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.components.temImagem
import com.peonia.nana.ui.components.CampoBusca
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores

/** As duas abas da tela Direitos. */
enum class AbaDireitos { DIREITOS, SAUDE }

/**
 * 07 – Direitos: busca, temas de direitos e a aba "Sua saúde".
 *
 * A aba Direitos mostra os temas do protótipo e, embaixo, os conteúdos da
 * categoria Meus direitos. A aba Sua saúde mostra os conteúdos educativos
 * de saúde, com filtro por categoria. A busca vale para a aba aberta.
 *
 * A aba e o filtro ficam com quem chama, para o Início poder abrir esta tela
 * já na aba e na categoria certas.
 */
@Composable
fun DireitosScreen(
    aba: AbaDireitos,
    aoMudarAba: (AbaDireitos) -> Unit,
    categoria: CategoriaConteudo?,
    aoMudarCategoria: (CategoriaConteudo?) -> Unit,
    conteudos: EstadoConteudos,
    aoAbrirTema: (TemaDireito) -> Unit,
    aoAbrirConteudo: (Conteudo) -> Unit,
) {
    var busca by rememberSaveable { mutableStateOf("") }
    val termo = busca.trim()

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
        SeletorDeAba(aba, aoMudarAba)
        Spacer(Modifier.height(14.dp))
        CampoBusca(busca, { busca = it }, stringResource(R.string.dir_busca))
        Spacer(Modifier.height(16.dp))

        when (aba) {
            AbaDireitos.DIREITOS -> AbaTemas(termo, conteudos, aoAbrirTema, aoAbrirConteudo)
            AbaDireitos.SAUDE -> AbaSaude(termo, categoria, aoMudarCategoria, conteudos, aoAbrirConteudo)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SeletorDeAba(aba: AbaDireitos, aoMudar: (AbaDireitos) -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = NanaCores.Branco,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(4.dp)
                .selectableGroup(),
        ) {
            listOf(
                AbaDireitos.DIREITOS to R.string.dir_aba_direitos,
                AbaDireitos.SAUDE to R.string.dir_aba_saude,
            ).forEach { (opcao, rotulo) ->
                val marcada = opcao == aba
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (marcada) NanaCores.Ameixa else NanaCores.Branco,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(50))
                        .selectable(selected = marcada, role = Role.Tab, onClick = { aoMudar(opcao) }),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(rotulo),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (marcada) NanaCores.Branco else NanaCores.Ameixa,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbaTemas(
    termo: String,
    conteudos: EstadoConteudos,
    aoAbrirTema: (TemaDireito) -> Unit,
    aoAbrirConteudo: (Conteudo) -> Unit,
) {
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
    val filtrados = if (termo.isEmpty()) temas
    else temas.filterIndexed { i, _ -> textos[i].contains(termo, ignoreCase = true) }

    val leiaTambem = conteudos.lista.orEmpty()
        .filter { it.categoria == CategoriaConteudo.MEUS_DIREITOS && it.combina(termo) }

    if (filtrados.isEmpty() && leiaTambem.isEmpty()) {
        Text(
            stringResource(R.string.dir_sem_resultado),
            style = MaterialTheme.typography.bodyMedium,
            color = NanaCores.TextoSuave,
        )
        return
    }
    if (filtrados.isNotEmpty()) {
        TituloSecao(stringResource(R.string.dir_lista_temas))
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtrados.forEach { tema -> CartaoTema(tema = tema, aoTocar = { aoAbrirTema(tema) }) }
        }
    }
    if (leiaTambem.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        TituloSecao(stringResource(R.string.dir_leia_tambem))
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            leiaTambem.forEach { c -> CartaoConteudo(c) { aoAbrirConteudo(c) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbaSaude(
    termo: String,
    categoria: CategoriaConteudo?,
    aoMudarCategoria: (CategoriaConteudo?) -> Unit,
    conteudos: EstadoConteudos,
    aoAbrirConteudo: (Conteudo) -> Unit,
) {
    val categorias = listOf(
        CategoriaConteudo.SAUDE_NA_GESTACAO,
        CategoriaConteudo.POS_PARTO,
        CategoriaConteudo.SAUDE_MENTAL,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltroCategoria(stringResource(R.string.cat_todos), categoria == null) { aoMudarCategoria(null) }
        categorias.forEach { c ->
            FiltroCategoria(stringResource(c.rotulo), categoria == c) { aoMudarCategoria(c) }
        }
    }
    Spacer(Modifier.height(16.dp))

    val lista = conteudos.lista
    when {
        conteudos.erro -> Text(
            stringResource(R.string.sau_erro),
            style = MaterialTheme.typography.bodyMedium,
            color = NanaCores.TextoSuave,
        )
        lista == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NanaCores.Ameixa)
        }
        else -> {
            val visiveis = lista.filter {
                it.categoria in categorias &&
                    (categoria == null || it.categoria == categoria) &&
                    it.combina(termo)
            }
            if (visiveis.isEmpty()) {
                Text(
                    stringResource(
                        if (termo.isEmpty()) R.string.sau_vazio else R.string.dir_sem_resultado,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanaCores.TextoSuave,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visiveis.forEach { c -> CartaoConteudo(c) { aoAbrirConteudo(c) } }
                }
            }
        }
    }
}

@Composable
private fun FiltroCategoria(texto: String, marcado: Boolean, aoTocar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (marcado) NanaCores.Ameixa else NanaCores.Branco,
        border = BorderStroke(1.5.dp, NanaCores.Ameixa),
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .selectable(selected = marcado, role = Role.RadioButton, onClick = aoTocar),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                color = if (marcado) NanaCores.Branco else NanaCores.Ameixa,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

private fun Conteudo.combina(termo: String): Boolean =
    termo.isEmpty() ||
        titulo.contains(termo, ignoreCase = true) ||
        descricao.contains(termo, ignoreCase = true) ||
        paragrafos.any { it.contains(termo, ignoreCase = true) }

/** Ícone de cada categoria de conteúdo. */
val CategoriaConteudo.icone: ImageVector
    get() = when (this) {
        CategoriaConteudo.SAUDE_NA_GESTACAO -> Icons.Outlined.PregnantWoman
        CategoriaConteudo.POS_PARTO -> Icons.Outlined.ChildCare
        CategoriaConteudo.SAUDE_MENTAL -> Icons.Outlined.SelfImprovement
        CategoriaConteudo.MEUS_DIREITOS -> Icons.Outlined.Balance
    }

@Composable
private fun CartaoConteudo(conteudo: Conteudo, aoTocar: () -> Unit) {
    Surface(
        onClick = aoTocar,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconeArredondado {
                Icon(conteudo.categoria.icone, contentDescription = null, tint = NanaCores.Ameixa)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    conteudo.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = NanaCores.AmeixaEscura,
                )
                Text(
                    conteudo.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.TextoSuave,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NanaCores.Ameixa,
            )
        }
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
