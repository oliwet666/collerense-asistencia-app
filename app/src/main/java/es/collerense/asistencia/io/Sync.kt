package es.collerense.asistencia.io

import android.util.Base64
import es.collerense.asistencia.data.AppData
import es.collerense.asistencia.data.JugadorDatos
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.data.SesionDatos
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime
import java.util.UUID

/**
 * Archivo de cambios móvil -> Excel (.colle, JSON UTF-8).
 * Cada elemento lleva "base" (cómo estaba al importar/exportar por última vez) y "datos" (cómo está ahora),
 * para que el Excel haga una fusión a tres bandas y detecte conflictos.
 */
object Sync {

    class Exportacion(val nombreArchivo: String, val contenido: ByteArray, val id: String, val cambios: Int)

    fun exportar(repo: Repo): Exportacion {
        val d = repo.data
        val id = UUID.randomUUID().toString()
        val ahora = LocalDateTime.now().withNano(0)
        var cambios = 0
        val nombres = buildJsonObject { d.jugadores.forEach { put(it.id.toString(), it.datos.nombre) } }
        val jugadores = buildJsonArray {
            d.jugadores.filter { it.modificado }.forEach { j ->
                cambios++
                add(buildJsonObject {
                    put("id", j.id)
                    put("base", j.base?.let { jugadorJson(it) } ?: JsonNull)
                    put("datos", jugadorJson(j.datos))
                    val foto = if (j.fotoNueva) repo.fotoBytes(j) else null
                    put("foto", foto?.let { JsonPrimitive(Base64.encodeToString(it, Base64.NO_WRAP)) } ?: JsonNull)
                })
            }
        }
        val sesiones = buildJsonArray {
            d.sesiones.filter { it.modificada }.forEach { s ->
                if (s.base == null && s.borrada) return@forEach
                cambios++
                add(buildJsonObject {
                    put("base", s.base?.let { sesionJson(it) } ?: JsonNull)
                    put("datos", if (s.borrada) JsonNull else sesionJson(s.datos))
                })
            }
        }
        val raiz = buildJsonObject {
            put("formato", "colle-cambios")
            put("version", 1)
            put("id", id)
            put("previo", d.ultimoExport)
            put("generado", ahora.toString())
            put("origen", d.origen)
            put("equipo", d.equipo.equipo)
            put("temporada", d.equipo.temporada)
            put("nombres", nombres)
            put("jugadores", jugadores)
            put("sesiones", sesiones)
        }
        val eq = d.equipo.equipo.replace(Regex("[^A-Za-z0-9]+"), "")
        val nombre = "cambios_${eq}_%04d%02d%02d_%02d%02d.colle".format(ahora.year, ahora.monthValue, ahora.dayOfMonth, ahora.hour, ahora.minute)
        return Exportacion(nombre, raiz.toString().toByteArray(Charsets.UTF_8), id, cambios)
    }

    /** Tras exportar: lo exportado pasa a ser la nueva base (el Excel ya lo tendrá al importar el archivo). */
    fun marcarExportado(repo: Repo, id: String) = repo.update { d ->
        d.copy(
            jugadores = d.jugadores.map { it.copy(base = it.datos, fotoNueva = false) },
            sesiones = d.sesiones.filter { !it.borrada }.map { it.copy(base = it.datos) },
            ultimoExport = id,
            exportaciones = d.exportaciones + 1
        )
    }

    private fun jugadorJson(j: JugadorDatos): JsonObject = buildJsonObject {
        put("dorsal", j.dorsal); put("nombre", j.nombre); put("tipo", j.tipo); put("dni", j.dni)
        put("nacimiento", j.nacimiento?.let { JsonPrimitive(it) } ?: JsonNull)
        put("alta", j.alta?.let { JsonPrimitive(it) } ?: JsonNull)
        put("baja", j.baja?.let { JsonPrimitive(it) } ?: JsonNull)
        put("motivo", j.motivo); put("obs", j.obs)
    }

    private fun sesionJson(s: SesionDatos): JsonObject = buildJsonObject {
        put("fecha", s.fecha); put("tipo", s.tipo); put("hora", s.hora); put("nota", s.nota)
        put("resultado", s.resultado); put("estado", s.estado)
        put("registros", buildJsonObject {
            s.registros.toSortedMap(compareBy<String> { it.toIntOrNull() ?: 999 }).forEach { (k, v) ->
                val n = v.toIntOrNull()
                put(k, if (n != null) JsonPrimitive(n) else JsonPrimitive(v))
            }
        })
    }

    /** Aplica lo leído del Excel al almacenamiento del móvil (sustituye todo). */
    fun aplicarImportacion(repo: Repo, l: LecturaExcel) {
        val jugadores = l.data.jugadores.map { j ->
            val bytes = l.fotos[j.id]
            if (bytes != null) j.copy(foto = repo.guardarFotoBytes(j.id, bytes), fotoNueva = false) else j
        }
        repo.reemplazarTodo(l.data.copy(jugadores = jugadores, ultimoExport = ""))
    }

    @Suppress("unused")
    private fun nulo(): JsonElement = JsonNull
    @Suppress("unused")
    private fun vacio(): JsonArray = JsonArray(emptyList())
}
