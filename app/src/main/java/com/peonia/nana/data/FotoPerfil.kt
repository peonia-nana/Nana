package com.peonia.nana.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Foto de perfil.
 *
 * Fica em `filesDir`, a área privada do aplicativo: nenhum outro app do
 * aparelho alcança esse arquivo, e ele não aparece na galeria. A foto
 * **não sai do celular** — não há envio para servidor nenhum.
 *
 * A escolha da imagem usa o seletor de fotos do próprio Android, que não
 * exige permissão de acesso às fotos: a usuária escolhe uma imagem e só
 * aquela imagem chega ao app.
 *
 * A imagem é reduzida antes de ser guardada. Uma foto de celular tem vários
 * megabytes, e guardar isso inteiro para mostrar num círculo de 80dp
 * desperdiça espaço e memória.
 */
object FotoPerfil {

    private const val ARQUIVO = "foto_perfil.jpg"
    private const val LADO_MAXIMO = 512
    private const val QUALIDADE = 85

    /** Muda a cada gravação ou remoção, para a tela se redesenhar. */
    private val _versao = MutableStateFlow(0L)
    val versao: StateFlow<Long> = _versao.asStateFlow()

    fun arquivo(context: Context): File = File(context.filesDir, ARQUIVO)

    fun existe(context: Context): Boolean = arquivo(context).exists()

    /**
     * Copia a imagem escolhida para dentro do app, reduzida.
     * Retorna false se o arquivo não puder ser lido como imagem.
     */
    suspend fun salvar(context: Context, origem: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val reduzida = lerReduzida(context, origem) ?: return@withContext false
            arquivo(context).outputStream().use { saida ->
                reduzida.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, saida)
            }
            reduzida.recycle()
            _versao.value = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun remover(context: Context) = withContext(Dispatchers.IO) {
        arquivo(context).delete()
        _versao.value = System.currentTimeMillis()
    }

    /** Carrega a foto guardada. Retorna null se não houver. */
    suspend fun carregar(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        val f = arquivo(context)
        if (!f.exists()) return@withContext null
        try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Lê a imagem já reduzida, sem carregar o arquivo inteiro na memória.
     * Primeiro mede, depois decodifica pulando pixels.
     */
    private fun lerReduzida(context: Context, origem: Uri): Bitmap? {
        val medida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(origem)?.use {
            BitmapFactory.decodeStream(it, null, medida)
        } ?: return null

        if (medida.outWidth <= 0 || medida.outHeight <= 0) return null

        var amostra = 1
        while (
            medida.outWidth / (amostra * 2) >= LADO_MAXIMO &&
            medida.outHeight / (amostra * 2) >= LADO_MAXIMO
        ) {
            amostra *= 2
        }

        val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
        return context.contentResolver.openInputStream(origem)?.use {
            BitmapFactory.decodeStream(it, null, opcoes)
        }
    }
}
