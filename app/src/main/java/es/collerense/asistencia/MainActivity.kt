@file:OptIn(ExperimentalMaterial3Api::class)

package es.collerense.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.ui.DialogoNuevaSesion
import es.collerense.asistencia.ui.Navy
import es.collerense.asistencia.ui.PantallaFicha
import es.collerense.asistencia.ui.PantallaJugadores
import es.collerense.asistencia.ui.PantallaPasarLista
import es.collerense.asistencia.ui.PantallaResumen
import es.collerense.asistencia.ui.PantallaSesiones
import es.collerense.asistencia.ui.PantallaSync
import es.collerense.asistencia.ui.Tema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repo(applicationContext)
        setContent { Tema { App(repo) } }
    }
}

@Composable
fun App(repo: Repo) {
    var tab by rememberSaveable { mutableIntStateOf(if (repo.data.origen.isEmpty()) 4 else 0) }
    var ficha by rememberSaveable { mutableStateOf<Int?>(null) }
    var sesionSel by rememberSaveable { mutableStateOf<String?>(null) }
    var nuevaSesion by remember { mutableStateOf(false) }
    val titulos = listOf("Pasar lista", "Sesiones", "Jugadores", "Resumen", "Sincronizar")
    val iconos = listOf(Icons.Filled.Check, Icons.Filled.DateRange, Icons.Filled.Person, Icons.Filled.Star, Icons.Filled.Refresh)
    val d = repo.data

    BackHandler(enabled = ficha != null) { ficha = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (ficha != null) "Ficha" else titulos[tab], fontWeight = FontWeight.Bold)
                        if (d.equipo.equipo.isNotEmpty()) Text("${d.equipo.club} · ${d.equipo.equipo}", fontSize = 12.sp, color = Color(0xFFDDE3F2))
                    }
                },
                navigationIcon = {
                    if (ficha != null) IconButton(onClick = { ficha = null }) { Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar {
                titulos.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i && ficha == null,
                        onClick = { tab = i; ficha = null },
                        icon = { Icon(iconos[i], t) },
                        label = { Text(t, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val f = ficha
            if (f != null) {
                PantallaFicha(repo, f, onVolver = { ficha = null })
            } else when (tab) {
                0 -> PantallaPasarLista(repo, sesionSel, onSel = { sesionSel = it }, onNuevaSesion = { nuevaSesion = true })
                1 -> PantallaSesiones(repo, onAbrir = { sesionSel = it; tab = 0 }, onNuevaSesion = { nuevaSesion = true })
                2 -> PantallaJugadores(repo, onFicha = { ficha = it })
                3 -> PantallaResumen(repo, onFicha = { ficha = it })
                else -> PantallaSync(repo)
            }
        }
    }
    if (nuevaSesion) {
        DialogoNuevaSesion(repo, onCreada = { uid -> sesionSel = uid; nuevaSesion = false; tab = 0 }, onCerrar = { nuevaSesion = false })
    }
}
