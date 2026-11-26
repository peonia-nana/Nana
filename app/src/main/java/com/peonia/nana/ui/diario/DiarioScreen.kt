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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import com.peonia.nana.data.RegistroDiario
import com.peonia.nana.data.Nuvem
import com.peonia.nana.ui.components.BotaoPrincipal
import com.peonia.nana.ui.components.SeloNana
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

    val registros: StateFlow<Map<LocalDate, RegistroDiario>> = repositorio.registros(Nuvem.uid)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _mesVisivel = MutableStateFlow(YearMonth.now())
    val mesVisivel: StateFlow<YearMonth> = _mesVisivel.asStateFlow()

    private val _diaSelecionado = MutableStateFlow(LocalDate.now())
    val diaSelecionado: StateFlow<LocalDate> = _diaSelecionado.asStateFlow()

    /** true enquanto a mãe edita um texto que já estava salvo. */
    private val _editando = MutableStateFlow(false)
    val editando: StateFlow<Boolean> = _editando.asStateFlow()

    private val _salvando = MutableStateFlow(false)
    val salvando: StateFlow<Boolean> = _salvando.asStateFlow()

    fun mudarMes(passos: Long) {
        _mesVisivel.value = _mesVisivel.value.plusMonths(passos)
    }

    fun selecionarDia(dia: LocalDate) {
        _diaSelecionado.value = dia
        _editando.value = false
    }

    fun comecarEdicao() { _editando.value = true }

    fun cancelarEdicao() { _editando.value = false }

    fun salvarTexto(texto: String, aoConcluir: (Int) -> Unit, aoSalvar: () -> Unit) {
        val dia = _diaSelecionado.value
        val limpo = texto.trim()
        if (limpo.isEmpty() || _salvando.value) return
        if (dia.isAfter(LocalDate.now())) {
            aoConcluir(R.string.dia_futuro)
            return
        }
        val uid = Nuvem.uid ?: return
        _salvando.value = true
        viewModelScope.launch {
            val ok = runCatching { repositorio.salvarTexto(uid, dia, limpo) }.isSuccess
            _salvando.value = false
            if (ok) {
                _editando.value = false
                aoSalvar()
            }
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

    fun apagarTexto(aoConcluir: (Int) -> Unit) {
        val dia = _diaSelecionado.value
        val uid = Nuvem.uid ?: return
        viewModelScope.launch {
            val ok = runCatching { repositorio.apagarTexto(uid, dia) }.isSuccess
            if (ok) _editando.value = false
            aoConcluir(if (ok) R.string.dia_apagado else R.string.sync_erro)
        }
    }
}

/**
 * 06 – Diário.
 *
 * Calendário do mês com uma bolinha nos dias que têm registro e, embaixo,
 * um espaço para a mãe escrever, do jeito dela, como está se sentindo.
 * O texto fica na conta dela, no campo "descricao" do diário de cuidado, e
 * só ela tem acesso (regras do Firestore). Se no mesmo dia ela tocou num
 * sentimento no Início, ele aparece junto do texto.
 */
@Composable
fun DiarioScreen(
    aoAvisar: (Int) -> Unit,
    vm: DiarioViewModel = viewModel(),
) {
    val registros by vm.registros.collectAsStateWithLifecycle()
    val mes by vm.mesVisivel.collectAsStateWithLifecycle()
    val diaSelecionado by vm.diaSelecionado.collectAsStateWithLifecycle()
    val editando by vm.editando.collectAsStateWithLifecycle()
    val salvando by vm.salvando.collectAsStateWithLifecycle()

    val hoje = LocalDate.now()
    val registro = registros[diaSelecionado]
    val textoSalvo = registro?.texto

    // O rascunho é por dia: trocar de dia ou abrir a edição recomeça do texto salvo.
    var rascunho by rememberSaveable(diaSelecionado, editando) {
        mutableStateOf(if (editando) textoSalvo.orEmpty() else "")
    }
    var confirmandoApagar by remember { mutableStateOf(false) }

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
            Column(Modifier.padding(12.dp)) {
                Calendario(
                    mes = mes,
                    hoje = hoje,
                    diaSelecionado = diaSelecionado,
                    registros = registros,
                    aoSelecionar = vm::selecionarDia,
                )
                Spacer(Modifier.height(8.dp))
                Legenda()
            }
        }

        Spacer(Modifier.height(24.dp))

        if (registro != null && registro.texto != null && !editando) {
            Text(
                stringResource(R.string.dia_o_que_escreveu),
                style = MaterialTheme.typography.titleSmall,
                color = NanaCores.AmeixaEscura,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            CartaoRegistro(
                registro = registro,
                aoEditar = vm::comecarEdicao,
                aoApagar = { confirmandoApagar = true },
            )
        } else {
            Text(
                stringResource(R.string.dia_como_se_sente),
                style = MaterialTheme.typography.titleSmall,
                color = NanaCores.AmeixaEscura,
                modifier = Modifier.semantics { heading() },
            )
            registro?.humor?.let {
                Spacer(Modifier.height(8.dp))
                SeloNana(stringResource(R.string.dia_sentiu_se, stringResource(it.rotulo)))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = rascunho,
                onValueChange = { rascunho = it.take(DiarioRepository.LIMITE_TEXTO) },
                placeholder = {
                    Text(stringResource(R.string.dia_placeholder), color = NanaCores.TextoSuave)
                },
                minLines = 5,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NanaCores.Branco,
                    unfocusedContainerColor = NanaCores.Branco,
                    focusedBorderColor = NanaCores.Ameixa,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = NanaCores.Ameixa,
                ),
                supportingText = {
                    Text(
                        "${rascunho.length}/${DiarioRepository.LIMITE_TEXTO}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = NanaCores.TextoSuave,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = NanaCores.TextoSuave,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(R.string.dia_privado),
                    style = MaterialTheme.typography.bodySmall,
                    color = NanaCores.TextoSuave,
                )
            }
            Spacer(Modifier.height(16.dp))
            BotaoPrincipal(
                texto = stringResource(
                    if (editando) R.string.salvar_alteracoes else R.string.dia_salvar,
                ),
                onClick = {
                    if (rascunho.isBlank()) {
                        aoAvisar(R.string.dia_vazio)
                    } else {
                        vm.salvarTexto(rascunho, aoAvisar, aoSalvar = { rascunho = "" })
                    }
                },
            )
            if (editando) {
                TextButton(
                    onClick = vm::cancelarEdicao,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.cancelar), color = NanaCores.Ameixa)
                }
            }
            if (salvando) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NanaCores.Ameixa,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmandoApagar) {
        AlertDialog(
            onDismissRequest = { confirmandoApagar = false },
            title = { Text(stringResource(R.string.dia_apagar_titulo)) },
            text = { Text(stringResource(R.string.dia_apagar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoApagar = false
                    vm.apagarTexto(aoAvisar)
                }) {
                    Text(stringResource(R.string.dia_apagar_confirmar), color = NanaCores.Ameixa)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoApagar = false }) {
                    Text(stringResource(R.string.cancelar), color = NanaCores.TextoSuave)
                }
            },
            containerColor = NanaCores.Branco,
        )
    }
}

private val formatoDia = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR"))
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

@Composable
private fun CartaoRegistro(
    registro: RegistroDiario,
    aoEditar: () -> Unit,
    aoApagar: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NanaCores.Branco,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    registro.data.format(formatoDia),
                    style = MaterialTheme.typography.titleSmall,
                    color = NanaCores.Ameixa,
                    modifier = Modifier.weight(1f),
                )
                registro.atualizadoEm?.let {
                    Text(
                        it.atZone(ZoneId.systemDefault()).format(formatoHora),
                        style = MaterialTheme.typography.labelSmall,
                        color = NanaCores.TextoSuave,
                    )
                }
            }
            registro.humor?.let {
                Spacer(Modifier.height(8.dp))
                SeloNana(stringResource(R.string.dia_sentiu_se, stringResource(it.rotulo)))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                registro.texto.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = NanaCores.Texto,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = aoEditar,
                    shape = RoundedCornerShape(50),
                    color = NanaCores.CartaoDestaque,
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Edit, null, tint = NanaCores.AmeixaEscura, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            stringResource(R.string.dia_editar),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NanaCores.AmeixaEscura,
                        )
                    }
                }
                Surface(
                    onClick = aoApagar,
                    shape = RoundedCornerShape(50),
                    color = NanaCores.Branco,
                    border = BorderStroke(1.5.dp, NanaCores.Fundo),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Delete, null, tint = NanaCores.Ameixa, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            stringResource(R.string.dia_apagar_curto),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NanaCores.Ameixa,
                        )
                    }
                }
            }
        }
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
    registros: Map<LocalDate, RegistroDiario>,
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
                            registro = registros[dia],
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
    registro: RegistroDiario?,
    habilitado: Boolean,
    aoSelecionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descricaoRegistro = when {
        registro == null -> null
        registro.humor != null && registro.texto != null ->
            stringResource(R.string.dia_desc_ambos, stringResource(registro.humor.rotulo))
        registro.humor != null -> stringResource(registro.humor.rotulo)
        else -> stringResource(R.string.dia_tem_registro)
    }
    val descricao = buildString {
        append(dia.dayOfMonth)
        append(" de ")
        append(dia.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")))
        if (ehHoje) append(", hoje")
        if (descricaoRegistro != null) append(", $descricaoRegistro")
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
            // Uma bolinha na cor do sentimento e outra verde se houver texto,
            // como na legenda. Sem registro, o espaço fica vazio.
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                registro?.humor?.let { Bolinha(it.cor) }
                if (registro?.texto != null) Bolinha(CorEscrita)
            }
        }
    }
}

/** Cor da bolinha dos dias em que a mãe escreveu no diário. */
private val CorEscrita = NanaCores.Verde

@Composable
private fun Bolinha(cor: Color, tamanho: Int = 6) {
    Box(
        Modifier
            .size(tamanho.dp)
            .clip(CircleShape)
            .background(cor),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda() {
    val descricao = stringResource(R.string.dia_legenda)
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = descricao },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Humor.entries.forEach { h -> ItemLegenda(h.cor, stringResource(h.rotulo)) }
        ItemLegenda(CorEscrita, stringResource(R.string.dia_legenda_escrita))
    }
}

@Composable
private fun ItemLegenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Bolinha(cor, tamanho = 8)
        Spacer(Modifier.size(5.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = NanaCores.TextoSuave)
    }
}
