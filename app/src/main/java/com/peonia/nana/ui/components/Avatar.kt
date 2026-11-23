package com.peonia.nana.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.peonia.nana.R
import com.peonia.nana.data.FotoPerfil
import com.peonia.nana.ui.theme.NanaCores

/**
 * Avatar da usuária: a foto que ela escolheu ou, se não houver, a primeira
 * letra do nome. Recarrega sozinho quando a foto muda.
 */
@Composable
fun AvatarPerfil(
    nome: String,
    tamanho: Int = 72,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val versao by FotoPerfil.versao.collectAsStateWithLifecycle()
    var foto by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(versao) {
        foto = FotoPerfil.carregar(contexto)?.asImageBitmap()
    }

    val descricao = stringResource(R.string.foto_descricao)
    val letra = nome.trim().firstOrNull()?.uppercase() ?: ""

    Box(
        modifier
            .size(tamanho.dp)
            .clip(CircleShape)
            .background(NanaCores.IconeFundo),
        contentAlignment = Alignment.Center,
    ) {
        val atual = foto
        if (atual != null) {
            Image(
                bitmap = atual,
                contentDescription = descricao,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                letra,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = (tamanho / 2.6).sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = NanaCores.Ameixa,
            )
        }
    }
}
