package es.collerense.asistencia.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.io.LecturaExcel
import es.collerense.asistencia.io.Sync
import es.collerense.asistencia.io.XlsmReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaSync(repo: Repo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val d = repo.data
    var aviso by remember { mutableStateOf<Pair<String, String>?>(null) }
    var confirmarImport by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<Sync.Exportacion?>(null) }

    fun nombreDe(uri: Uri): String = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "Excel"
    } catch (e: Exception) { "Excel" }

    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            trabajando = true
            scope.launch {
                val res: Result<LecturaExcel> = withContext(Dispatchers.IO) {
                    runCatching {
                        val nombre = nombreDe(uri)
                        val l = ctx.contentResolver.openInputStream(uri)!!.use { XlsmReader.leer(it, nombre) }
                        Sync.aplicarImportacion(repo, l)
                        l
                    }
                }
                trabajando = false
                aviso = res.fold(
                    onSuccess = { l ->
                        "Excel importado" to "${l.data.equipo.equipo} · ${l.data.equipo.temporada}\n" +
                            "${l.data.jugadores.size} jugadores (${l.fotos.size} con foto)\n${l.data.sesiones.size} sesiones\n${l.data.horarios.map { it.nombre }.distinct().size} horarios"
                    },
                    onFailure = { e -> "No se pudo importar" to (e.message ?: e.toString()) }
                )
            }
        }
    }
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val ex = pendiente
        if (uri != null && ex != null) {
            try {
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(ex.contenido) }
                Sync.marcarExportado(repo, ex.id)
                aviso = "Cambios exportados" to "${ex.cambios} cambios guardados en ${ex.nombreArchivo}.\n\nEn el PC: abre el Excel y pulsa IMPORTAR DEL MÓVIL."
            } catch (e: Exception) {
                aviso = "No se pudo guardar" to (e.message ?: e.toString())
            }
        }
        pendiente = null
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Sincronizar con el Excel", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy)
        Espacio()
        Card(colors = CardDefaults.cardColors(containerColor = GrisClaro), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                FilaSimple("Equipo", "${d.equipo.equipo} ${d.equipo.temporada}".ifBlank { "—" })
                FilaSimple("Último Excel importado", d.origen.ifEmpty { "ninguno" })
                FilaSimple("Fecha de importación", d.importado.replace('T', ' ').ifEmpty { "—" })
                FilaSimple("Cambios sin exportar", "${d.pendientes}")
                FilaSimple("Exportaciones hechas", "${d.exportaciones}")
            }
        }
        Espacio(16)
        Seccion("1 · PC → móvil")
        Text("Guarda el Excel en OneDrive o Drive y ábrelo aquí. Se cargan jugadores (con fotos), sesiones, horarios y asistencia.",
            fontSize = 13.sp, color = GrisTexto, modifier = Modifier.padding(vertical = 6.dp))
        Button(onClick = { if (d.pendientes > 0) confirmarImport = true else abrir.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), enabled = !trabajando) {
            Text("Importar Excel (.xlsm)")
        }
        Espacio(16)
        Seccion("2 · Móvil → PC")
        Text("Crea un archivo .colle con lo que has cambiado en el móvil. Guárdalo en OneDrive o Drive y en el Excel pulsa IMPORTAR DEL MÓVIL.",
            fontSize = 13.sp, color = GrisTexto, modifier = Modifier.padding(vertical = 6.dp))
        Button(onClick = {
            val ex = Sync.exportar(repo)
            if (ex.cambios == 0) aviso = "Nada que exportar" to "No hay cambios desde la última importación o exportación."
            else { pendiente = ex; guardar.launch(ex.nombreArchivo) }
        }, modifier = Modifier.fillMaxWidth(), enabled = !trabajando && d.origen.isNotEmpty()) {
            Text("Exportar cambios (${d.pendientes})")
        }
        if (trabajando) { Espacio(); CircularProgressIndicator() }
        Espacio(24)
        Text("Consejo: exporta tus cambios y que el PC los importe antes de volver a importar el Excel en el móvil; " +
            "si no, lo que hayas hecho en el móvil se sustituye por lo del Excel.", fontSize = 12.sp, color = GrisTexto)
        Espacio(16)
        Text("© Oliver Gutiérrez · UD Collerense", fontSize = 11.sp, color = Color(0xFF9CA3AF))
    }

    if (confirmarImport) {
        Confirmar("Cambios sin exportar",
            "Tienes ${d.pendientes} cambios sin exportar. Si importas el Excel ahora se perderán.\n\n¿Importar igualmente?",
            si = "Importar", onSi = { confirmarImport = false; abrir.launch(arrayOf("*/*")) }, onNo = { confirmarImport = false })
    }
    aviso?.let { (t, m) -> Aviso(t, m) { aviso = null } }
}
