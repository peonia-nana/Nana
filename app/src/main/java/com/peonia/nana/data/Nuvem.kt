package com.peonia.nana.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Ponto único de acesso ao Firebase.
 *
 * Centralizar aqui evita que cada tela escreva o caminho das coleções na mão,
 * que é como aparecem erros de digitação que só se descobrem em produção.
 * Os nomes seguem o documento "Banco de Dados – Nanã", versão 0.2.
 */
object Nuvem {

    val auth: FirebaseAuth get() = Firebase.auth
    val bd: FirebaseFirestore get() = Firebase.firestore

    /** Identificador da mãe logada, ou null se ninguém entrou. */
    val uid: String? get() = auth.currentUser?.uid

    /**
     * O Firestore guarda um cache no próprio aparelho, então o app continua
     * funcionando sem internet e sincroniza depois. É bom para a usuária e é
     * um dado pessoal a mais no celular: por isso o cache é limpo ao sair da
     * conta, em [AutenticacaoRepository.sair].
     */
    object Colecoes {
        const val MAES = "maes"
        const val PERFIL_ACOLHIMENTO = "perfil_acolhimento"
        const val DIARIO_CUIDADO = "diario_cuidado"
        const val PREFERENCIAS_CONTEUDO = "preferencias_conteudo"
        const val CONSENTIMENTOS = "consentimentos"
        const val CONSENTIMENTOS_HISTORICO = "consentimentos_historico"
        const val AUDITORIA = "auditoria"
        const val CHATS = "chats"
        const val MEMBROS = "membros"
        const val MENSAGENS = "mensagens"
        const val DIGITANDO = "digitando"
    }

    /** Documento fixo do perfil: garante a relação de um perfil por mãe. */
    const val PERFIL_PRINCIPAL = "principal"

    fun mae(uid: String): DocumentReference =
        bd.collection(Colecoes.MAES).document(uid)

    fun perfil(uid: String): DocumentReference =
        mae(uid).collection(Colecoes.PERFIL_ACOLHIMENTO).document(PERFIL_PRINCIPAL)

    fun diario(uid: String) = mae(uid).collection(Colecoes.DIARIO_CUIDADO)

    fun preferencias(uid: String) = mae(uid).collection(Colecoes.PREFERENCIAS_CONTEUDO)

    fun consentimentos(uid: String) = mae(uid).collection(Colecoes.CONSENTIMENTOS)

    fun consentimentosHistorico(uid: String) =
        mae(uid).collection(Colecoes.CONSENTIMENTOS_HISTORICO)

    fun auditoria(uid: String) = mae(uid).collection(Colecoes.AUDITORIA)

    // Chats da aba Apoio. Ficam fora de maes/{uid} porque são de várias
    // mães ao mesmo tempo (relação N:N do documento).
    fun chats() = bd.collection(Colecoes.CHATS)

    fun chat(chatId: String): DocumentReference = chats().document(chatId)

    fun membros(chatId: String) = chat(chatId).collection(Colecoes.MEMBROS)

    fun mensagens(chatId: String) = chat(chatId).collection(Colecoes.MENSAGENS)

    fun digitando(chatId: String) = chat(chatId).collection(Colecoes.DIGITANDO)
}

/** Finalidades de consentimento aceitas pelas regras do servidor. */
enum class FinalidadeConsentimento(val chave: String) {
    RACA_AUTODECLARADA("raca_autodeclarada"),
    DADOS_DE_SAUDE("dados_de_saude"),
}

/** Versão do texto de privacidade aceito. Mude junto com o texto. */
const val VERSAO_POLITICA = "v1"
