package com.peonia.nana.ui.apoio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.peonia.nana.data.Chat
import com.peonia.nana.ui.components.CampoBusca
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores

/** Cor do contador de mensagens não lidas, tirada do protótipo. */
internal val CorNaoLidas = Color(0xFFE0A03A)

/**
 * 08 – Apoio: seus chats e chats para descobrir.
 *
 * Os chats vêm do Firestore e são criados pela equipe
 * (nana-api/admin/criar-chat.js). Tocar num chat abre a conversa;
 * "Entrar" coloca a mãe no chat e já abre a conversa.
 */
@Composable
fun ApoioScreen(
    aoAbrirChat: (String) -> Unit,
    aoAvisar: (Int) -> Unit,
    vm: ApoioViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var busca by rememberSaveable { mutableStateOf("") }

    val termo = busca.trim()
    val seus = estado.seus.filter { termo.isEmpty() || it.chat.nome.contains(termo, ignoreCase = true) }
    val descobrir = estado.descobrir.filter { termo.isEmpty() || it.nome.contains(termo, ignoreCase = true) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.apo_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = NanaCores.AmeixaEscura,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.apo_apoio),
            style = MaterialTheme.typography.bodySmall,
            color = NanaCores.TextoSuave,
        )
        Spacer(Modifier.height(16.dp))
        CampoBusca(busca, { busca = it }, stringResource(R.string.apo_busca))
        Spacer(Modifier.height(20.dp))

        when {
            estado.carregando -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                CircularProgressIndicator(color = NanaCores.Ameixa)
            }

            estado.seus.isEmpty() && estado.descobrir.isEmpty() -> Text(
                stringResource(R.string.apo_nenhum_chat),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.TextoSuave,
            )

            seus.isEmpty() && descobrir.isEmpty() -> Text(
                stringResource(R.string.apo_sem_resultado),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.TextoSuave,
            )

            else -> {
                if (seus.isNotEmpty()) {
                    TituloSecao(stringResource(R.string.apo_seus_chats))
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        seus.forEach { r -> LinhaSeuChat(r) { aoAbrirChat(r.chat.id) } }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                if (descobrir.isNotEmpty()) {
                    TituloSecao(stringResource(R.string.apo_descobrir))
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        descobrir.forEach { chat ->
                            LinhaDescobrirChat(chat) { vm.entrar(chat, aoAbrirChat, aoAvisar) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LinhaSeuChat(resumo: ResumoChat, aoTocar: () -> Unit) {
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
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = NanaCores.Ameixa)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    resumo.chat.nome,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NanaCores.AmeixaEscura,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    resumo.ultima?.texto ?: stringResource(R.string.apo_sem_mensagens),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.TextoSuave,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                resumo.ultima?.let {
                    Text(
                        horaCurta(it.enviadaEm),
                        style = MaterialTheme.typography.labelSmall,
                        color = NanaCores.TextoSuave,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (resumo.naoLidas > 0) {
                        val descricao = pluralStringResource(
                            R.plurals.apo_nao_lidas, resumo.naoLidas, resumo.naoLidas,
                        )
                        Box(
                            Modifier
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .background(CorNaoLidas, CircleShape)
                                .padding(horizontal = 5.dp)
                                .semantics { contentDescription = descricao },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (resumo.naoLidas > 99) "99+" else resumo.naoLidas.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NanaCores.Texto,
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = NanaCores.TextoSuave,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDescobrirChat(chat: Chat, aoEntrar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NanaCores.Branco,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconeArredondado {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = NanaCores.Ameixa)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    chat.nome,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NanaCores.AmeixaEscura,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    pluralStringResource(R.plurals.chat_participantes, chat.participantes, chat.participantes),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.TextoSuave,
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = aoEntrar,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NanaCores.Ameixa),
                border = BorderStroke(1.5.dp, NanaCores.Ameixa),
            ) {
                Text(
                    stringResource(R.string.apo_entrar),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
