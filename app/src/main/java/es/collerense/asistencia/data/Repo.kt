package es.collerense.asistencia.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

class Repo(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val archivo = File(context.filesDir, "datos.json")
    val dirFotos = File(context.filesDir, "fotos").apply { mkdirs() }

    var data by mutableStateOf(cargar())
        private set

    private fun cargar(): AppData = try {
        if (archivo.exists()) json.decodeFromString(AppData.serializer(), archivo.readText()) else AppData()
    } catch (e: Exception) {
        AppData()
    }

    fun set(nuevo: AppData) {
        data = nuevo
        val tmp = File(context.filesDir, "datos.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), nuevo))
        tmp.renameTo(archivo)
    }

    fun update(f: (AppData) -> AppData) = set(f(data))

    // ---------------- sesiones ----------------
    fun actualizarSesion(uid: String, f: (SesionDatos) -> SesionDatos) = update { d ->
        d.copy(sesiones = d.sesiones.map { if (it.uid == uid) it.copy(datos = f(it.datos)) else it })
    }

    fun ponerCodigo(uid: String, id: Int, codigo: String) = actualizarSesion(uid) { s ->
        val m = s.registros.toMutableMap()
        if (codigo.isEmpty() || m[id.toString()] == codigo) m.remove(id.toString()) else m[id.toString()] = codigo
        s.copy(registros = m)
    }

    fun nuevaSesion(fecha: LocalDate, tipo: String, hora: String, nota: String): String {
        val s = Sesion(datos = SesionDatos(fecha = fecha.toString(), tipo = tipo, hora = hora, nota = nota))
        update { it.copy(sesiones = it.sesiones + s) }
        return s.uid
    }

    fun borrarSesion(uid: String) = update { d ->
        d.copy(sesiones = d.sesiones.mapNotNull {
            when {
                it.uid != uid -> it
                it.base == null -> null           // creada en el móvil y nunca exportada: se elimina sin más
                else -> it.copy(borrada = true)
            }
        })
    }

    fun buscarSesion(fecha: LocalDate, tipo: String): Sesion? =
        data.sesionesVivas.firstOrNull { it.datos.fecha == fecha.toString() && it.datos.tipo.equals(tipo, true) }

    /** Aplica un horario a la semana de [fecha]; crea los entrenos que falten. Devuelve nº de entrenos. */
    fun horarioSemana(fecha: LocalDate, horario: String): Int {
        val lunes = fecha.minusDays((fecha.dayOfWeek.value - 1).toLong())
        val filas = data.horarios.filter { it.nombre.equals(horario, true) }
        var n = 0
        var d = data
        for (h in filas) {
            val idx = DIAS_LETRA.indexOf(h.dia.uppercase())
            if (idx < 0) continue
            val dia = lunes.plusDays(idx.toLong())
            val existente = d.sesiones.firstOrNull { !it.borrada && it.datos.fecha == dia.toString() && !it.datos.partido }
            d = if (existente != null) {
                d.copy(sesiones = d.sesiones.map { if (it.uid == existente.uid) it.copy(datos = it.datos.copy(hora = h.texto())) else it })
            } else {
                d.copy(sesiones = d.sesiones + Sesion(datos = SesionDatos(fecha = dia.toString(), tipo = "Entreno", hora = h.texto())))
            }
            n++
        }
        set(d)
        return n
    }

    // ---------------- jugadores ----------------
    fun idLibre(): Int? = (1..40).firstOrNull { id -> data.jugadores.none { it.id == id } }

    fun guardarJugador(id: Int, datos: JugadorDatos) = update { d ->
        if (d.jugadores.any { it.id == id }) d.copy(jugadores = d.jugadores.map { if (it.id == id) it.copy(datos = datos) else it })
        else d.copy(jugadores = (d.jugadores + Jugador(id = id, datos = datos)).sortedBy { it.id })
    }

    fun ponerFoto(id: Int, bmp: Bitmap) {
        val escala = minOf(1f, 480f / maxOf(bmp.width, bmp.height))
        val b = if (escala < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true) else bmp
        val f = File(dirFotos, "j${id}_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        update { d -> d.copy(jugadores = d.jugadores.map { if (it.id == id) it.copy(foto = f.name, fotoNueva = true) else it }) }
    }

    fun fotoBitmap(j: Jugador): Bitmap? {
        val n = j.foto ?: return null
        val f = File(dirFotos, n)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    fun fotoBytes(j: Jugador): ByteArray? {
        val n = j.foto ?: return null
        val f = File(dirFotos, n)
        return if (f.exists()) f.readBytes() else null
    }

    fun guardarFotoBytes(id: Int, bytes: ByteArray): String? = try {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val escala = minOf(1f, 480f / maxOf(bmp.width, bmp.height))
        val b = if (escala < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true) else bmp
        val f = File(dirFotos, "j${id}_x${System.nanoTime()}.jpg")
        val bos = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        f.writeBytes(bos.toByteArray())
        f.name
    } catch (e: Exception) {
        null
    }

    /** Sustituye todo por lo importado del Excel. Borra fotos que ya no se usan. */
    fun reemplazarTodo(nuevo: AppData) {
        set(nuevo)
        val usadas = nuevo.jugadores.mapNotNull { it.foto }.toSet()
        dirFotos.listFiles()?.forEach { if (it.name !in usadas) it.delete() }
    }
}
