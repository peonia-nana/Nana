package com.peonia.nana.ui.diario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peonia.nana.R
import com.peonia.nana.data.DiarioRepository
import com.peonia.nana.data.Humor
import com.peonia.nana.data.Nuvem
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.ImagemNana
import com.peonia.nana.ui.components.Imagens
import com.peonia.nana.ui.components.temImagem
import com.peonia.nana.ui.theme.HumorCores
import com.peonia.nana.ui.theme.NanaCores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Cor da bolinha de cada sentimento. */
val Humor.cor: Color
    get() = when (this) {
        Humor.BEM -> HumorCores.Bem
        Humor.CALMA -> HumorCores.Calma
        Humor.ANSIOSA -> HumorCores.Ansiosa
        Humor.CANSADA -> HumorCores.Cansada
        Humor.TRISTE -> HumorCores.Triste
    }

/** Nome do arquivo de imagem que a equipe envia para cada sentimento. */
val Humor.imagem: String
    get() = when (this) {
        Humor.BEM -> Imagens.HUMOR_BEM
        Humor.CALMA -> Imagens.HUMOR_CALMA
        Humor.ANSIOSA -> Imagens.HUMOR_ANSIOSA
        Humor.CANSADA -> Imagens.HUMOR_CANSADA
        Humor.TRISTE -> Imagens.HUMOR_TRISTE
    }

class DiarioViewModel : ViewModel() {

    private val repositorio = DiarioRepository()

    val registros: StateFlow<Map<LocalDate, Humor>> = repositorio.registros(Nuvem.uid)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _mesVisivel = MutableStateFlow(YearMonth.now())
    val mesVisivel: StateFlow<YearMonth> = _mesVisivel.asStateFlow()

    private val _diaSelecionado = MutableStateFlow(LocalDate.now())
    val diaSelecionado: StateFlow<LocalDate> = _diaSelecionado.asStateFlow()

    private val _humorEscolhido = MutableStateFlow<Humor?>(null)
    val humorEscolhido: StateFlow<Humor?> = _humorEscolhido.asStateFlow()

    fun mudarMes(passos: Long) {
        _mesVisivel.value = _mesVisivel.value.plusMonths(passos)
    }

    fun selecionarDia(dia: LocalDate) {
        _diaSelecionado.value = dia
        // Ao trocar de dia, mostra o que já estava registrado nele.
        _humorEscolhido.value = registros.value[dia]
    }

    fun escolherHumor(humor: Humor) {
        _humorEscolhido.value = if (_humorEscolhido.value == humor) null else humor
    }

    fun registrar(aoConcluir: (Int) -> Unit) {
        val dia = _diaSelecionado.value
        val humor = _humorEscolhido.value ?: return
        if (dia.isAfter(LocalDate.now())) {
            aoConcluir(R.string.dia_futuro)
            return
        }
        val uid = Nuvem.uid ?: return
        viewModelScope.launch {
            val ok = runCatching { repositorio.registrar(uid, dia, humor) }.isSuccess
            aoConcluir(if (ok) R.string.dia_salvo else R.string.sync_erro)
        }
    }

    /**
     * Grava direto, sem passar pela seleção. Usado pelo check-in da tela
     * inicial, onde tocar num sentimento já significa registrar.
     */
    fun registrarHoje(humor: Humor, aoConcluir: (Int) -> Unit) {
        val uid = Nuvem.uid ?: return
        viewModelScope.launch {
            val ok = runCatching { repositorio.registrar(uid, LocalDate.now(), humor) }.isSuccess
            aoConcluir(if (ok) R.string.dia_salvo else R.string.sync_erro)
        }
    }

    fun apagarDoDia(aoConcluir: (Int) -> Unit) {
        val dia = _diaSelecionado.value
        val uid = Nuvem.uid ?: return
        viewModelScope.launch {
            val ok = runCatching { repositorio.apagar(uid, dia) }.isSuccess
            if (ok) _humorEscolhido.value = null
            aoConcluir(if (ok) R.string.dia_apagado else R.string.sync_erro)
        }
    }
}

/**
 * 06 – Diário.
 *
 * Calendário do mês com uma bolinha colorida nos dias que já têm registro,
 * e o seletor de sentimento embaixo. Toda a informação fica neste aparelho.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiarioScreen(
    aoAvisar: (Int) -> Unit,
    vm: DiarioViewModel = viewModel(),
) {
    val registros by vm.registros.collectAsStateWithLifecycle()
    val mes by vm.mesVisivel.collectAsStateWithLifecycle()
    val diaSelecionado by vm.diaSelecionado.collectAsStateWithLifecycle()
    val humorEscolhido by vm.humorEscolhido.collectAsStateWithLifecycle()

    val hoje = LocalDate.now()
    val jaTemRegistro = registros[diaSelecionado] != null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.dia_titulo),
            style = MaterialTheme.typography.headlineSmall,
            color = NanaCores.AmeixaEscura,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(16.dp))

        BarraDoMes(
            mes = mes,
            aoVoltar = { vm.mudarMes(-1) },
            aoAvancar = { vm.mudarMes(1) },
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = NanaCores.Branco,
        ) {
            Calendario(
                mes = mes,
                hoje = hoje,
                diaSelecionado = diaSelecionado,
                registros = registros,
                aoSelecionar = vm::selecionarDia,
                modifier = Modifier.padding(12.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Legenda()

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.dia_como_agora),
            style = MaterialTheme.typography.titleSmall,
            color = NanaCores.Ameixa,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Humor.entries.forEach { h ->
                OpcaoHumor(
                    humor = h,
                    selecionado = humorEscolhido == h,
                    aoSelecionar = { vm.escolherHumor(h) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        BotaoPrincipal(
            texto = stringResource(
                if (jaTemRegistro) R.string.dia_atualizar else R.string.dia_registrar
            ),
            onClick = { vm.registrar(aoAvisar) },
        )
        if (jaTemRegistro) {
            TextButton(
                onClick = { vm.apagarDoDia(aoAvisar) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(R.string.dia_apagar),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.Ameixa,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BarraDoMes(mes: YearMonth, aoVoltar: () -> Unit, aoAvancar: () -> Unit) {
    val nome = mes.month
        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$nome ${mes.year}",
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleSmall,
            color = NanaCores.Ameixa,
        )
        IconButton(onClick = aoVoltar) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.dia_mes_anterior),
                tint = NanaCores.Ameixa,
            )
        }
        IconButton(onClick = aoAvancar) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.dia_proximo_mes),
                tint = NanaCores.Ameixa,
            )
        }
    }
}

@Composable
private fun Calendario(
    mes: YearMonth,
    hoje: LocalDate,
    diaSelecionado: LocalDate,
    registros: Map<LocalDate, Humor>,
    aoSelecionar: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cabecalhos = listOf(
        R.string.dia_dom, R.string.dia_seg, R.string.dia_ter, R.string.dia_qua,
        R.string.dia_qui, R.string.dia_sex, R.string.dia_sab,
    )
    val primeiroDia = mes.atDay(1)
    // A grade começa no domingo. DayOfWeek vai de 1 (segunda) a 7 (domingo).
    val vazios = primeiroDia.dayOfWeek.value % 7
    val totalDias = mes.lengthOfMonth()
    val celulas = vazios + totalDias
    val linhas = (celulas + 6) / 7

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            cabecalhos.forEach { rotulo ->
                Text(
                    stringResource(rotulo),
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = NanaCores.TextoSuave,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (linha in 0 until linhas) {
            Row(Modifier.fillMaxWidth()) {
                for (coluna in 0 until 7) {
                    val indice = linha * 7 + coluna
                    val numero = indice - vazios + 1
                    if (numero in 1..totalDias) {
                        val dia = mes.atDay(numero)
                        CelulaDia(
                            dia = dia,
                            selecionado = dia == diaSelecionado,
                            ehHoje = dia == hoje,
                            humor = registros[dia],
                            habilitado = !dia.isAfter(hoje),
                            aoSelecionar = { aoSelecionar(dia) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CelulaDia(
    dia: LocalDate,
    selecionado: Boolean,
    ehHoje: Boolean,
    humor: Humor?,
    habilitado: Boolean,
    aoSelecionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descricaoHumor = humor?.let { stringResource(it.rotulo) }
    val descricao = buildString {
        append(dia.dayOfMonth)
        append(" de ")
        append(dia.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")))
        if (ehHoje) append(", hoje")
        if (descricaoHumor != null) append(", $descricaoHumor")
    }

    Box(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = descricao }
            .then(
                if (habilitado) {
                    Modifier.selectable(
                        selected = selecionado,
                        role = Role.RadioButton,
                        onClick = aoSelecionar,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (selecionado) NanaCores.Ameixa else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    dia.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selecionado || ehHoje) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        selecionado -> NanaCores.Branco
                        !habilitado -> NanaCores.TextoSuave.copy(alpha = 0.4f)
                        else -> NanaCores.Texto
                    },
                )
            }
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(humor?.cor ?: Color.Transparent),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda() {
    val descricao = stringResource(R.string.dia_legenda)
    FlowRow(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = descricao },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Humor.entries.forEach { h ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(h.cor),
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    stringResource(h.rotulo),
                    style = MaterialTheme.typography.labelSmall,
                    color = NanaCores.TextoSuave,
                )
            }
        }
    }
}

@Composable
private fun OpcaoHumor(
    humor: Humor,
    selecionado: Boolean,
    aoSelecionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(14.dp)
    Surface(
        shape = forma,
        color = if (selecionado) NanaCores.CartaoDestaque else NanaCores.Branco,
        border = if (selecionado) BorderStroke(1.5.dp, NanaCores.Ameixa) else null,
        modifier = modifier
            .clip(forma)
            .selectable(selected = selecionado, role = Role.RadioButton, onClick = aoSelecionar),
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NanaCores.IconeFundo),
                contentAlignment = Alignment.Center,
            ) {
                // Imagem enviada pela equipe. Enquanto o arquivo não existir,
                // fica a bolinha da cor do sentimento, igual à da legenda.
                if (temImagem(humor.imagem)) {
                    ImagemNana(humor.imagem, descricao = null, modifier = Modifier.size(36.dp))
                } else {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(humor.cor),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(humor.rotulo),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = NanaCores.Texto,
            )
        }
    }
}
