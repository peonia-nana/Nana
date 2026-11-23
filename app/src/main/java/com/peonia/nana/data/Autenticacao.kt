package com.peonia.nana.data

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** Em que ponto do acesso a usuária está. */
sealed interface EstadoAcesso {
    /** Ainda consultando o Firebase. */
    data object Carregando : EstadoAcesso

    /** Ninguém entrou. */
    data object Deslogada : EstadoAcesso

    /** Entrou, mas ainda não confirmou o e-mail. */
    data class EmailNaoVerificado(val email: String) : EstadoAcesso

    /** Entrou e confirmou o e-mail. */
    data class Dentro(val uid: String, val email: String) : EstadoAcesso
}

/**
 * Erros de acesso em português, para a tela não mostrar mensagem técnica.
 *
 * Importante: as mensagens de "e-mail já usado" e "senha errada" são
 * propositalmente genéricas em alguns casos. Dizer "este e-mail não existe"
 * permitiria descobrir quem tem conta no Nanã testando endereços — o que, num
 * app sobre maternidade e saúde, é informação sensível sobre a pessoa.
 */
enum class ErroAcesso {
    EMAIL_INVALIDO,
    SENHA_FRACA,
    EMAIL_JA_USADO,
    CREDENCIAIS_INVALIDAS,
    SEM_INTERNET,
    DESCONHECIDO,
}

class AutenticacaoRepository {

    /**
     * Acompanha o login em tempo real.
     *
     * Usa o ouvinte de TOKEN, e não o de login: confirmar o e-mail não muda
     * quem está logada, então o ouvinte de login nunca avisaria, e o app
     * ficaria preso na tela "Confirme seu e-mail". O token é renovado em
     * [conferirVerificacao], e aí este ouvinte dispara.
     */
    val estado: Flow<EstadoAcesso> = callbackFlow {
        val ouvinte = com.google.firebase.auth.FirebaseAuth.IdTokenListener { auth ->
            val usuaria = auth.currentUser
            trySend(
                when {
                    usuaria == null -> EstadoAcesso.Deslogada
                    !usuaria.isEmailVerified ->
                        EstadoAcesso.EmailNaoVerificado(usuaria.email.orEmpty())
                    else -> EstadoAcesso.Dentro(usuaria.uid, usuaria.email.orEmpty())
                },
            )
        }
        Nuvem.auth.addIdTokenListener(ouvinte)
        awaitClose { Nuvem.auth.removeIdTokenListener(ouvinte) }
    }.distinctUntilChanged()

    /**
     * Cria a conta e já envia o e-mail de confirmação.
     * As regras do servidor só aceitam gravação com o e-mail verificado.
     */
    suspend fun criarConta(email: String, senha: String): ErroAcesso? = try {
        Nuvem.auth.createUserWithEmailAndPassword(email.trim(), senha).await()
        Nuvem.auth.currentUser?.sendEmailVerification()?.await()
        null
    } catch (e: Exception) {
        traduzir(e)
    }

    suspend fun entrar(email: String, senha: String): ErroAcesso? = try {
        Nuvem.auth.signInWithEmailAndPassword(email.trim(), senha).await()
        null
    } catch (e: Exception) {
        traduzir(e)
    }

    suspend fun reenviarVerificacao(): ErroAcesso? = try {
        Nuvem.auth.currentUser?.sendEmailVerification()?.await()
        null
    } catch (e: Exception) {
        traduzir(e)
    }

    /**
     * Relê a conta no servidor para saber se o e-mail já foi confirmado.
     *
     * Confirmado, renova o token na hora. Isso faz duas coisas:
     *   1. dispara o ouvinte de [estado], e o app sai da tela de confirmação;
     *   2. o novo token leva email_verified = true, que é o que as regras do
     *      Firestore conferem. Com o token antigo, o cadastro seria recusado.
     */
    suspend fun conferirVerificacao(): Boolean = try {
        val usuaria = Nuvem.auth.currentUser
        if (usuaria == null) {
            false
        } else {
            usuaria.reload().await()
            val atual = Nuvem.auth.currentUser
            if (atual != null && atual.isEmailVerified) {
                atual.getIdToken(true).await()
                true
            } else {
                false
            }
        }
    } catch (e: Exception) {
        false
    }

    suspend fun recuperarSenha(email: String): ErroAcesso? = try {
        Nuvem.auth.sendPasswordResetEmail(email.trim()).await()
        null
    } catch (e: Exception) {
        traduzir(e)
    }

    /**
     * Sai da conta e **limpa o cache local do Firestore**.
     *
     * Sem essa limpeza, os dados da mãe continuariam guardados no aparelho
     * depois que ela sai — inclusive se outra pessoa usar o mesmo celular.
     */
    suspend fun sair() {
        Nuvem.auth.signOut()
        runCatching { Nuvem.bd.terminate().await() }
        runCatching { Nuvem.bd.clearPersistence().await() }
    }

    private fun traduzir(e: Exception): ErroAcesso = when (e) {
        is FirebaseAuthWeakPasswordException -> ErroAcesso.SENHA_FRACA
        is FirebaseAuthUserCollisionException -> ErroAcesso.EMAIL_JA_USADO
        is FirebaseAuthInvalidCredentialsException -> ErroAcesso.CREDENCIAIS_INVALIDAS
        is FirebaseAuthInvalidUserException -> ErroAcesso.CREDENCIAIS_INVALIDAS
        is IOException -> ErroAcesso.SEM_INTERNET
        else -> ErroAcesso.DESCONHECIDO
    }
}
