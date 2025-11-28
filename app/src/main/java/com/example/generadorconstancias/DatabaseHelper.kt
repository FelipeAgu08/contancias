package com.example.generadorconstancias

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*
import java.io.Serializable

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "OficioBD"
        private const val DATABASE_VERSION = 5 // ⬆️ Incrementada la versión

        // Tabla de contador de oficios
        private const val TABLE_OFICIO = "oficio"
        private const val COLUMN_OFICIO_ID = "id"
        private const val COLUMN_CONTADOR = "contador"

        // Tabla de constancias detalladas
        private const val TABLE_CONSTANCIAS = "constancias_detalle"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TIPO = "tipo_constancia"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_ESPECIALIDAD = "especialidad"
        private const val COLUMN_NO_CONTROL = "no_control"
        private const val COLUMN_CURP = "curp"
        private const val COLUMN_GRADO = "grado"
        private const val COLUMN_GRUPO = "grupo"
        private const val COLUMN_PROMEDIO = "promedio"
        private const val COLUMN_NUMERO_OFICIO = "numero_oficio"
        private const val COLUMN_FECHA = "fecha_registro"

        // ✨ Nueva tabla de alumnos
        private const val TABLE_ALUMNOS = "alumnos"
        private const val COLUMN_ALUMNO_NO_CONTROL = "noControl"
        private const val COLUMN_ALUMNO_NOMBRE = "nombre"
        private const val COLUMN_ALUMNO_CURP = "curp"
        private const val COLUMN_ALUMNO_ESPECIALIDAD = "especialidad"
        private const val COLUMN_ALUMNO_GRADO = "grado"
        private const val COLUMN_ALUMNO_GRUPO = "grupo"
        private const val COLUMN_ALUMNO_PROMEDIO = "promedio"
        private const val COLUMN_ALUMNO_FECHA = "fechaRegistro"

        // Singleton
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                val newInstance = DatabaseHelper(context.applicationContext)
                instance = newInstance
                newInstance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Crear tabla de oficios con contador
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_OFICIO ($COLUMN_OFICIO_ID INTEGER PRIMARY KEY, $COLUMN_CONTADOR INTEGER)")
        val values = ContentValues().apply {
            put(COLUMN_OFICIO_ID, 1)
            put(COLUMN_CONTADOR, 1)
        }
        db?.insert(TABLE_OFICIO, null, values)

        // Crear tabla de constancias detalladas
        db?.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CONSTANCIAS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIPO TEXT NOT NULL,
                $COLUMN_NOMBRE TEXT NOT NULL,
                $COLUMN_ESPECIALIDAD TEXT NOT NULL,
                $COLUMN_NO_CONTROL TEXT NOT NULL,
                $COLUMN_CURP TEXT NOT NULL,
                $COLUMN_GRADO TEXT NOT NULL,
                $COLUMN_GRUPO TEXT NOT NULL,
                $COLUMN_PROMEDIO TEXT,
                $COLUMN_NUMERO_OFICIO INTEGER,
                $COLUMN_FECHA DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        // ✨ Crear tabla de alumnos
        db?.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_ALUMNOS (
                $COLUMN_ALUMNO_NO_CONTROL TEXT PRIMARY KEY,
                $COLUMN_ALUMNO_NOMBRE TEXT NOT NULL,
                $COLUMN_ALUMNO_CURP TEXT NOT NULL,
                $COLUMN_ALUMNO_ESPECIALIDAD TEXT,
                $COLUMN_ALUMNO_GRADO TEXT,
                $COLUMN_ALUMNO_GRUPO TEXT,
                $COLUMN_ALUMNO_PROMEDIO TEXT,
                $COLUMN_ALUMNO_FECHA TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            // Recrear tabla de constancias si es necesario
            db?.execSQL("DROP TABLE IF EXISTS constancia")
            db?.execSQL("DROP TABLE IF EXISTS constancia_simple")

            db?.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_CONSTANCIAS (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_TIPO TEXT NOT NULL,
                    $COLUMN_NOMBRE TEXT NOT NULL,
                    $COLUMN_ESPECIALIDAD TEXT NOT NULL,
                    $COLUMN_NO_CONTROL TEXT NOT NULL,
                    $COLUMN_CURP TEXT NOT NULL,
                    $COLUMN_GRADO TEXT NOT NULL,
                    $COLUMN_GRUPO TEXT NOT NULL,
                    $COLUMN_PROMEDIO TEXT,
                    $COLUMN_NUMERO_OFICIO INTEGER,
                    $COLUMN_FECHA DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())
        }

        // ✨ Agregar tabla de alumnos en la actualización
        if (oldVersion < 5) {
            db?.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_ALUMNOS (
                    $COLUMN_ALUMNO_NO_CONTROL TEXT PRIMARY KEY,
                    $COLUMN_ALUMNO_NOMBRE TEXT NOT NULL,
                    $COLUMN_ALUMNO_CURP TEXT NOT NULL,
                    $COLUMN_ALUMNO_ESPECIALIDAD TEXT,
                    $COLUMN_ALUMNO_GRADO TEXT,
                    $COLUMN_ALUMNO_GRUPO TEXT,
                    $COLUMN_ALUMNO_PROMEDIO TEXT,
                    $COLUMN_ALUMNO_FECHA TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())
        }
    }

    // ==================== FUNCIONES DE CONTADOR ====================

    fun getContador(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_CONTADOR FROM $TABLE_OFICIO WHERE $COLUMN_OFICIO_ID = 1", null)
        val valor = if (cursor.moveToFirst()) cursor.getInt(0) else 1
        cursor.close()
        return valor
    }

    fun incrementarContador() {
        val db = writableDatabase
        val actual = getContador()
        val values = ContentValues().apply { put(COLUMN_CONTADOR, actual + 1) }
        db.update(TABLE_OFICIO, values, "$COLUMN_OFICIO_ID = ?", arrayOf("1"))
    }

    // ==================== FUNCIONES DE CONSTANCIAS ====================

    fun insertarConstancia(
        tipo: String,
        nombre: String,
        especialidad: String,
        noControl: String,
        curp: String,
        grado: String,
        grupo: String,
        promedio: String = ""
    ): Long {
        val db = this.writableDatabase
        val numeroOficio = getContador()

        val values = ContentValues().apply {
            put(COLUMN_TIPO, tipo)
            put(COLUMN_NOMBRE, nombre)
            put(COLUMN_ESPECIALIDAD, especialidad)
            put(COLUMN_NO_CONTROL, noControl)
            put(COLUMN_CURP, curp)
            put(COLUMN_GRADO, grado)
            put(COLUMN_GRUPO, grupo)

            if (tipo.equals("Promedio", ignoreCase = true) && promedio.isNotEmpty()) {
                put(COLUMN_PROMEDIO, promedio)
            } else {
                put(COLUMN_PROMEDIO, "")
            }

            put(COLUMN_NUMERO_OFICIO, numeroOficio)
        }

        return db.insert(TABLE_CONSTANCIAS, null, values)
    }

    fun obtenerTodasConstancias(): List<Constancia> {
        val constancias = mutableListOf<Constancia>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CONSTANCIAS ORDER BY $COLUMN_FECHA DESC",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                constancias.add(cursorToConstancia(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return constancias
    }

    fun obtenerConstanciasPorTipo(tipo: String): List<Constancia> {
        val constancias = mutableListOf<Constancia>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CONSTANCIAS WHERE $COLUMN_TIPO = ? ORDER BY $COLUMN_FECHA DESC",
            arrayOf(tipo)
        )

        if (cursor.moveToFirst()) {
            do {
                constancias.add(cursorToConstancia(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return constancias
    }

    fun obtenerConstanciaPorNoControl(noControl: String): Constancia? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CONSTANCIAS WHERE $COLUMN_NO_CONTROL = ? ORDER BY $COLUMN_FECHA DESC LIMIT 1",
            arrayOf(noControl)
        )

        var constancia: Constancia? = null
        if (cursor.moveToFirst()) {
            constancia = cursorToConstancia(cursor)
        }
        cursor.close()
        return constancia
    }

    fun contarConstanciasPorTipo(tipo: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CONSTANCIAS WHERE $COLUMN_TIPO = ?",
            arrayOf(tipo)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun contarTotalConstancias(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CONSTANCIAS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun eliminarConstancia(id: Int): Boolean {
        val db = writableDatabase
        val resultado = db.delete(TABLE_CONSTANCIAS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return resultado > 0
    }

    fun buscarConstanciasPorNombre(nombre: String): List<Constancia> {
        val constancias = mutableListOf<Constancia>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CONSTANCIAS WHERE $COLUMN_NOMBRE LIKE ? ORDER BY $COLUMN_FECHA DESC",
            arrayOf("%$nombre%")
        )

        if (cursor.moveToFirst()) {
            do {
                constancias.add(cursorToConstancia(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return constancias
    }

    fun obtenerEstadisticas(): Map<String, Int> {
        val estadisticas = mutableMapOf<String, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_TIPO, COUNT(*) as total FROM $TABLE_CONSTANCIAS GROUP BY $COLUMN_TIPO",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                estadisticas[tipo] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        return estadisticas
    }

    private fun cursorToConstancia(cursor: Cursor): Constancia {
        return Constancia(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO)),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE)),
            especialidad = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESPECIALIDAD)),
            noControl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NO_CONTROL)),
            curp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURP)),
            grado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GRADO)),
            grupo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GRUPO)),
            promedio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMEDIO)) ?: "",
            numeroOficio = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_OFICIO)),
            fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA))
        )
    }

    // ==================== ✨ FUNCIONES DE ALUMNOS (NUEVAS) ====================

    /**
     * Insertar o actualizar alumno
     */
    fun insertarOActualizarAlumno(
        noControl: String,
        nombre: String,
        curp: String,
        especialidad: String,
        grado: String,
        grupo: String,
        promedio: String
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ALUMNO_NO_CONTROL, noControl)
            put(COLUMN_ALUMNO_NOMBRE, nombre)
            put(COLUMN_ALUMNO_CURP, curp)
            put(COLUMN_ALUMNO_ESPECIALIDAD, especialidad)
            put(COLUMN_ALUMNO_GRADO, grado)
            put(COLUMN_ALUMNO_GRUPO, grupo)
            put(COLUMN_ALUMNO_PROMEDIO, promedio)
        }

        return try {
            val result = db.insertWithOnConflict(
                TABLE_ALUMNOS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            result != -1L
        } catch (e: Exception) {
            android.util.Log.e("DB_ERROR", "Error al guardar alumno: ${e.message}")
            false
        }
    }

    /**
     * Obtener alumno por número de control
     */
    fun obtenerAlumno(noControl: String): AlumnoData? {
        val db = this.readableDatabase
        var alumno: AlumnoData? = null

        val cursor: Cursor? = db.query(
            TABLE_ALUMNOS,
            null,
            "$COLUMN_ALUMNO_NO_CONTROL = ?",
            arrayOf(noControl),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                alumno = AlumnoData(
                    noControl = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NO_CONTROL)),
                    nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NOMBRE)),
                    curp = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_CURP)),
                    especialidad = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_ESPECIALIDAD)),
                    grado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_GRADO)),
                    grupo = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_GRUPO)),
                    promedio = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_PROMEDIO))
                )
            }
        }

        return alumno
    }

    /**
     * Obtener todos los alumnos
     */
    fun obtenerTodosLosAlumnos(): List<AlumnoData> {
        val listaAlumnos = mutableListOf<AlumnoData>()
        val db = this.readableDatabase

        val cursor: Cursor? = db.query(
            TABLE_ALUMNOS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ALUMNO_NOMBRE ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val alumno = AlumnoData(
                    noControl = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NO_CONTROL)),
                    nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NOMBRE)),
                    curp = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_CURP)),
                    especialidad = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_ESPECIALIDAD)),
                    grado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_GRADO)),
                    grupo = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_GRUPO)),
                    promedio = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_PROMEDIO))
                )
                listaAlumnos.add(alumno)
            }
        }

        return listaAlumnos
    }

    /**
     * Verificar si existe un alumno
     */
    fun existeAlumno(noControl: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ALUMNOS,
            arrayOf(COLUMN_ALUMNO_NO_CONTROL),
            "$COLUMN_ALUMNO_NO_CONTROL = ?",
            arrayOf(noControl),
            null,
            null,
            null
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    /**
     * Eliminar alumno
     */
    fun eliminarAlumno(noControl: String): Boolean {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_ALUMNOS,
            "$COLUMN_ALUMNO_NO_CONTROL = ?",
            arrayOf(noControl)
        )
        return result > 0
    }
}

// ==================== DATA CLASSES ====================

data class Constancia(
    val id: Int,
    val tipo: String,
    val nombre: String,
    val especialidad: String,
    val noControl: String,
    val curp: String,
    val grado: String,
    val grupo: String,
    val promedio: String,
    val numeroOficio: Int,
    val fecha: String
) : Serializable

// ✨ Nueva data class para alumnos
data class AlumnoData(
    val noControl: String,
    val nombre: String,
    val curp: String,
    val especialidad: String,
    val grado: String,
    val grupo: String,
    val promedio: String
) : Serializable