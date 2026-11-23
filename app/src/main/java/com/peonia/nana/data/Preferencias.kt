package com.peonia.nana.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Escolhas da usuária nas telas de Notificações e de Privacidade.
 *
 * Os valores padrão repetem o que o protótipo mostra desenhado.
 */
data class PreferenciasApp(
    // Notificações
    val lembreteDiario: Boolean = true,
    val novidadesDireitos: Boolean = true,
    val respostasApoio: Boolean = true,
    val dicasBemEstar: Boolean = false,
    val horarioLembrete: String = "20:00",
    val naoIncomodar: Boolean = true,
    // Privacidade
    val duasEtapas: Boolean = true,
    val bloqueioApp: Boolean = false,
    val nomeNosChats: Boolean = false,
)

private val Context.preferenciasStore: DataStore<Preferences> by
    preferencesDataStore(name = "preferencias_nana")

class PreferenciasRepository(private val context: Context) {

    private object Chaves {
        val LEMBRETE = booleanPreferencesKey("lembrete_diario")
        val DIREITOS = booleanPreferencesKey("novidades_direitos")
        val APOIO = booleanPreferencesKey("respostas_apoio")
        val BEM_ESTAR = booleanPreferencesKey("dicas_bem_estar")
        val HORARIO = stringPreferencesKey("horario_lembrete")
        val NAO_INCOMODAR = booleanPreferencesKey("nao_incomodar")
        val DUAS_ETAPAS = booleanPreferencesKey("duas_etapas")
        val BLOQUEIO = booleanPreferencesKey("bloqueio_app")
        val NOME_CHATS = booleanPreferencesKey("nome_nos_chats")
    }

    val preferencias: Flow<PreferenciasApp> = context.preferenciasStore.data.map { p ->
        val padrao = PreferenciasApp()
        PreferenciasApp(
            lembreteDiario = p[Chaves.LEMBRETE] ?: padrao.lembreteDiario,
            novidadesDireitos = p[Chaves.DIREITOS] ?: padrao.novidadesDireitos,
            respostasApoio = p[Chaves.APOIO] ?: padrao.respostasApoio,
            dicasBemEstar = p[Chaves.BEM_ESTAR] ?: padrao.dicasBemEstar,
            horarioLembrete = p[Chaves.HORARIO] ?: padrao.horarioLembrete,
            naoIncomodar = p[Chaves.NAO_INCOMODAR] ?: padrao.naoIncomodar,
            duasEtapas = p[Chaves.DUAS_ETAPAS] ?: padrao.duasEtapas,
            bloqueioApp = p[Chaves.BLOQUEIO] ?: padrao.bloqueioApp,
            nomeNosChats = p[Chaves.NOME_CHATS] ?: padrao.nomeNosChats,
        )
    }

    suspend fun salvar(p: PreferenciasApp) {
        context.preferenciasStore.edit { d ->
            d[Chaves.LEMBRETE] = p.lembreteDiario
            d[Chaves.DIREITOS] = p.novidadesDireitos
            d[Chaves.APOIO] = p.respostasApoio
            d[Chaves.BEM_ESTAR] = p.dicasBemEstar
            d[Chaves.HORARIO] = p.horarioLembrete
            d[Chaves.NAO_INCOMODAR] = p.naoIncomodar
            d[Chaves.DUAS_ETAPAS] = p.duasEtapas
            d[Chaves.BLOQUEIO] = p.bloqueioApp
            d[Chaves.NOME_CHATS] = p.nomeNosChats
        }
    }

    /** Usado pela exclusão da conta. */
    suspend fun apagarTudo() {
        context.preferenciasStore.edit { it.clear() }
    }
}
