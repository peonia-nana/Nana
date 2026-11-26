package com.peonia.nana.notificacoes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.peonia.nana.MainActivity
import com.peonia.nana.R

/**
 * Os avisos do Nanã: canais e exibição.
 *
 * Cada tipo de aviso tem um canal próprio, para a usuária poder desligar só
 * um deles também nas configurações do Android.
 *
 * Privacidade: o aviso nunca mostra o texto de uma mensagem do chat nem do
 * diário. Na tela bloqueada aparece só "Você tem novidades no Nanã", para
 * ninguém ao lado ler do que se trata.
 */
enum class Canal(val id: String, @StringRes val nome: Int, @StringRes val descricao: Int) {
    DIARIO("diario", R.string.canal_diario, R.string.not_diario_desc),
    APOIO("apoio", R.string.canal_apoio, R.string.not_apoio_chats_desc),
    CONTEUDOS("conteudos", R.string.canal_conteudos, R.string.not_direitos_desc),
    BEM_ESTAR("bem_estar", R.string.canal_bem_estar, R.string.not_bemestar_desc),
}

object Avisos {

    fun criarCanais(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gerenciador = context.getSystemService(NotificationManager::class.java) ?: return
        Canal.entries.forEach { c ->
            val canal = NotificationChannel(
                c.id,
                context.getString(c.nome),
                if (c == Canal.APOIO) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(c.descricao) }
            gerenciador.createNotificationChannel(canal)
        }
    }

    fun podeAvisar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun mostrar(context: Context, canal: Canal, idAviso: Int, titulo: String, texto: String) {
        if (!podeAvisar(context)) return
        criarCanais(context)

        val abrirApp = PendingIntent.getActivity(
            context,
            idAviso,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Versão que aparece com a tela bloqueada: sem detalhes.
        val publica = NotificationCompat.Builder(context, canal.id)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.aviso_publico))
            .build()

        val aviso = NotificationCompat.Builder(context, canal.id)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(ContextCompat.getColor(context, R.color.nana_ameixa))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publica)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(idAviso, aviso)
        } catch (e: SecurityException) {
            // A permissão foi retirada entre a conferência e o aviso.
        }
    }

    fun limparTudo(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
