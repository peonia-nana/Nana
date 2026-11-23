package com.peonia.nana.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.ui.theme.NanaCores

/**
 * Mostra uma imagem enviada pela equipe, procurada pelo nome em res/drawable
 * (ex.: "ilustracao_boas_vindas" → res/drawable/ilustracao_boas_vindas.png).
 * Se o arquivo ainda não foi adicionado ao projeto, nada é exibido. Assim o app
 * compila e funciona mesmo antes de as imagens chegarem.
 * Nenhuma imagem ou forma é desenhada pelo código.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun idDaImagem(nome: String): Int {
    val contexto = LocalContext.current
    return remember(nome) {
        contexto.resources.getIdentifier(nome, "drawable", contexto.packageName)
    }
}

/** True quando a equipe já colocou o arquivo em res/drawable. */
@Composable
fun temImagem(nome: String): Boolean = idDaImagem(nome) != 0

@Composable
fun ImagemNana(
    nome: String,
    descricao: String?,
    modifier: Modifier = Modifier,
    escala: ContentScale = ContentScale.Fit,
) {
    val id = idDaImagem(nome)
    if (id != 0) {
        Image(
            painter = painterResource(id),
            contentDescription = descricao,
            modifier = modifier,
            contentScale = escala,
        )
    }
}

/**
 * Fundo de flores que preenche a tela inteira, atrás do conteúdo.
 * Enquanto o arquivo não estiver em res/drawable, fica só a cor de fundo do Nanã.
 */
@Composable
fun FundoNana(modifier: Modifier = Modifier, fundo: String = Imagens.FUNDO_FLORES) {
    ImagemNana(
        nome = fundo,
        descricao = null,
        modifier = modifier.fillMaxSize(),
        escala = ContentScale.Crop,
    )
}

/** Fundo padrão das telas: cor do Nanã + imagem de flores. */
@Composable
fun TelaNana(
    modifier: Modifier = Modifier,
    fundo: String = Imagens.FUNDO_FLORES,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FundoNana(fundo = fundo)
        content()
    }
}

@Composable
fun BotaoPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NanaCores.Ameixa),
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotaoVoltar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.voltar),
            tint = NanaCores.Ameixa,
        )
    }
}

/** Indicador de páginas (pontos), como no protótipo. */
@Composable
fun IndicadorPaginas(total: Int, atual: Int, modifier: Modifier = Modifier) {
    val descricao = stringResource(R.string.indicador_pagina, atual + 1, total)
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = descricao },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(width = if (i == atual) 18.dp else 7.dp, height = 7.dp)
                    .background(
                        if (i == atual) NanaCores.Ameixa else NanaCores.Folha,
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}

/**
 * Chip em formato de pílula. Se [onClick] for nulo, é apenas informativo.
 * [multipla] define a semântica de acessibilidade: caixa de seleção (true) ou opção única (false).
 */
@Composable
fun ChipNana(
    texto: String,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    multipla: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val forma = RoundedCornerShape(50)
    val interacao = when {
        onClick == null -> Modifier
        multipla -> Modifier.toggleable(value = selecionado, role = Role.Checkbox, onValueChange = { onClick() })
        else -> Modifier.selectable(selected = selecionado, role = Role.RadioButton, onClick = onClick)
    }
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(forma)
            .then(interacao),
        shape = forma,
        color = if (selecionado) NanaCores.CartaoDestaque else NanaCores.Branco,
        border = if (selecionado || onClick == null) BorderStroke(1.5.dp, NanaCores.Ameixa) else null,
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selecionado || onClick == null) NanaCores.AmeixaEscura else NanaCores.Texto,
                fontWeight = if (selecionado) FontWeight.Bold else null,
            )
        }
    }
}

/** Título de campo com indicação de opcional e uma frase curta explicando por que é pedido. */
@Composable
fun RotuloCampo(titulo: String, ajuda: String, opcional: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = if (opcional) "$titulo ${stringResource(R.string.opcional)}" else titulo,
            style = MaterialTheme.typography.titleSmall,
            color = NanaCores.Ameixa,
        )
        Spacer(Modifier.padding(top = 2.dp))
        Text(ajuda, style = MaterialTheme.typography.bodySmall, color = NanaCores.TextoSuave)
    }
}

@Composable
fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    acao: ImeAction = ImeAction.Next,
    erro: String? = null,
    transformacao: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = transformacao,
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = acao),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = NanaCores.Branco,
            unfocusedContainerColor = NanaCores.Branco,
            errorContainerColor = NanaCores.Branco,
            unfocusedBorderColor = NanaCores.Branco,
            focusedBorderColor = NanaCores.Ameixa,
        ),
    )
}

/** Cartão informativo (privacidade, limites). */
@Composable
fun CartaoAviso(
    titulo: String?,
    texto: String,
    modifier: Modifier = Modifier,
    icone: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NanaCores.Branco,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            if (icone != null) {
                icone()
                Spacer(Modifier.width(12.dp))
            }
            Column {
                if (titulo != null) {
                    Text(titulo, style = MaterialTheme.typography.titleSmall, color = NanaCores.AmeixaEscura)
                    Spacer(Modifier.padding(top = 4.dp))
                }
                Text(texto, style = MaterialTheme.typography.bodySmall, color = NanaCores.Texto)
            }
        }
    }
}

/** Ícone dentro de um fundo arredondado rosa, usado nos cartões de áreas. */
@Composable
fun IconeArredondado(content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(NanaCores.IconeFundo, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun TextoCentral(texto: AnnotatedString, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = NanaCores.Texto,
    )
}

@Composable
fun TextoCentral(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = NanaCores.Texto,
    )
}

/**
 * Estrutura das telas: conteúdo rolável em cima e rodapé fixo (botão + indicador).
 * Respeita barras do sistema e teclado (safeDrawingPadding).
 */
@Composable
fun LayoutComRodape(
    modifier: Modifier = Modifier,
    rodape: @Composable ColumnScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            content = conteudo,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = rodape,
        )
    }
}

/** Campo de busca com lupa, usado nas telas de Direitos e Apoio. */
@Composable
fun CampoBusca(
    valor: String,
    aoMudar: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = NanaCores.TextoSuave)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = NanaCores.Branco,
            unfocusedContainerColor = NanaCores.Branco,
            unfocusedBorderColor = NanaCores.Branco,
            focusedBorderColor = NanaCores.Ameixa,
        ),
    )
}

/** Título de seção ("Lista de temas", "Seus chats"...). */
@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = NanaCores.AmeixaEscura,
    )
}

/** Selo em pílula com a referência da lei ou a observação do card. */
@Composable
fun SeloNana(texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = NanaCores.CartaoDestaque,
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = NanaCores.AmeixaEscura,
        )
    }
}

/** Linha de lista com ícone à esquerda, textos e seta à direita. */
@Composable
fun LinhaLista(
    titulo: String,
    descricao: String?,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    icone: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = aoTocar,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icone != null) {
                icone()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = NanaCores.AmeixaEscura,
                )
                if (descricao != null) {
                    Text(
                        descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.TextoSuave,
                    )
                }
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

/** Cabeçalho com botão de voltar e título, usado nas telas de detalhe. */
@Composable
fun CabecalhoVoltar(titulo: String, aoVoltar: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        BotaoVoltar(aoVoltar, Modifier.padding(start = 0.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = NanaCores.AmeixaEscura,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * Monta um texto com algumas palavras em destaque, na cor verde do Nanã.
 *
 * As palavras vêm de um `string-array` em strings.xml, então a equipe muda o
 * destaque sem mexer em código. A busca ignora maiúsculas e respeita limites
 * de palavra, para "apoio" não pintar o "apoiar" de outra frase.
 */
@Composable
fun textoComDestaques(
    texto: String,
    destaques: List<String>,
    cor: Color = NanaCores.Verde,
): AnnotatedString = buildAnnotatedString {
    val marcados = BooleanArray(texto.length)
    destaques.filter { it.isNotBlank() }.forEach { palavra ->
        Regex("\\b${Regex.escape(palavra)}\\b", RegexOption.IGNORE_CASE)
            .findAll(texto)
            .forEach { achado ->
                for (i in achado.range) marcados[i] = true
            }
    }
    var i = 0
    while (i < texto.length) {
        val destacado = marcados[i]
        var fim = i
        while (fim < texto.length && marcados[fim] == destacado) fim++
        val trecho = texto.substring(i, fim)
        if (destacado) withStyle(SpanStyle(color = cor)) { append(trecho) } else append(trecho)
        i = fim
    }
}
