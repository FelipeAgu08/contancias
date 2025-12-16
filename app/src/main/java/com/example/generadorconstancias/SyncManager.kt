package com.example.generadorconstancias

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject

class SyncManager(private val context: Context) {

    private val dbHelper = DatabaseHelper.getInstance(context)

    // 🔧 CAMBIA ESTA URL POR LA DE TU SERVIDOR
    companion object {
        private const val BASE_URL = "http://192.168.0.173/swconstancias/"
        private const val UPLOAD_URL = "${BASE_URL}upload_constancias.php"
        private const val DOWNLOAD_URL = "${BASE_URL}download_constancias.php"
    }

    // ==================== SUBIR CONSTANCIAS AL SERVIDOR ====================

    fun subirConstancias(
        onSuccess: (mensaje: String, cantidad: Int) -> Unit,
        onError: (error: String) -> Unit
    ) {
        Log.d("SYNC", "📤 Iniciando subida de constancias...")

        // Obtener todas las constancias de la BD local
        val constanciasLocales = dbHelper.obtenerTodasConstancias()

        if (constanciasLocales.isEmpty()) {
            onError("No hay constancias para subir")
            return
        }

        try {
            // Convertir constancias a JSON
            val jsonArray = JSONArray()

            constanciasLocales.forEach { constancia ->
                val jsonConstancia = JSONObject().apply {
                    put("id", constancia.id)
                    put("tipo", constancia.tipo)
                    put("nombre", constancia.nombre)
                    put("especialidad", constancia.especialidad)
                    put("no_control", constancia.noControl)
                    put("curp", constancia.curp)
                    put("grado", constancia.grado)
                    put("grupo", constancia.grupo)
                    put("promedio", constancia.promedio)
                    put("numero_oficio", constancia.numeroOficio)
                    put("fecha", constancia.fecha)
                    put("codigo_verificacion", constancia.codigoVerificacion)
                }
                jsonArray.put(jsonConstancia)
            }

            val jsonBody = JSONObject().apply {
                put("constancias", jsonArray)
            }

            Log.d("SYNC", "📦 Subiendo ${constanciasLocales.size} constancias...")

            // Crear petición Volley
            val request = JsonObjectRequest(
                Request.Method.POST,
                UPLOAD_URL,
                jsonBody,
                { response ->
                    try {
                        val success = response.getBoolean("success")
                        val mensaje = response.getString("message")

                        if (success) {
                            val cantidad = response.optInt("count", constanciasLocales.size)
                            Log.d("SYNC", "✅ $mensaje")
                            onSuccess(mensaje, cantidad)
                        } else {
                            Log.e("SYNC", "❌ Error del servidor: $mensaje")
                            onError(mensaje)
                        }
                    } catch (e: Exception) {
                        Log.e("SYNC", "❌ Error parseando respuesta: ${e.message}")
                        onError("Error al procesar respuesta del servidor")
                    }
                },
                { error ->
                    val errorMsg = when {
                        error.networkResponse == null -> "Sin conexión a internet"
                        error.networkResponse.statusCode == 404 -> "Servidor no encontrado"
                        error.networkResponse.statusCode >= 500 -> "Error del servidor"
                        else -> "Error de conexión: ${error.message}"
                    }
                    Log.e("SYNC", "❌ $errorMsg")
                    onError(errorMsg)
                }
            )

            // Agregar a la cola de Volley
            VolleySingleton.getInstance(context).addToRequestQueue(request)

        } catch (e: Exception) {
            Log.e("SYNC", "❌ Error preparando datos: ${e.message}")
            onError("Error al preparar los datos: ${e.localizedMessage}")
        }
    }

    // ==================== DESCARGAR CONSTANCIAS DEL SERVIDOR ====================

    fun descargarConstancias(
        sobrescribirLocal: Boolean = false,
        onSuccess: (mensaje: String, cantidad: Int) -> Unit,
        onError: (error: String) -> Unit
    ) {
        Log.d("SYNC", "📥 Iniciando descarga de constancias...")

        val request = JsonObjectRequest(
            Request.Method.GET,
            DOWNLOAD_URL,
            null,
            { response ->
                try {
                    val success = response.getBoolean("success")

                    if (!success) {
                        val mensaje = response.getString("message")
                        Log.e("SYNC", "❌ $mensaje")
                        onError(mensaje)
                        return@JsonObjectRequest
                    }

                    val constanciasArray = response.getJSONArray("data")

                    if (constanciasArray.length() == 0) {
                        Log.d("SYNC", "⚠️ No hay constancias en el servidor")
                        onError("No hay datos en el servidor")
                        return@JsonObjectRequest
                    }

                    Log.d("SYNC", "📦 Descargadas ${constanciasArray.length()} constancias")

                    // Limpiar BD local si se solicita
                    if (sobrescribirLocal) {
                        dbHelper.limpiarTodasLasTablas()
                        Log.d("SYNC", "🗑️ Base de datos local limpiada")
                    }

                    var insertadas = 0
                    var errores = 0

                    // Insertar constancias descargadas
                    for (i in 0 until constanciasArray.length()) {
                        val constanciaJson = constanciasArray.getJSONObject(i)

                        try {
                            val resultado = dbHelper.insertarConstancia(
                                tipo = constanciaJson.getString("tipo"),
                                nombre = constanciaJson.getString("nombre"),
                                especialidad = constanciaJson.getString("especialidad"),
                                noControl = constanciaJson.getString("no_control"),
                                curp = constanciaJson.getString("curp"),
                                grado = constanciaJson.getString("grado"),
                                grupo = constanciaJson.getString("grupo"),
                                promedio = constanciaJson.optString("promedio", "")
                            )

                            if (resultado != -1L) {
                                insertadas++
                            } else {
                                errores++
                            }
                        } catch (e: Exception) {
                            Log.e("SYNC", "❌ Error insertando constancia: ${e.message}")
                            errores++
                        }
                    }

                    val mensaje = if (errores == 0) {
                        "✅ $insertadas constancias descargadas correctamente"
                    } else {
                        "⚠️ $insertadas descargadas, $errores con error"
                    }

                    Log.d("SYNC", mensaje)
                    onSuccess(mensaje, insertadas)

                } catch (e: Exception) {
                    Log.e("SYNC", "❌ Error procesando descarga: ${e.message}")
                    onError("Error al procesar datos descargados")
                }
            },
            { error ->
                val errorMsg = when {
                    error.networkResponse == null -> "Sin conexión a internet"
                    error.networkResponse.statusCode == 404 -> "Servidor no encontrado"
                    error.networkResponse.statusCode >= 500 -> "Error del servidor"
                    else -> "Error de conexión: ${error.message}"
                }
                Log.e("SYNC", "❌ $errorMsg")
                onError(errorMsg)
            }
        )

        VolleySingleton.getInstance(context).addToRequestQueue(request)
    }

    // ==================== SINCRONIZACIÓN COMPLETA ====================

    fun sincronizarBidireccional(
        onSuccess: (mensaje: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        // Primero subir, luego descargar
        subirConstancias(
            onSuccess = { msg, cantidad ->
                Log.d("SYNC", "✅ Subida completada, iniciando descarga...")

                descargarConstancias(
                    sobrescribirLocal = false,
                    onSuccess = { msgDescarga, cantidadDescarga ->
                        onSuccess("Sincronización completa:\n↑ $cantidad subidas\n↓ $cantidadDescarga descargadas")
                    },
                    onError = { errorDescarga ->
                        onError("Subida OK, pero error en descarga: $errorDescarga")
                    }
                )
            },
            onError = { error ->
                onError("Error en subida: $error")
            }
        )
    }
}