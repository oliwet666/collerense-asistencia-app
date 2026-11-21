package es.collerense.asistencia.io

import android.util.Xml
import es.collerense.asistencia.data.AppData
import es.collerense.asistencia.data.Equipo
import es.collerense.asistencia.data.Horario
import es.collerense.asistencia.data.Jugador
import es.collerense.asistencia.data.JugadorDatos
import es.collerense.asistencia.data.Sesion
import es.collerense.asistencia.data.SesionDatos
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream

/** Resultado de leer el Excel: datos + bytes de las fotos por id de jugador. */
class LecturaExcel(val data: AppData, val fotos: Map<Int, ByteArray>)

class CeldaX(val valor: String, val tipo: String?, val vm: Int?)

/**
 * Lector propio de .xlsm/.xlsx (sin librerías): lee solo los valores introducidos
 * (no fórmulas) de ASISTENCIA, JUGADORES y AJUSTES, usando los nombres definidos del libro.
 */
object XlsmReader {

    fun leer(input: InputStream, nombreArchivo: String): LecturaExcel {
        val zip = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { z ->
            var e = z.nextEntry
            while (e != null) {
                if (!e.isDirectory) zip[e.name.trimStart('/')] = z.readBytes()
                e = z.nextEntry
            }
        }
        val wb = zip["xl/workbook.xml"] ?: throw IllegalArgumentException("No es un archivo de Excel válido")
        val hojas = mutableMapOf<String, String>()      // nombre -> rId
        val nombres = mutableMapOf<String, String>()    // nombre definido -> referencia
        parse(wb) { p ->
            if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") {
                val rid = (0 until p.attributeCount).firstOrNull { p.getAttributeName(it) == "id" || p.getAttributeName(it).endsWith(":id") }
                    ?.let { p.getAttributeValue(it) }
                hojas[p.getAttributeValue(null, "name")] = rid ?: ""
            }
            if (p.eventType == XmlPullParser.START_TAG && p.name == "definedName") {
                val n = p.getAttributeValue(null, "name")
                val local = p.getAttributeValue(null, "localSheetId")
                val txt = p.nextText()
                if (local == null) nombres[n] = txt
            }
        }
        val rels = relaciones(zip["xl/_rels/workbook.xml.rels"])
        fun rutaHoja(nombre: String): String {
            val rid = hojas[nombre] ?: throw IllegalArgumentException("Falta la hoja $nombre: ¿es el Excel de asistencia?")
            return resolver("xl/", rels[rid] ?: "")
        }
        val compartidas = sharedStrings(zip["xl/sharedStrings.xml"])
        val cacheHojas = mutableMapOf<String, Map<String, CeldaX>>()
        fun hoja(nombre: String): Map<String, CeldaX> = cacheHojas.getOrPut(nombre) {
            leerHoja(zip[rutaHoja(nombre)] ?: ByteArray(0), compartidas)
        }

        fun ref(nombre: String): Rango = Rango.parse(nombres[nombre] ?: throw IllegalArgumentException("Falta el nombre $nombre en el Excel"))
        fun celda(r: Rango, fila: Int, col: Int): CeldaX? = hoja(r.hoja)[colLetra(col) + fila]
        fun texto(c: CeldaX?): String = c?.valor?.trim() ?: ""
        fun fecha(c: CeldaX?): String? {
            val v = c?.valor?.toDoubleOrNull() ?: return null
            if (v < 1) return null
            return LocalDate.of(1899, 12, 30).plusDays(v.toLong()).toString()
        }
        fun num(c: CeldaX?): String {
            val s = texto(c)
            val d = s.toDoubleOrNull() ?: return s
            return if (d == Math.floor(d)) d.toLong().toString() else s
        }
        fun cfg(n: String): CeldaX? = nombres[n]?.let { val r = Rango.parse(it); celda(r, r.f1, r.c1) }

        // ---------- AJUSTES ----------
        val equipo = Equipo(
            club = texto(cfg("CFG_CLUB")).ifEmpty { "UD Collerense" },
            equipo = texto(cfg("CFG_EQUIPO")),
            temporada = texto(cfg("CFG_TEMP")),
            inicio = fecha(cfg("CFG_INICIO")) ?: "",
            fin = fecha(cfg("CFG_FIN")) ?: "",
            umbral = texto(cfg("CFG_UMBRAL")).toDoubleOrNull() ?: 0.75,
            dias = texto(cfg("CFG_DIAS")),
            partido = texto(cfg("CFG_PARTIDO")),
            entrenador = texto(cfg("CFG_ENTRENADOR"))
        )
        val horarios = mutableListOf<Horario>()
        nombres["T_HORARIOS"]?.let {
            val r = Rango.parse(it)
            for (f in r.f1..r.f2) {
                val n = texto(celda(r, f, r.c1))
                val d = texto(celda(r, f, r.c1 + 1)).uppercase()
                if (n.isNotEmpty() && d.isNotEmpty()) horarios += Horario(n, d, texto(celda(r, f, r.c1 + 3)), texto(celda(r, f, r.c1 + 4)))
            }
        }
        val motivos = mutableListOf<String>()
        nombres["L_MOTIVOS"]?.let {
            val r = Rango.parse(it)
            for (f in r.f1..r.f2) texto(celda(r, f, r.c1)).let { m -> if (m.isNotEmpty()) motivos += m }
        }

        // ---------- JUGADORES ----------
        val rId = ref("J_ID"); val rDor = ref("J_DORSAL"); val rNom = ref("J_NOMBRE")
        val rTipo = nombres["J_TIPO"]?.let { Rango.parse(it) }
        val rNac = ref("J_NAC"); val rFoto = ref("J_FOTO"); val rAlta = ref("J_ALTA"); val rBaja = ref("J_BAJA")
        val rMot = ref("J_MOTIVO"); val rObs = nombres["J_OBS"]?.let { Rango.parse(it) }
        val jugadores = mutableListOf<Jugador>()
        val celdasFoto = mutableMapOf<Int, Int>()   // id -> vm
        for (i in 0..(rId.f2 - rId.f1)) {
            val nombre = texto(celda(rNom, rNom.f1 + i, rNom.c1))
            if (nombre.isEmpty()) continue
            val id = num(celda(rId, rId.f1 + i, rId.c1)).toIntOrNull() ?: (i + 1)
            val dni = rTipo?.let { texto(celda(it, it.f1 + i, it.c1 + 1)) } ?: ""
            val datos = JugadorDatos(
                dorsal = num(celda(rDor, rDor.f1 + i, rDor.c1)),
                nombre = nombre,
                tipo = rTipo?.let { texto(celda(it, it.f1 + i, it.c1)) }.orEmpty().ifEmpty { "Plantilla" },
                dni = dni,
                nacimiento = fecha(celda(rNac, rNac.f1 + i, rNac.c1)),
                alta = fecha(celda(rAlta, rAlta.f1 + i, rAlta.c1)),
                baja = fecha(celda(rBaja, rBaja.f1 + i, rBaja.c1)),
                motivo = texto(celda(rMot, rMot.f1 + i, rMot.c1)),
                obs = rObs?.let { texto(celda(it, it.f1 + i, it.c1)) } ?: ""
            )
            jugadores += Jugador(id = id, datos = datos, base = datos)
            celda(rFoto, rFoto.f1 + i, rFoto.c1)?.vm?.let { celdasFoto[id] = it }
        }

        // ---------- ASISTENCIA ----------
        val rF = ref("S_FECHA"); val rT = ref("S_TIPO"); val rH = ref("S_HORA"); val rN = ref("S_NOTA"); val rE = ref("S_EST")
        val rG = ref("GRID")
        val sesiones = mutableListOf<Sesion>()
        for (c in rF.c1..rF.c2) {
            val f = fecha(celda(rF, rF.f1, c)) ?: continue
            val regs = mutableMapOf<String, String>()
            for (k in 0..(rG.f2 - rG.f1)) {
                val v = num(celda(rG, rG.f1 + k, c)).uppercase()
                if (v.isNotEmpty()) regs[(k + 1).toString()] = v
            }
            val datos = SesionDatos(
                fecha = f,
                tipo = texto(celda(rT, rT.f1, c)).ifEmpty { "Entreno" }.replaceFirstChar { it.uppercase() },
                hora = texto(celda(rH, rH.f1, c)),
                nota = texto(celda(rN, rN.f1, c)),
                resultado = texto(celda(rN, rN.f1 + 1, c)),
                estado = texto(celda(rE, rE.f1, c)),
                registros = regs
            )
            sesiones += Sesion(datos = datos, base = datos)
        }

        val fotos = try { fotosEnCelda(zip, celdasFoto) } catch (e: Exception) { emptyMap() }
        val data = AppData(
            equipo = equipo, horarios = horarios,
            motivos = if (motivos.isEmpty()) AppData().motivos else motivos,
            jugadores = jugadores, sesiones = sesiones,
            origen = nombreArchivo, importado = java.time.LocalDateTime.now().withNano(0).toString()
        )
        return LecturaExcel(data, fotos)
    }

    // ------------------------------------------------------------------
    private fun parse(bytes: ByteArray, f: (XmlPullParser) -> Unit) {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            f(p)
            ev = p.next()
        }
    }

    private fun localName(n: String) = n.substringAfter(':')

    private fun relaciones(bytes: ByteArray?): Map<String, String> {
        val m = mutableMapOf<String, String>()
        if (bytes == null) return m
        parse(bytes) { p ->
            if (p.eventType == XmlPullParser.START_TAG && localName(p.name) == "Relationship") {
                m[p.getAttributeValue(null, "Id")] = p.getAttributeValue(null, "Target")
            }
        }
        return m
    }

    private fun resolver(base: String, target: String): String {
        if (target.startsWith("/")) return target.trimStart('/')
        val partes = (base.trimEnd('/') + "/" + target).split('/').toMutableList()
        val out = mutableListOf<String>()
        for (x in partes) {
            when (x) {
                "", "." -> {}
                ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
                else -> out += x
            }
        }
        return out.joinToString("/")
    }

    private fun sharedStrings(bytes: ByteArray?): List<String> {
        val out = mutableListOf<String>()
        if (bytes == null) return out
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var sb: StringBuilder? = null
        var enRph = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (localName(p.name)) {
                    "si" -> sb = StringBuilder()
                    "rPh" -> enRph = true
                    "t" -> if (!enRph) sb?.append(p.nextText())
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (localName(p.name)) {
                    "si" -> { out += sb?.toString() ?: ""; sb = null }
                    "rPh" -> enRph = false
                }
            }
            ev = p.next()
        }
        return out
    }

    private fun leerHoja(bytes: ByteArray, compartidas: List<String>): Map<String, CeldaX> {
        val m = HashMap<String, CeldaX>()
        if (bytes.isEmpty()) return m
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var ref: String? = null
        var tipo: String? = null
        var vm: Int? = null
        var valor: String? = null
        var inline = StringBuilder()
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (localName(p.name)) {
                    "c" -> {
                        ref = p.getAttributeValue(null, "r"); tipo = p.getAttributeValue(null, "t")
                        vm = p.getAttributeValue(null, "vm")?.toIntOrNull(); valor = null; inline = StringBuilder()
                    }
                    "v" -> valor = p.nextText()
                    "t" -> inline.append(p.nextText())
                }
            } else if (ev == XmlPullParser.END_TAG && localName(p.name) == "c") {
                val r = ref
                if (r != null) {
                    val v = when (tipo) {
                        "s" -> valor?.toIntOrNull()?.let { compartidas.getOrNull(it) } ?: ""
                        "inlineStr" -> inline.toString()
                        "e" -> ""
                        else -> valor ?: ""
                    }
                    if (v.isNotEmpty() || vm != null) m[r] = CeldaX(v, tipo, vm)
                }
                ref = null
            }
            ev = p.next()
        }
        return m
    }

    /** Fotos insertadas "en celda" (richData). */
    private fun fotosEnCelda(zip: Map<String, ByteArray>, celdas: Map<Int, Int>): Map<Int, ByteArray> {
        if (celdas.isEmpty()) return emptyMap()
        val md = zip["xl/metadata.xml"] ?: return emptyMap()
        val rcV = mutableListOf<Int>()
        val rvb = mutableListOf<Int>()
        var enValueMeta = false
        parse(md) { p ->
            if (p.eventType == XmlPullParser.START_TAG) {
                when (localName(p.name)) {
                    "valueMetadata" -> enValueMeta = true
                    "rc" -> if (enValueMeta) rcV += p.getAttributeValue(null, "v").toInt()
                    "rvb" -> rvb += p.getAttributeValue(null, "i").toInt()
                }
            } else if (p.eventType == XmlPullParser.END_TAG && localName(p.name) == "valueMetadata") enValueMeta = false
        }
        // rich values: primer <v> de cada <rv> = índice de relación
        val rvPrimero = mutableListOf<Int>()
        zip["xl/richData/rdrichvalue.xml"]?.let { b ->
            var primero: Int? = null
            var enRv = false
            parse(b) { p ->
                if (p.eventType == XmlPullParser.START_TAG && localName(p.name) == "rv") { enRv = true; primero = null }
                else if (p.eventType == XmlPullParser.START_TAG && localName(p.name) == "v" && enRv) {
                    val t = p.nextText()
                    if (primero == null) primero = t.toIntOrNull() ?: -1
                } else if (p.eventType == XmlPullParser.END_TAG && localName(p.name) == "rv") {
                    rvPrimero += (primero ?: -1); enRv = false
                }
            }
        }
        val relIds = mutableListOf<String>()
        zip["xl/richData/richValueRel.xml"]?.let { b ->
            parse(b) { p ->
                if (p.eventType == XmlPullParser.START_TAG && localName(p.name) == "rel") {
                    val id = (0 until p.attributeCount).firstOrNull { localName(p.getAttributeName(it)) == "id" }?.let { p.getAttributeValue(it) }
                    relIds += id ?: ""
                }
            }
        }
        val rels = relaciones(zip["xl/richData/_rels/richValueRel.xml.rels"])
        val out = mutableMapOf<Int, ByteArray>()
        for ((id, vm) in celdas) {
            val bk = rcV.getOrNull(vm - 1) ?: continue
            val rv = rvb.getOrNull(bk) ?: continue
            val relIdx = rvPrimero.getOrNull(rv) ?: continue
            val rid = relIds.getOrNull(relIdx) ?: continue
            val target = rels[rid] ?: continue
            val ruta = resolver("xl/richData/", target)
            zip[ruta]?.let { out[id] = it }
        }
        return out
    }
}

data class Rango(val hoja: String, val c1: Int, val f1: Int, val c2: Int, val f2: Int) {
    companion object {
        fun parse(s: String): Rango {
            val txt = s.trim().removePrefix("=")
            val hoja = txt.substringBeforeLast('!').trim('\'').replace("''", "'")
            val partes = txt.substringAfterLast('!').replace("$", "").split(':')
            val (c1, f1) = celda(partes[0])
            val (c2, f2) = if (partes.size > 1) celda(partes[1]) else c1 to f1
            return Rango(hoja, c1, f1, c2, f2)
        }

        private fun celda(r: String): Pair<Int, Int> {
            val letras = r.takeWhile { it.isLetter() }
            val num = r.drop(letras.length).toInt()
            var c = 0
            for (ch in letras.uppercase()) c = c * 26 + (ch - 'A' + 1)
            return c to num
        }
    }
}

fun colLetra(c: Int): String {
    var n = c
    val sb = StringBuilder()
    while (n > 0) {
        val r = (n - 1) % 26
        sb.insert(0, ('A' + r))
        n = (n - 1) / 26
    }
    return sb.toString()
}
