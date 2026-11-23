package com.peonia.nana.ui.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.data.Perfil
import com.peonia.nana.ui.components.AvatarPerfil
import com.peonia.nana.ui.components.ChipNana
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.LinhaLista
import com.peonia.nana.ui.components.TituloSecao
import com.peonia.nana.ui.theme.NanaCores

/**
 * 09 – Perfil.
 *
 * Meus dados, Notificações, Privacidade e segurança e Ajuda e contato abrem
 * as telas correspondentes. "Sair da conta" pede confirmação antes de sair.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfilScreen(
    perfil: Perfil,
    aoEditar: () -> Unit,
    aoAbrirNotificacoes: () -> Unit,
    aoAbrirPrivacidade: () -> Unit,
    aoAbrirAjuda: () -> Unit,
    aoAbrirMeusDados: () -> Unit,
    aoSair: () -> Unit,
    aoAvisar: (Int) -> Unit,
) {
    var confirmandoSaida by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.per_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = NanaCores.AmeixaEscura,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = NanaCores.Branco,
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AvatarPerfil(perfil.nome)
                Spacer(Modifier.height(10.dp))
                Text(
                    perfil.nome.ifBlank { stringResource(R.string.per_sem_nome) },
                    style = MaterialTheme.typography.titleLarge,
                    color = NanaCores.AmeixaEscura,
                )
                val resumo = resumoPerfil(perfil)
                if (resumo.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        resumo,
                        style = MaterialTheme.typography.bodySmall,
                        color = NanaCores.TextoSuave,
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = aoEditar,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.5.dp, NanaCores.Ameixa),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(
                        stringResource(R.string.per_editar),
                        color = NanaCores.Ameixa,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (perfil.interesses.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            TituloSecao(stringResource(R.string.per_procura))
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                perfil.interesses.sortedBy { it.ordinal }.forEach {
                    ChipNana(stringResource(it.rotulo), selecionado = true)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LinhaLista(
                titulo = stringResource(R.string.per_meus_dados),
                descricao = null,
                aoTocar = aoAbrirMeusDados,
                icone = { IconeMenu { Icon(Icons.Outlined.Person, null, tint = NanaCores.Ameixa) } },
            )
            LinhaLista(
                titulo = stringResource(R.string.per_notificacoes),
                descricao = null,
                aoTocar = aoAbrirNotificacoes,
                icone = { IconeMenu { Icon(Icons.Outlined.Notifications, null, tint = NanaCores.Ameixa) } },
            )
            LinhaLista(
                titulo = stringResource(R.string.per_privacidade),
                descricao = null,
                aoTocar = aoAbrirPrivacidade,
                icone = { IconeMenu { Icon(Icons.Outlined.Lock, null, tint = NanaCores.Ameixa) } },
            )
            LinhaLista(
                titulo = stringResource(R.string.per_ajuda),
                descricao = null,
                aoTocar = aoAbrirAjuda,
                icone = { IconeMenu { Icon(Icons.Outlined.HelpOutline, null, tint = NanaCores.Ameixa) } },
            )
        }

        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = { confirmandoSaida = true },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = 48.dp),
        ) {
            Text(
                stringResource(R.string.per_sair),
                color = NanaCores.Ameixa,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmandoSaida) {
        DialogoSair(
            aoConfirmar = { confirmandoSaida = false; aoSair() },
            aoCancelar = { confirmandoSaida = false },
        )
    }
}

@Composable
private fun IconeMenu(content: @Composable () -> Unit) {
    IconeArredondado(content)
}

@Composable
private fun DialogoSair(aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(stringResource(R.string.sair_titulo)) },
        text = { Text(stringResource(R.string.sair_texto)) },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text(stringResource(R.string.sair_confirmar), color = NanaCores.Ameixa)
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text(stringResource(R.string.cancelar), color = NanaCores.TextoSuave)
            }
        },
        containerColor = NanaCores.Branco,
    )
}

/** Monta a linha "Gestação · 1º filho · Cidade" com o que a usuária informou. */
@Composable
private fun resumoPerfil(perfil: Perfil): String {
    val partes = buildList {
        perfil.fase?.let { add(stringResource(it.rotulo)) }
        perfil.quantidadeFilhos?.let { add(stringResource(it.rotulo)) }
        if (perfil.cidade.isNotBlank()) add(perfil.cidade)
    }
    return partes.joinToString(" · ")
}
