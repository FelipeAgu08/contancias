    package com.example.generadorconstancias

    import android.content.ContentValues
    import android.content.Context
    import android.database.Cursor
    import android.database.sqlite.SQLiteDatabase
    import android.database.sqlite.SQLiteOpenHelper
    import java.io.Serializable
    import java.text.SimpleDateFormat
    import java.util.*

    class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        companion object {
            private const val DATABASE_NAME = "OficioBD"
            private const val DATABASE_VERSION = 8 // ⬆️ Incrementado para nueva columna

            private const val TABLE_OFICIO = "oficio"
            private const val COLUMN_OFICIO_ID = "id"
            private const val COLUMN_CONTADOR = "contador"

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
            private const val COLUMN_CODIGO_VERIFICACION = "codigo_verificacion" // 🔐 NUEVA

            private const val TABLE_ALUMNOS = "alumnos"
            private const val COLUMN_ALUMNO_NO_CONTROL = "noControl"
            private const val COLUMN_ALUMNO_NOMBRE = "nombre"
            private const val COLUMN_ALUMNO_CURP = "curp"
            private const val COLUMN_ALUMNO_ESPECIALIDAD = "especialidad"
            private const val COLUMN_ALUMNO_GRADO = "grado"
            private const val COLUMN_ALUMNO_GRUPO = "grupo"
            private const val COLUMN_ALUMNO_PROMEDIO = "promedio"
            private const val COLUMN_ALUMNO_FECHA = "fechaRegistro"

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
            // Crear tabla de oficios sin insertar datos
            db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_OFICIO ($COLUMN_OFICIO_ID INTEGER PRIMARY KEY, $COLUMN_CONTADOR INTEGER)")

            // Crear tabla de constancias detalladas CON código de verificación
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
                    $COLUMN_FECHA DATETIME DEFAULT CURRENT_TIMESTAMP,
                    $COLUMN_CODIGO_VERIFICACION TEXT
                )
            """.trimIndent())

            // Crear tabla de alumnos
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

            // 🔐 MIGRACIÓN: Encriptar datos existentes
            if (oldVersion < 7) {
                android.util.Log.d("DB_MIGRATION", "🔐 Iniciando encriptación de datos existentes...")
                encriptarDatosExistentes(db)
            }

            // 🔐 MIGRACIÓN: Agregar columna de código de verificación
            if (oldVersion < 8) {
                try {
                    db?.execSQL("ALTER TABLE $TABLE_CONSTANCIAS ADD COLUMN $COLUMN_CODIGO_VERIFICACION TEXT")
                    android.util.Log.d("DB_MIGRATION", "✅ Columna codigo_verificacion agregada")

                    // Generar códigos para registros existentes
                    generarCodigosParaRegistrosExistentes(db)
                } catch (e: Exception) {
                    android.util.Log.e("DB_MIGRATION", "Error al agregar columna: ${e.message}")
                }
            }
        }

        // ==================== 🔐 MIGRACIÓN DE DATOS ====================

        private fun encriptarDatosExistentes(db: SQLiteDatabase?) {
            db ?: return

            try {
                // Encriptar tabla de constancias
                val cursorConstancias = db.rawQuery("SELECT * FROM $TABLE_CONSTANCIAS", null)
                if (cursorConstancias.moveToFirst()) {
                    do {
                        val id = cursorConstancias.getInt(cursorConstancias.getColumnIndexOrThrow(COLUMN_ID))
                        val nombre = cursorConstancias.getString(cursorConstancias.getColumnIndexOrThrow(COLUMN_NOMBRE))
                        val curp = cursorConstancias.getString(cursorConstancias.getColumnIndexOrThrow(COLUMN_CURP))

                        // Solo encriptar si aún no está encriptado (verificar formato)
                        if (!esTextoEncriptado(nombre)) {
                            val values = ContentValues().apply {
                                put(COLUMN_NOMBRE, SecurityUtils.encrypt(nombre))
                                put(COLUMN_CURP, SecurityUtils.encrypt(curp))
                            }
                            db.update(TABLE_CONSTANCIAS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
                        }
                    } while (cursorConstancias.moveToNext())
                }
                cursorConstancias.close()

                // Encriptar tabla de alumnos
                val cursorAlumnos = db.rawQuery("SELECT * FROM $TABLE_ALUMNOS", null)
                if (cursorAlumnos.moveToFirst()) {
                    do {
                        val noControl = cursorAlumnos.getString(cursorAlumnos.getColumnIndexOrThrow(COLUMN_ALUMNO_NO_CONTROL))
                        val nombre = cursorAlumnos.getString(cursorAlumnos.getColumnIndexOrThrow(COLUMN_ALUMNO_NOMBRE))
                        val curp = cursorAlumnos.getString(cursorAlumnos.getColumnIndexOrThrow(COLUMN_ALUMNO_CURP))

                        if (!esTextoEncriptado(nombre)) {
                            val values = ContentValues().apply {
                                put(COLUMN_ALUMNO_NOMBRE, SecurityUtils.encrypt(nombre))
                                put(COLUMN_ALUMNO_CURP, SecurityUtils.encrypt(curp))
                            }
                            db.update(TABLE_ALUMNOS, values, "$COLUMN_ALUMNO_NO_CONTROL = ?", arrayOf(noControl))
                        }
                    } while (cursorAlumnos.moveToNext())
                }
                cursorAlumnos.close()

                android.util.Log.d("DB_MIGRATION", "✅ Encriptación completada")
            } catch (e: Exception) {
                android.util.Log.e("DB_MIGRATION", "❌ Error en migración: ${e.message}")
            }
        }

        private fun generarCodigosParaRegistrosExistentes(db: SQLiteDatabase?) {
            db ?: return

            try {
                val cursor = db.rawQuery(
                    "SELECT $COLUMN_ID, $COLUMN_NOMBRE, $COLUMN_NO_CONTROL, $COLUMN_NUMERO_OFICIO, $COLUMN_FECHA FROM $TABLE_CONSTANCIAS WHERE $COLUMN_CODIGO_VERIFICACION IS NULL OR $COLUMN_CODIGO_VERIFICACION = ''",
                    null
                )

                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                        val nombreEncriptado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE))
                        val noControl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NO_CONTROL))
                        val numeroOficio = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_OFICIO))
                        val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA))

                        // Desencriptar nombre
                        val nombre = try {
                            SecurityUtils.decrypt(nombreEncriptado)
                        } catch (e: Exception) {
                            nombreEncriptado
                        }

                        // Generar código
                        val codigo = SecurityUtils.generarCodigoVerificacionCompacto(
                            folio = numeroOficio,
                            nombre = nombre,
                            noControl = noControl,
                            fecha = fecha
                        )

                        val values = ContentValues().apply {
                            put(COLUMN_CODIGO_VERIFICACION, codigo)
                        }

                        db.update(TABLE_CONSTANCIAS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
                        android.util.Log.d("DB_MIGRATION", "✅ Código generado para ID $id: $codigo")

                    } while (cursor.moveToNext())
                }
                cursor.close()

                android.util.Log.d("DB_MIGRATION", "✅ Códigos de verificación generados")
            } catch (e: Exception) {
                android.util.Log.e("DB_MIGRATION", "❌ Error generando códigos: ${e.message}")
            }
        }

        private fun esTextoEncriptado(texto: String): Boolean {
            // El texto encriptado en Base64 tiene un patrón específico
            return texto.matches(Regex("^[A-Za-z0-9+/]+=*$")) && texto.length > 20
        }

        // ==================== FUNCIONES DE CONTADOR ====================

        fun obtenerYIncrementarContadorInterno(): Int {
            val db = writableDatabase
            val stackTrace = Thread.currentThread().stackTrace
            android.util.Log.d("CONTADOR_DEBUG", "🔍 LLAMADO DESDE: ${stackTrace.getOrNull(3)?.methodName ?: "desconocido"}")
            android.util.Log.d("CONTADOR_DEBUG", "📄 Clase: ${stackTrace.getOrNull(3)?.className ?: "desconocido"}")

            var contador = 1
            val cursor = db.rawQuery(
                "SELECT $COLUMN_CONTADOR FROM $TABLE_OFICIO WHERE $COLUMN_OFICIO_ID = 1",
                null
            )

            if (cursor.moveToFirst()) {
                contador = cursor.getInt(0)
                android.util.Log.d("CONTADOR_DEBUG", "📊 Contador encontrado en BD: $contador")
            } else {
                android.util.Log.d("CONTADOR_DEBUG", "⚠️ No existe contador, creando uno nuevo con valor 1")
                val values = ContentValues().apply {
                    put(COLUMN_OFICIO_ID, 1)
                    put(COLUMN_CONTADOR, 1)
                }
                db.insert(TABLE_OFICIO, null, values)
            }
            cursor.close()

            android.util.Log.d("CONTADOR_DEBUG", "📝 Retornando número de oficio: $contador")

            val nuevoContador = contador + 1
            val values = ContentValues().apply {
                put(COLUMN_CONTADOR, nuevoContador)
            }
            val rowsUpdated = db.update(TABLE_OFICIO, values, "$COLUMN_OFICIO_ID = ?", arrayOf("1"))

            android.util.Log.d("CONTADOR_DEBUG", "⬆️ Incrementando contador a $nuevoContador (filas actualizadas: $rowsUpdated)")

            return contador
        }

        fun getContador(): Int {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_CONTADOR FROM $TABLE_OFICIO WHERE $COLUMN_OFICIO_ID = 1",
                null
            )
            val valor = if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                1
            }
            cursor.close()
            return valor
        }

        // ==================== 🔐 FUNCIONES DE CONSTANCIAS (ENCRIPTADAS) ====================

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
            android.util.Log.d("CONTADOR_DEBUG", "═══════════════════════════════")
            android.util.Log.d("CONTADOR_DEBUG", "🔵 INSERTANDO CONSTANCIA")

            val db = this.writableDatabase
            val numeroOficio = obtenerYIncrementarContadorInterno()

            // 🔐 Obtener fecha actual
            val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // 🔐 GENERAR CÓDIGO DE VERIFICACIÓN
            val codigoVerificacion = SecurityUtils.generarCodigoVerificacionCompacto(
                folio = numeroOficio,
                nombre = nombre,
                noControl = noControl,
                fecha = fecha
            )

            android.util.Log.d("CONTADOR_DEBUG", "🔐 Código generado: $codigoVerificacion")

            // 👤 PRIMERO: Guardar o actualizar el alumno en la tabla alumnos
            val alumnoGuardado = insertarOActualizarAlumno(
                noControl = noControl,
                nombre = nombre,
                curp = curp,
                especialidad = especialidad,
                grado = grado,
                grupo = grupo,
                promedio = promedio
            )

            if (alumnoGuardado) {
                android.util.Log.d("CONTADOR_DEBUG", "✅ Alumno guardado/actualizado: $noControl")
            } else {
                android.util.Log.w("CONTADOR_DEBUG", "⚠️ Error al guardar alumno: $noControl")
            }

            // 📝 SEGUNDO: Insertar la constancia
            val values = ContentValues().apply {
                put(COLUMN_TIPO, tipo)
                put(COLUMN_NOMBRE, SecurityUtils.encrypt(nombre)) // 🔐 Encriptado
                put(COLUMN_ESPECIALIDAD, especialidad)
                put(COLUMN_NO_CONTROL, noControl)
                put(COLUMN_CURP, SecurityUtils.encrypt(curp)) // 🔐 Encriptado
                put(COLUMN_GRADO, grado)
                put(COLUMN_GRUPO, grupo)

                if (tipo.equals("Promedio", ignoreCase = true) && promedio.isNotEmpty()) {
                    put(COLUMN_PROMEDIO, promedio)
                } else {
                    put(COLUMN_PROMEDIO, "")
                }

                put(COLUMN_NUMERO_OFICIO, numeroOficio)
                put(COLUMN_CODIGO_VERIFICACION, codigoVerificacion) // 🔐 CÓDIGO
                put(COLUMN_FECHA, fecha)
            }

            val resultado = db.insert(TABLE_CONSTANCIAS, null, values)

            if (resultado != -1L) {
                android.util.Log.d("CONTADOR_DEBUG", "✅ Constancia insertada con oficio #$numeroOficio (ID: $resultado)")
                android.util.Log.d("CONTADOR_DEBUG", "✅ Código de verificación: $codigoVerificacion")
            } else {
                android.util.Log.e("CONTADOR_DEBUG", "❌ Error al insertar constancia")
            }

            android.util.Log.d("CONTADOR_DEBUG", "═══════════════════════════════")
            return resultado
        }

        fun obtenerTodasConstancias(): List<Constancia> {
            val constancias = mutableListOf<Constancia>()
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_CONSTANCIAS ORDER BY $COLUMN_NUMERO_OFICIO DESC",
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

        fun obtenerConstanciaPorCodigo(codigo: String): Constancia? {
            val db = readableDatabase
            val codigoLimpio = codigo.replace("-", "").trim().uppercase()

            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_CONSTANCIAS WHERE UPPER(REPLACE($COLUMN_CODIGO_VERIFICACION, '-', '')) = ?",
                arrayOf(codigoLimpio)
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
            // ⚠️ BÚSQUEDA CON DATOS ENCRIPTADOS: Necesita desencriptar todo
            val todasConstancias = obtenerTodasConstancias()
            return todasConstancias.filter {
                it.nombre.contains(nombre, ignoreCase = true)
            }
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

        fun obtenerConstanciasPorCarrera(): Map<String, Int> {
            val estadisticas = mutableMapOf<String, Int>()
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ESPECIALIDAD, COUNT(*) as total FROM $TABLE_CONSTANCIAS GROUP BY $COLUMN_ESPECIALIDAD",
                null
            )

            if (cursor.moveToFirst()) {
                do {
                    val carrera = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESPECIALIDAD))
                    val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))

                    // Normalizar (remover acentos y convertir a mayúsculas)
                    val carreraNormalizada = carrera
                        .replace("á", "a", ignoreCase = true)
                        .replace("é", "e", ignoreCase = true)
                        .replace("í", "i", ignoreCase = true)
                        .replace("ó", "o", ignoreCase = true)
                        .replace("ú", "u", ignoreCase = true)
                        .uppercase()
                        .trim()

                    // Sumar si ya existe
                    estadisticas[carreraNormalizada] = (estadisticas[carreraNormalizada] ?: 0) + total

                } while (cursor.moveToNext())
            }
            cursor.close()
            return estadisticas
        }

        private fun cursorToConstancia(cursor: Cursor): Constancia {
            val nombreEncriptado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE))
            val curpEncriptado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURP))

            // 🔐 Obtener código de verificación (puede ser null en registros antiguos)
            val codigoVerificacion = try {
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CODIGO_VERIFICACION)) ?: ""
            } catch (e: Exception) {
                ""
            }

            return Constancia(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                tipo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIPO)),
                nombre = try { SecurityUtils.decrypt(nombreEncriptado) } catch (e: Exception) { nombreEncriptado }, // 🔓 Desencriptado
                especialidad = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESPECIALIDAD)),
                noControl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NO_CONTROL)),
                curp = try { SecurityUtils.decrypt(curpEncriptado) } catch (e: Exception) { curpEncriptado }, // 🔓 Desencriptado
                grado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GRADO)),
                grupo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GRUPO)),
                promedio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMEDIO)) ?: "",
                numeroOficio = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_OFICIO)),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA)),
                codigoVerificacion = codigoVerificacion // 🔐 NUEVO CAMPO
            )
        }

        // ==================== 🔐 FUNCIONES DE ALUMNOS (ENCRIPTADAS) ====================

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
                put(COLUMN_ALUMNO_NOMBRE, SecurityUtils.encrypt(nombre)) // 🔐 Encriptado
                put(COLUMN_ALUMNO_CURP, SecurityUtils.encrypt(curp)) // 🔐 Encriptado
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
        fun contarTotalAlumnos(): Int {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ALUMNOS", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            return count
        }

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
                    val nombreEncriptado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NOMBRE))
                    val curpEncriptado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_CURP))

                    alumno = AlumnoData(
                        noControl = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NO_CONTROL)),
                        nombre = try { SecurityUtils.decrypt(nombreEncriptado) } catch (e: Exception) { nombreEncriptado }, // 🔓 Desencriptado
                        curp = try { SecurityUtils.decrypt(curpEncriptado) } catch (e: Exception) { curpEncriptado }, // 🔓 Desencriptado
                        especialidad = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_ESPECIALIDAD)),
                        grado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_GRADO)),
                        grupo = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_GRUPO)),
                        promedio = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_PROMEDIO))
                    )
                }
            }

            return alumno
        }

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
                    val nombreEncriptado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NOMBRE))
                    val curpEncriptado = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_CURP))

                    val alumno = AlumnoData(
                        noControl = it.getString(it.getColumnIndexOrThrow(COLUMN_ALUMNO_NO_CONTROL)),
                        nombre = try { SecurityUtils.decrypt(nombreEncriptado) } catch (e: Exception) { nombreEncriptado },
                        curp = try { SecurityUtils.decrypt(curpEncriptado) } catch (e: Exception) { curpEncriptado },
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

        fun eliminarAlumno(noControl: String): Boolean {
            val db = this.writableDatabase
            val result = db.delete(
                TABLE_ALUMNOS,
                "$COLUMN_ALUMNO_NO_CONTROL = ?",
                arrayOf(noControl)
            )
            return result > 0
        }

        // ==================== MÉTODO DE LIMPIEZA (SOLO DESARROLLO) ====================

        fun limpiarTodasLasTablas() {
            val db = writableDatabase

            db.execSQL("DELETE FROM $TABLE_CONSTANCIAS")
            db.execSQL("DELETE FROM $TABLE_ALUMNOS")
            db.execSQL("DELETE FROM $TABLE_OFICIO")

            val values = ContentValues().apply {
                put(COLUMN_OFICIO_ID, 1)
                put(COLUMN_CONTADOR, 1)
            }
            db.insert(TABLE_OFICIO, null, values)

            android.util.Log.d("DB_LIMPIEZA", "✅ Base de datos limpiada completamente")
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
        val fecha: String,
        val codigoVerificacion: String = "" // 🔐 NUEVO CAMPO
    ) : Serializable

    data class AlumnoData(
        val noControl: String,
        val nombre: String,
        val curp: String,
        val especialidad: String,
        val grado: String,
        val grupo: String,
        val promedio: String
    ) : Serializable