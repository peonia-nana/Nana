package com.peonia.nana.ui.direitos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peonia.nana.R
import com.peonia.nana.data.Conteudo
import com.peonia.nana.data.FonteConteudo
import com.peonia.nana.ui.components.CabecalhoVoltar
import com.peonia.nana.ui.components.IconeArredondado
import com.peonia.nana.ui.components.SeloNana
import com.peonia.nana.ui.theme.NanaCores

/** Começo do aviso padrão que o script de importação põe no fim de cada texto. */
private const val INICIO_AVISO = "Este conteúdo é informativo"

/**
 * Um conteúdo educativo aberto, no mesmo estilo das páginas de Direitos:
 * título, card com o texto e o botão Compartilhar, as fontes e o aviso.
 */
@Composable
fun ConteudoScreen(
    conteudo: Conteudo?,
    aoVoltar: () -> Unit,
    aoAvisar: (Int) -> Unit,
) {
    val contexto = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (conteudo == null) {
            CabecalhoVoltar(stringResource(R.string.sau_nao_encontrado), aoVoltar)
            return@Column
        }

        CabecalhoVoltar(conteudo.titulo, aoVoltar)
        Spacer(Modifier.height(20.dp))

        val aviso = conteudo.paragrafos.lastOrNull()?.takeIf { it.startsWith(INICIO_AVISO) }
        val corpo = if (aviso != null) conteudo.paragrafos.dropLast(1) else conteudo.paragrafos

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = NanaCores.Branco,
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconeArredondado {
                        Icon(conteudo.categoria.icone, contentDescription = null, tint = NanaCores.Ameixa)
                    }
                    Spacer(Modifier.width(12.dp))
                    SeloNana(stringResource(conteudo.categoria.rotulo))
                }
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    corpo.forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = NanaCores.Texto)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        onClick = { compartilhar(contexto, conteudo, aoAvisar) },
                        shape = RoundedCornerShape(50),
                        color = NanaCores.CartaoDestaque,
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                tint = NanaCores.AmeixaEscura,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.sau_compartilhar),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = NanaCores.AmeixaEscura,
                            )
                        }
                    }
                }
            }
        }

        if (conteudo.fontes.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NanaCores.Branco,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = NanaCores.Ameixa)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.sau_fontes),
                            style = MaterialTheme.typography.titleSmall,
                            color = NanaCores.AmeixaEscura,
                        )
                    }
                    conteudo.fontes.forEach { fonte ->
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = NanaCores.Fundo, thickness = 1.dp)
                        Spacer(Modifier.height(10.dp))
                        LinhaFonte(fonte) { abrirFonte(contexto, it, aoAvisar) }
                    }
                }
            }
        }

        if (aviso != null) {
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = NanaCores.CartaoRosa,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = NanaCores.Ameixa)
                    Spacer(Modifier.width(10.dp))
                    Text(aviso, style = MaterialTheme.typography.bodySmall, color = NanaCores.AmeixaEscura)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LinhaFonte(fonte: FonteConteudo, aoAbrir: (String) -> Unit) {
    Column {
        if (fonte.tipo.isNotBlank()) {
            Text(
                fonte.tipo,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = NanaCores.Texto,
            )
        }
        Text(fonte.referencia, style = MaterialTheme.typography.bodySmall, color = NanaCores.TextoSuave)
        val url = fonte.url
        if (!url.isNullOrBlank()) {
            TextButton(onClick = { aoAbrir(url) }, modifier = Modifier.heightIn(min = 44.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = NanaCores.Ameixa,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.sau_ver_estudo),
                    color = NanaCores.Ameixa,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

/** Só abre endereços https: o link vem do banco e não deve abrir outro tipo de coisa. */
private fun abrirFonte(contexto: Context, url: String, aoAvisar: (Int) -> Unit) {
    val endereco = Uri.parse(url)
    if (endereco.scheme != "https") {
        aoAvisar(R.string.sau_link_erro)
        return
    }
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, endereco))
    } catch (e: ActivityNotFoundException) {
        aoAvisar(R.string.sau_link_erro)
    }
}

private fun compartilhar(contexto: Context, conteudo: Conteudo, aoAvisar: (Int) -> Unit) {
    val texto = buildString {
        append(conteudo.titulo).append("\n\n")
        append(conteudo.descricao)
        conteudo.fontes.firstOrNull()?.url?.let { append("\n\n").append(contexto.getString(R.string.sau_fonte_prefixo)).append(' ').append(it) }
        append("\n\n").append(contexto.getString(R.string.sau_via_nana))
    }
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    try {
        contexto.startActivity(Intent.createChooser(envio, conteudo.titulo))
    } catch (e: ActivityNotFoundException) {
        aoAvisar(R.string.sau_link_erro)
    }
}
