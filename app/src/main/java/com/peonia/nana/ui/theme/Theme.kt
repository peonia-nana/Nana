package com.peonia.nana.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta baseada no protótipo do Nanã: tons de rosa da natureza e ameixa.
object NanaCores {
    val Fundo = Color(0xFFF3E4E6)
    val Ameixa = Color(0xFF8A3A84)
    val AmeixaEscura = Color(0xFF5E2059)
    val Texto = Color(0xFF3A1F38)
    val TextoSuave = Color(0xFF5C4459)
    val CartaoRosa = Color(0xFFF8E1EA)
    val CartaoDestaque = Color(0xFFF2D3E1)
    val IconeFundo = Color(0xFFF6C1D6)
    val Folha = Color(0xFFE9B3C2)
    val FolhaEscura = Color(0xFFD892A6)
    val Branco = Color(0xFFFFFFFF)
    val Erro = Color(0xFFB3261E)

    /** Verde de destaque, usado em palavras-chave dos textos de abertura. */
    val Verde = Color(0xFF698910)
}

/** Cores dos sentimentos, usadas nas bolinhas do calendário e na legenda do diário. */
object HumorCores {
    val Bem = Color(0xFFF2A3C0)
    val Calma = Color(0xFFB9A6E0)
    val Ansiosa = Color(0xFFF0B46A)
    val Cansada = Color(0xFF7FB2E5)
    val Triste = Color(0xFF7B4FA8)
}

private val esquemaNana = lightColorScheme(
    primary = NanaCores.Ameixa,
    onPrimary = NanaCores.Branco,
    primaryContainer = NanaCores.CartaoDestaque,
    onPrimaryContainer = NanaCores.AmeixaEscura,
    secondary = NanaCores.FolhaEscura,
    onSecondary = NanaCores.Texto,
    secondaryContainer = NanaCores.CartaoRosa,
    onSecondaryContainer = NanaCores.Texto,
    background = NanaCores.Fundo,
    onBackground = NanaCores.Texto,
    surface = NanaCores.Branco,
    onSurface = NanaCores.Texto,
    surfaceVariant = NanaCores.CartaoRosa,
    onSurfaceVariant = NanaCores.TextoSuave,
    outline = NanaCores.Ameixa,
    error = NanaCores.Erro,
)

private val base = Typography()

private val tipografiaNana = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, color = NanaCores.Ameixa),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
)

@Composable
fun NanaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = esquemaNana,
        typography = tipografiaNana,
        content = content,
    )
}
