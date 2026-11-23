package com.peonia.nana.ui.apoio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peonia.nana.R
import com.peonia.nana.data.Mensagem
import com.peonia.nana.ui.components.FundoNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.theme.NanaCores
import java.time.LocalDate

/** Cores da conversa, tiradas do protótipo. */
private val CorSeparadorDia = Color(0xFFF9E1F1)
private val CorHoraRecebida = Color(0xFF8E7A8B)
private val CorHoraEnviada = Color(0xFFF1D4EC)

/** O que aparece na lista da conversa, de baixo para cima. */
private sealed interface ItemConversa {
    val chave: String

    data class Dia(val dia: LocalDate) : ItemConversa {
        override val chave = "dia_$dia"
    }

    data class Balao(val mensagem: Mensagem, val minha: Boolean, val primeiraDoGrupo: Boolean) : ItemConversa {
        override val chave = mensagem.id
    }

    data class Digitando(val uid: String) : ItemConversa {
        override val chave = "digitando_$uid"
    }
}

/**
 * Tela de um chat em grupo da aba Apoio.
 *
 * Conversa entre pessoas, sem resposta automática. Segura o dedo numa
 * mensagem (ou toque no coração embaixo dela) para curtir.
 */
@Composable
fun ChatScreen(
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
    vm: ChatViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var texto by rememberSaveable { mutableStateOf("") }
    var confirmandoSaida by remember { mutableStateOf(false) }
    val lista = rememberLazyListState()

    val itens = remember(estado.mensagens, estado.digitando, estado.meuUid) {
        montarItens(estado.mensagens, estado.digitando, estado.meuUid)
    }

    // Chegou mensagem nova com a tela aberta: marca como lida e desce até ela.
    LaunchedEffect(estado.mensagens.lastOrNull()?.id) {
        vm.marcarLidoSePreciso()
        if (lista.firstVisibleItemIndex <= 2) lista.animateScrollToItem(0)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FundoNana(fundo = Imagens.FUNDO_CHAT)

        Column(Modifier.fillMaxSize()) {
            Cabecalho(
                nome = estado.chat?.nome.orEmpty(),
                participantes = estado.chat?.participantes ?: 0,
                podeSair = estado.participacao != null,
                aoVoltar = aoVoltar,
                aoSair = { confirmandoSaida = true },
            )

            when {
                estado.carregando -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                    CircularProgressIndicator(color = NanaCores.Ameixa)
                }

                estado.chat == null || estado.participacao == null -> ForaDoChat(
                    Modifier.weight(1f),
                    aoVoltar,
                )

                else -> {
                    LazyColumn(
                        state = lista,
                        reverseLayout = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (itens.isEmpty()) {
                            item(key = "vazio") { ConversaVazia() }
                        }
                        items(itens, key = { it.chave }) { item ->
                            when (item) {
                                is ItemConversa.Dia -> SeparadorDia(item.dia)
                                is ItemConversa.Digitando -> BalaoDigitando()
                                is ItemConversa.Balao -> LinhaMensagem(
                                    item = item,
                                    nome = estado.nomes[item.mensagem.autora],
                                    meuUid = estado.meuUid,
                                    aoCurtir = { vm.alternarCurtida(item.mensagem) },
                                )
                            }
                        }
                    }

                    BarraDeEnvio(
                        texto = texto,
                        aoMudar = {
                            texto = it.take(ChatViewModel.LIMITE_TEXTO)
                            if (it.isNotBlank()) vm.digitou()
                        },
                        podeEnviar = texto.isNotBlank() && !estado.enviando,
                        aoEnviar = {
                            vm.enviar(
                                texto,
                                aoLimpar = { texto = "" },
                                aoFalhar = { texto = it },
                                aoAvisar = aoAvisar,
                            )
                        },
                        aoAnexar = { aoAvisar(R.string.proxima_etapa_aviso) },
                    )
                }
            }
        }
    }

    if (confirmandoSaida) {
        AlertDialog(
            onDismissRequest = { confirmandoSaida = false },
            title = { Text(stringResource(R.string.chat_sair_titulo)) },
            text = { Text(stringResource(R.string.chat_sair_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoSaida = false
                    vm.sair(aoSair = aoVoltar, aoAvisar = aoAvisar)
                }) {
                    Text(stringResource(R.string.chat_sair), color = NanaCores.Ameixa)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoSaida = false }) {
                    Text(stringResource(R.string.cancelar), color = NanaCores.TextoSuave)
                }
            },
            containerColor = NanaCores.Branco,
        )
    }
}

/**
 * Monta a lista já na ordem de baixo para cima (a lista é invertida):
 * quem está digitando, depois as mensagens da mais nova para a mais antiga,
 * com um separador sempre que muda o dia.
 */
private fun montarItens(
    mensagens: List<Mensagem>,
    digitando: List<String>,
    meuUid: String,
): List<ItemConversa> {
    val cronologica = mutableListOf<ItemConversa>()
    var diaAnterior: LocalDate? = null
    var autoraAnterior: String? = null
    for (m in mensagens) {
        val dia = m.enviadaEm.dia()
        if (dia != diaAnterior) {
            cronologica += ItemConversa.Dia(dia)
            diaAnterior = dia
            autoraAnterior = null
        }
        cronologica += ItemConversa.Balao(
            mensagem = m,
            minha = m.autora == meuUid,
            primeiraDoGrupo = m.autora != autoraAnterior,
        )
        autoraAnterior = m.autora
    }
    return digitando.map { ItemConversa.Digitando(it) } + cronologica.asReversed()
}

@Composable
private fun Cabecalho(
    nome: String,
    participantes: Int,
    podeSair: Boolean,
    aoVoltar: () -> Unit,
    aoSair: () -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    Surface(
        color = NanaCores.Branco,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.voltar),
                    tint = NanaCores.Ameixa,
                )
            }
            Box(
                Modifier
                    .size(45.dp)
                    .background(NanaCores.IconeFundo, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = NanaCores.Ameixa)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nome,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NanaCores.AmeixaEscura,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    pluralStringResource(R.plurals.chat_participantes, participantes, participantes),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.TextoSuave,
                )
            }
            if (podeSair) {
                Box {
                    IconButton(onClick = { menuAberto = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.chat_mais_opcoes),
                            tint = NanaCores.TextoSuave,
                        )
                    }
                    DropdownMenu(
                        expanded = menuAberto,
                        onDismissRequest = { menuAberto = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_sair), color = NanaCores.Texto) },
                            onClick = {
                                menuAberto = false
                                aoSair()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeparadorDia(dia: LocalDate) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Surface(color = CorSeparadorDia, shape = RoundedCornerShape(50)) {
            Text(
                rotuloDoDia(dia),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = NanaCores.AmeixaEscura,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AvatarParticipante() {
    Box(
        Modifier
            .size(29.dp)
            .background(NanaCores.IconeFundo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Person,
            contentDescription = null,
            tint = NanaCores.Ameixa,
            modifier = Modifier.size(16.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaMensagem(
    item: ItemConversa.Balao,
    nome: String?,
    meuUid: String,
    aoCurtir: () -> Unit,
) {
    val m = item.mensagem
    val rotuloCurtir = stringResource(R.string.chat_curtir)
    val balao: @Composable () -> Unit = {
        Surface(
            color = if (item.minha) NanaCores.Ameixa else NanaCores.Branco,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = if (item.minha) 0.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = aoCurtir,
                    onLongClickLabel = rotuloCurtir,
                ),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(
                    m.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.minha) NanaCores.Branco else NanaCores.Texto,
                )
                Text(
                    hora(m.enviadaEm),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.minha) CorHoraEnviada else CorHoraRecebida,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }

    if (item.minha) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            balao()
            if (m.curtidas.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                PilulaCurtidas(m.curtidas.size, meuUid in m.curtidas, aoCurtir)
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        if (item.primeiraDoGrupo) {
            Text(
                nome ?: stringResource(R.string.chat_participante),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NanaCores.Ameixa,
                modifier = Modifier.padding(start = 39.dp, bottom = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.primeiraDoGrupo) AvatarParticipante() else Spacer(Modifier.width(29.dp))
            Spacer(Modifier.width(10.dp))
            balao()
        }
        if (m.curtidas.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Box(Modifier.padding(start = 47.dp)) {
                PilulaCurtidas(m.curtidas.size, meuUid in m.curtidas, aoCurtir)
            }
        }
    }
}

@Composable
private fun PilulaCurtidas(total: Int, minha: Boolean, aoTocar: () -> Unit) {
    val descricao = pluralStringResource(R.plurals.chat_curtidas, total, total)
    Surface(
        onClick = aoTocar,
        color = NanaCores.Branco,
        shape = RoundedCornerShape(50),
        shadowElevation = 1.dp,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (minha) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = descricao,
                tint = NanaCores.Ameixa,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                total.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NanaCores.Texto,
            )
        }
    }
}

@Composable
private fun BalaoDigitando() {
    val descricao = stringResource(R.string.chat_digitando)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        AvatarParticipante()
        Spacer(Modifier.width(10.dp))
        Surface(color = NanaCores.Branco, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
            Text(
                "• • •",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = NanaCores.TextoSuave,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .semantics { this.contentDescription = descricao },
            )
        }
    }
}

@Composable
private fun ConversaVazia() {
    Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
        Surface(color = NanaCores.Branco.copy(alpha = 0.9f), shape = RoundedCornerShape(16.dp)) {
            Text(
                stringResource(R.string.chat_vazio),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.TextoSuave,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ForaDoChat(modifier: Modifier, aoVoltar: () -> Unit) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(color = NanaCores.Branco, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.chat_fora),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NanaCores.Texto,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = aoVoltar) {
                    Text(stringResource(R.string.voltar), color = NanaCores.Ameixa)
                }
            }
        }
    }
}

@Composable
private fun BarraDeEnvio(
    texto: String,
    aoMudar: (String) -> Unit,
    podeEnviar: Boolean,
    aoEnviar: () -> Unit,
    aoAnexar: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = NanaCores.Branco,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 54.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = aoAnexar) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.chat_anexar),
                        tint = NanaCores.TextoSuave,
                    )
                }
                TextField(
                    value = texto,
                    onValueChange = aoMudar,
                    placeholder = {
                        Text(stringResource(R.string.chat_escreva), color = NanaCores.TextoSuave)
                    },
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = NanaCores.Ameixa,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = aoEnviar,
            enabled = podeEnviar,
            shape = CircleShape,
            color = if (podeEnviar) NanaCores.Ameixa else NanaCores.Ameixa.copy(alpha = 0.5f),
            modifier = Modifier.size(50.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_enviar),
                    tint = NanaCores.Branco,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
