package com.peonia.nana.data

/** Campos do cadastro previstos no documento. */
enum class CampoCadastro {
    NOME, IDADE, CIDADE, FASE, INTERESSES, NOME_BEBE, QUANTIDADE_FILHOS, AUTODECLARACAO
}

/**
 * Quais campos do cadastro são obrigatórios.
 *
 * A lista fica concentrada aqui porque o documento "Jornada do Usuário" diz
 * que a obrigatoriedade "ainda deve ser validada" com as mães. Mudar a regra
 * é mexer numa linha só.
 *
 * ATENÇÃO: esta lista precisa combinar com as regras do servidor, em
 * `firestore.rules`. Se um campo for obrigatório aqui e não lá, o app apenas
 * exige a mais; se for obrigatório lá e não aqui, a gravação é recusada e a
 * usuária leva um erro sem entender por quê.
 *
 * Hoje são obrigatórios:
 *   NOME  — decisão da equipe
 *   FASE  — a tela inicial depende dela para escolher os conteúdos
 *   IDADE — decisão da equipe
 */
object RegrasCadastro {
    val camposObrigatorios: Set<CampoCadastro> = setOf(
        CampoCadastro.NOME,
        CampoCadastro.FASE,
        CampoCadastro.IDADE,
    )

    fun eObrigatorio(campo: CampoCadastro): Boolean = campo in camposObrigatorios
}
