package com.peonia.nana.notificacoes

import android.content.Context

/**
 * O que já foi avisado, guardado só neste aparelho, para não repetir avisos.
 * Não guarda conteúdo de mensagem: só ids e horários.
 */
class EstadoAvisos(context: Context) {

    private val sp = context.getSharedPreferences("avisos_estado", Context.MODE_PRIVATE)

    fun ultimoAvisoChat(chatId: String): Long = sp.getLong("chat_$chatId", 0L)

    fun marcarAvisoChat(chatId: String, millis: Long) {
        sp.edit().putLong("chat_$chatId", millis).apply()
    }

    /** null = ainda não conferiu nenhuma vez (não avisa na primeira). */
    fun conteudosVistos(): Set<String>? =
        if (sp.contains("conteudos")) sp.getStringSet("conteudos", emptySet()).orEmpty() else null

    fun marcarConteudosVistos(ids: Set<String>) {
        sp.edit().putStringSet("conteudos", ids).apply()
    }

    fun ultimaDica(): String? = sp.getString("ultima_dica", null)

    fun marcarDica(data: String) {
        sp.edit().putString("ultima_dica", data).apply()
    }

    fun limpar() {
        sp.edit().clear().apply()
    }
}
