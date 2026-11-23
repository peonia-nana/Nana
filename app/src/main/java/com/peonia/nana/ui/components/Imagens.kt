package com.peonia.nana.ui.components

/**
 * Nomes das imagens que a equipe deve colocar em app/src/main/res/drawable/.
 * Formatos aceitos: .png, .jpg ou .webp. Use nomes só com letras minúsculas,
 * números e "_".
 *
 * Enquanto um arquivo não existir, o lugar dele fica vazio, sem quebrar o app.
 */
object Imagens {
    /**
     * Fundo de flores usado em todas as telas.
     * No Figma é o frame "floral fundo 1" (node 88:2), 402 × 874.
     * Exporte em PNG 3x e salve como res/drawable/fundo_flores.png
     */
    const val FUNDO_FLORES = "fundo_flores"

    /** Fundo de flores mais forte, usado na tela de Chat. */
    const val FUNDO_CHAT = "fundo_chat"

    /** Tela 01: ilustração principal das boas-vindas. */
    const val BOAS_VINDAS = "ilustracao_boas_vindas"

    /** Tela 02: ilustração do propósito. */
    const val PROPOSITO = "ilustracao_proposito"

    /** Fotos dos temas na tela de Direitos. */
    const val DIREITO_ACOMPANHANTE = "direito_acompanhante"
    const val DIREITO_DIGNIDADE = "direito_dignidade"
    const val DIREITO_VINCULO = "direito_vinculo"
    const val DIREITO_POS_PARTO = "direito_pos_parto"

    /** Tela 05: ícones do "Como você está hoje?". */
    const val HUMOR_BEM = "humor_bem"
    const val HUMOR_CALMA = "humor_calma"
    const val HUMOR_ANSIOSA = "humor_ansiosa"
    const val HUMOR_CANSADA = "humor_cansada"
    const val HUMOR_TRISTE = "humor_triste"
}
