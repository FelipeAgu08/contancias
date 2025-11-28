    package com.example.generadorconstancias

    import android.Manifest
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import android.graphics.Paint
    import android.net.Uri
    import android.os.Bundle
    import android.text.Layout
    import android.text.StaticLayout
    import android.text.TextPaint
    import android.util.Log
    import android.view.View
    import android.widget.*
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.content.ContextCompat
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.core.content.FileProvider
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.example.generadorconstancias.utils.TratadoDatos
    import com.google.android.material.bottomsheet.BottomSheetDialog
    import com.google.android.material.textfield.TextInputEditText
    import com.google.android.material.textfield.TextInputLayout
    import java.io.File
    import java.text.SimpleDateFormat

    import android.widget.ImageView
    import java.util.*

    class HomeActivity : AppCompatActivity() {

        var IP: String = "http://192.168.0.173"

        private lateinit var ivSelectedImage: ImageView
        private lateinit var btnSelectGallery: Button
        private lateinit var btnTakePhoto: Button
        private lateinit var btnGetText: Button
        private lateinit var btnGenerarPdf: Button
        private lateinit var btnVerHistorial: Button

        private lateinit var txtNombre: TextInputEditText
        private lateinit var txtEspecialidad: TextInputEditText
        private lateinit var txtNoControl: TextInputEditText
        private lateinit var txtCurp: TextInputEditText
        private lateinit var txtGrado: TextInputEditText
        private lateinit var txtGrupo: TextInputEditText
        private lateinit var txtPromedio: TextInputEditText
        private lateinit var textInputLayoutPromedio: TextInputLayout

        private lateinit var spnOpciones: Spinner
        lateinit var dbHelper: DatabaseHelper

        private var currentImageUri: Uri? = null

        private val listaConstancias = listOf("Promedio", "Certificado", "Estudios")

        private val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    currentImageUri = uri
                    ivSelectedImage.setImageURI(uri)
                } else Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT)
                    .show()
            }

        private val takePhotoLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) currentImageUri?.let { ivSelectedImage.setImageURI(it) }
                else Toast.makeText(this, "No se tomó la foto", Toast.LENGTH_SHORT).show()
            }

        private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) takePhoto() else Toast.makeText(
                    this,
                    "Permiso de cámara denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }

        private val requestStoragePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    generarConstanciaPDF()
                } else {
                    Toast.makeText(
                        this,
                        "Permiso de almacenamiento denegado. No se puede guardar el PDF",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_home)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            ivSelectedImage = findViewById(R.id.ivSelectedImage)
            btnSelectGallery = findViewById(R.id.btnSelectGallery)
            btnTakePhoto = findViewById(R.id.btnTakePhoto)
            btnGetText = findViewById(R.id.btnGetText)
            btnGenerarPdf = findViewById(R.id.btnGenerarPdf)
            btnVerHistorial = findViewById(R.id.btnVerHistorial)

            txtNombre = findViewById(R.id.txtNombre)
            txtEspecialidad = findViewById(R.id.txtEspecialidad)
            txtNoControl = findViewById(R.id.txtNoControl)
            txtCurp = findViewById(R.id.txtCurp)
            txtGrado = findViewById(R.id.txtGrado)
            txtGrupo = findViewById(R.id.txtGrupo)
            txtPromedio = findViewById(R.id.txtPromedio)
            textInputLayoutPromedio = findViewById(R.id.textInputLayoutPromedio)

            spnOpciones = findViewById(R.id.spnOpciones)

            dbHelper = DatabaseHelper.getInstance(this)

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaConstancias)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spnOpciones.adapter = adapter

            spnOpciones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (listaConstancias[position]) {
                        "Promedio" -> textInputLayoutPromedio.visibility = View.VISIBLE
                        else -> {
                            textInputLayoutPromedio.visibility = View.GONE
                            txtPromedio.setText("")
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    textInputLayoutPromedio.visibility = View.GONE
                }
            }

            btnSelectGallery.setOnClickListener { pickImageFromGallery() }
            btnTakePhoto.setOnClickListener {
                if (allPermissionsGranted()) takePhoto()
                else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            btnGetText.setOnClickListener { recognizeTextFromCurrentImage() }
            btnGenerarPdf.setOnClickListener {
                generarConstanciaPDF()
            }
            btnVerHistorial.setOnClickListener {
                mostrarHistorialConstancias()
            }
        }

        private fun pickImageFromGallery() = pickImageLauncher.launch("image/*")

        private fun takePhoto() {
            val photoUri = createImageUri()
            currentImageUri = photoUri
            if (photoUri != null) takePhotoLauncher.launch(photoUri)
        }

        private fun createImageUri(): Uri? {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}.jpg"
            val storageDir = getExternalFilesDir("Pictures")
            val file = File(storageDir, fileName)
            return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        }

        private fun allPermissionsGranted(): Boolean {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun recognizeTextFromCurrentImage() {
            val uri = currentImageUri
            if (uri == null) {
                Toast.makeText(this, "No hay imagen seleccionada/capturada", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            TextRecognitionHelper.recognizeTextFromUri(this, uri)
                .addOnSuccessListener { visionText ->
                    val recognized = visionText.text.uppercase()
                    Log.e("OCR", recognized)
                    val tratado = TratadoDatos()
                    safeSetText(txtNombre, tratado.obtenerNombre(recognized), 40)
                    safeSetText(txtEspecialidad, tratado.obtenerEspecialidad(recognized), 50)
                    safeSetText(txtNoControl, tratado.obtenerNoControl(recognized), 50)
                    safeSetText(txtCurp, tratado.obtenerCurp(recognized), 23)
                    safeSetText(txtGrado, tratado.obtenerGrado(recognized), 50)
                    safeSetText(txtGrupo, tratado.obtenerGrupo(recognized), 50)
                }
                .addOnFailureListener { e -> e.printStackTrace() }
        }

        private fun safeSetText(field: TextInputEditText, value: String, maxLength: Int) {
            if (value.length <= maxLength) field.setText(value)
            else field.error = "No se pudo reconocer"
        }


        // ============================================
        // FUNCIÓN PRINCIPAL: Generar Constancia PDF
        // ============================================
        private fun generarConstanciaPDF() {
            val nombre = txtNombre.text.toString()
            val especialidad = txtEspecialidad.text.toString()
            val noControl = txtNoControl.text.toString()
            val curp = txtCurp.text.toString()
            val grado = txtGrado.text.toString()
            val grupo = txtGrupo.text.toString()
            // ⚠️ Si NO tienes txtPromedio, cambia esta línea por: val promedioCapturado = "0.0"
            val promedioCapturado = txtPromedio.text.toString()
            val tipoConstancia = listaConstancias[spnOpciones.selectedItemPosition]

            // Validación de campos
            if (nombre.isEmpty() || especialidad.isEmpty() || noControl.isEmpty() ||
                curp.isEmpty() || grado.isEmpty() || grupo.isEmpty() || promedioCapturado.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Por favor, completa todos los campos antes de generar el PDF.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // 1️⃣ Consultar si existe el alumno en el servidor
            consultarPromedioWS { promedioServidor ->

                val promedioFinal: String

                if (promedioServidor == null) {
                    // ❌ NO EXISTE - Usar el promedio capturado y guardar TODO
                    android.util.Log.d(
                        "PDF_FLOW",
                        "Alumno no existe, se guardará con promedio: $promedioCapturado"
                    )
                    promedioFinal = promedioCapturado

                    // Guardar en servidor web
                    guardarEnBaseDeDatos(
                        tipoConstancia,
                        nombre,
                        especialidad,
                        noControl,
                        curp,
                        grado,
                        grupo,
                        promedioFinal
                    ) { guardadoExitoso ->
                        if (guardadoExitoso) {
                            // 💾 Guardar también localmente en SQLite
                            guardarEnBaseDeDatosLocal(
                                noControl,
                                nombre,
                                curp,
                                especialidad,
                                grado,
                                grupo,
                                promedioFinal
                            )

                            generarYMostrarPDF(
                                nombre,
                                especialidad,
                                noControl,
                                curp,
                                grado,
                                grupo,
                                promedioFinal,
                                tipoConstancia
                            )
                        }
                    }

                } else {
                    // ✅ SÍ EXISTE - Usar el promedio del servidor
                    android.util.Log.d(
                        "PDF_FLOW",
                        "Alumno existe, promedio servidor: $promedioServidor"
                    )
                    promedioFinal = promedioServidor

                    // Actualizar datos en servidor web
                    guardarEnBaseDeDatos(
                        tipoConstancia,
                        nombre,
                        especialidad,
                        noControl,
                        curp,
                        grado,
                        grupo,
                        promedioFinal
                    ) { guardadoExitoso ->
                        if (guardadoExitoso) {
                            // 💾 Guardar también localmente en SQLite
                            guardarEnBaseDeDatosLocal(
                                noControl,
                                nombre,
                                curp,
                                especialidad,
                                grado,
                                grupo,
                                promedioFinal
                            )

                            generarYMostrarPDF(
                                nombre,
                                especialidad,
                                noControl,
                                curp,
                                grado,
                                grupo,
                                promedioFinal,
                                tipoConstancia
                            )
                        }
                    }
                }
            }
        }

        // ============================================
        // Función auxiliar para generar y mostrar PDF
        // ============================================
        private fun generarYMostrarPDF(
            nombre: String,
            especialidad: String,
            noControl: String,
            curp: String,
            grado: String,
            grupo: String,
            promedio: String,
            tipoConstancia: String
        ) {
            android.util.Log.d("PDF_HISTORIAL", "Generando PDF para: $nombre")

            val pdfFile = crearPDF(
                nombre,
                especialidad,
                noControl,
                curp,
                grado,
                grupo,
                promedio,
                tipoConstancia
            )

            if (pdfFile != null) {
                android.util.Log.d("PDF_HISTORIAL", "PDF creado exitosamente")

                // ✨ GUARDAR EN HISTORIAL LOCAL
                try {
                    val idConstancia = dbHelper.insertarConstancia(
                        tipo = tipoConstancia,
                        nombre = nombre,
                        especialidad = especialidad,
                        noControl = noControl,
                        curp = curp,
                        grado = grado,
                        grupo = grupo,
                        promedio = promedio
                    )

                    if (idConstancia > 0) {
                        android.util.Log.d("PDF_HISTORIAL", "✅ Constancia guardada en historial con ID: $idConstancia")
                        // Incrementar contador de oficios
                        dbHelper.incrementarContador()
                        android.util.Log.d("PDF_HISTORIAL", "✅ Contador incrementado")
                    } else {
                        android.util.Log.e("PDF_HISTORIAL", "❌ Error al guardar en historial (ID <= 0)")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PDF_HISTORIAL", "❌ Excepción al guardar historial: ${e.message}", e)
                }

                // Mostrar vista previa del PDF
                val intent = Intent(this, PdfPreviewActivity::class.java)
                intent.putExtra("PDF_PATH", pdfFile.absolutePath)
                intent.putExtra("NO_CONTROL", noControl)
                intent.putExtra("NOMBRE", nombre)
                startActivity(intent)

            } else {
                android.util.Log.e("PDF_HISTORIAL", "❌ Error: PDF es null")
                Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_LONG).show()
            }
        }

        // ============================================
        // CONSULTAR PROMEDIO DEL WEB SERVICE
        // ============================================
        private fun consultarPromedioWS(onResult: (String?) -> Unit) {
            val noControl = txtNoControl.text.toString().trim()

            if (noControl.isEmpty()) {
                Toast.makeText(this, "Ingresa el número de control", Toast.LENGTH_LONG).show()
                onResult(null)
                return
            }

            val url = "$IP/SWConstancias/getPromedio.php"
            val jsonEntrada = org.json.JSONObject()
            jsonEntrada.put("noControl", noControl)
            // NO enviar "accion" para consulta (PHP lo maneja con accion vacía)

            android.util.Log.d("WS_DEBUG", "═══════════════════════════════")
            android.util.Log.d("WS_DEBUG", "CONSULTANDO PROMEDIO")
            android.util.Log.d("WS_DEBUG", "URL: $url")
            android.util.Log.d("WS_DEBUG", "JSON: $jsonEntrada")
            android.util.Log.d("WS_DEBUG", "═══════════════════════════════")

            val request = object : com.android.volley.toolbox.StringRequest(
                Method.POST,
                url,
                { response ->
                    android.util.Log.d("WS_DEBUG", "📥 Respuesta RAW: $response")

                    try {
                        val jsonResponse = org.json.JSONObject(response)
                        android.util.Log.d("WS_DEBUG", "📦 JSON parseado: $jsonResponse")

                        // Obtener el valor de success (puede ser boolean o int)
                        val successValue = jsonResponse.get("success")
                        android.util.Log.d(
                            "WS_DEBUG",
                            "🔍 Success RAW: $successValue (${successValue.javaClass.simpleName})"
                        )

                        val isSuccess = when (successValue) {
                            is Boolean -> successValue
                            is Int -> successValue == 200
                            is String -> successValue.equals(
                                "true",
                                ignoreCase = true
                            ) || successValue == "200"

                            else -> false
                        }

                        android.util.Log.d("WS_DEBUG", "✅ Success interpretado: $isSuccess")

                        if (isSuccess) {
                            // ✅ ALUMNO EXISTE - Usar promedio del servidor
                            val promedio = jsonResponse.getString("promedio")
                            android.util.Log.d(
                                "WS_DEBUG",
                                "✅ Alumno encontrado, promedio: $promedio"
                            )
                            onResult(promedio)
                        } else {
                            // ❌ ALUMNO NO EXISTE - Se creará con promedio capturado
                            val message = jsonResponse.optString("message", "Alumno no encontrado")
                            android.util.Log.d("WS_DEBUG", "ℹ️ $message - Se creará nuevo registro")
                            // NO mostrar Toast de error - es un flujo normal
                            onResult(null)
                        }

                    } catch (e: org.json.JSONException) {
                        android.util.Log.e("WS_DEBUG", "❌ Error JSON: ${e.message}")
                        android.util.Log.e("WS_DEBUG", "Respuesta que causó error: $response")
                        Toast.makeText(
                            this@HomeActivity,
                            "Error al leer respuesta del servidor",
                            Toast.LENGTH_LONG
                        ).show()
                        onResult(null)

                    } catch (e: Exception) {
                        android.util.Log.e("WS_DEBUG", "❌ Excepción: ${e.message}", e)
                        Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                        onResult(null)
                    }
                },
                { error ->
                    android.util.Log.e("WS_DEBUG", "═══════════════════════════════")
                    android.util.Log.e("WS_DEBUG", "❌ ERROR VOLLEY")
                    android.util.Log.e("WS_DEBUG", "Mensaje: ${error.message}")
                    android.util.Log.e(
                        "WS_DEBUG",
                        "Código HTTP: ${error.networkResponse?.statusCode}"
                    )

                    val errorBody = error.networkResponse?.data?.let {
                        String(it, Charsets.UTF_8)
                    } ?: "Sin cuerpo"

                    android.util.Log.e("WS_DEBUG", "Cuerpo error: $errorBody")
                    android.util.Log.e("WS_DEBUG", "═══════════════════════════════")

                    // Manejar códigos HTTP específicos
                    when (error.networkResponse?.statusCode) {
                        404 -> {
                            // El servidor respondió 404 - Alumno no encontrado (ya no debería pasar con HTTP 200)
                            android.util.Log.d(
                                "WS_DEBUG",
                                "404: Alumno no encontrado, procesando como NULL"
                            )
                            onResult(null)
                        }

                        422 -> {
                            Toast.makeText(
                                this@HomeActivity,
                                "Datos incompletos",
                                Toast.LENGTH_LONG
                            ).show()
                            onResult(null)
                        }

                        500 -> {
                            Toast.makeText(
                                this@HomeActivity,
                                "Error del servidor",
                                Toast.LENGTH_LONG
                            ).show()
                            onResult(null)
                        }

                        else -> {
                            Toast.makeText(
                                this@HomeActivity,
                                "Error de conexión",
                                Toast.LENGTH_LONG
                            ).show()
                            onResult(null)
                        }
                    }
                }
            ) {
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }

                override fun getBody(): ByteArray {
                    return jsonEntrada.toString().toByteArray(Charsets.UTF_8)
                }
            }

            // Timeout más largo para debug
            request.setRetryPolicy(
                com.android.volley.DefaultRetryPolicy(
                    10000,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )
            )

            com.example.generadorconstancias.VolleySingleton.getInstance(this)
                .addToRequestQueue(request)
        }

        // ============================================
        // GUARDAR EN BASE DE DATOS LOCAL (SQLite)
        // ============================================
        private fun guardarEnBaseDeDatosLocal(
            noControl: String,
            nombre: String,
            curp: String,
            especialidad: String,
            grado: String,
            grupo: String,
            promedio: String
        ) {
            try {
                val guardado = dbHelper.insertarOActualizarAlumno(
                    noControl,
                    nombre,
                    curp,
                    especialidad,
                    grado,
                    grupo,
                    promedio
                )

                if (guardado) {
                    android.util.Log.d("DB_LOCAL", "Alumno guardado localmente: $noControl")
                } else {
                    android.util.Log.e("DB_LOCAL", "Error al guardar localmente")
                }
            } catch (e: Exception) {
                android.util.Log.e("DB_LOCAL", "Excepción al guardar: ${e.message}")
            }
        }

        // ============================================
        // GUARDAR EN BASE DE DATOS
        // ============================================
        private fun guardarEnBaseDeDatos(
            tipoConstancia: String,
            nombre: String,
            especialidad: String,
            noControl: String,
            curp: String,
            grado: String,
            grupo: String,
            promedio: String,
            onResult: ((Boolean) -> Unit)? = null
        ) {
            val url = "$IP/SWConstancias/getPromedio.php"
            val jsonEntrada = org.json.JSONObject()

            jsonEntrada.put("accion", "guardar")
            jsonEntrada.put("noControl", noControl)
            jsonEntrada.put("nombre", nombre)
            jsonEntrada.put("curp", curp)
            jsonEntrada.put("grado", grado)
            jsonEntrada.put("grupo", grupo)
            jsonEntrada.put("especialidad", especialidad)
            jsonEntrada.put("promedio", promedio)

            android.util.Log.d("WS_GUARDAR", "URL: $url")
            android.util.Log.d("WS_GUARDAR", "JSON Enviado: $jsonEntrada")

            val request = com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.POST,
                url,
                jsonEntrada,
                { response ->
                    try {
                        android.util.Log.d("WS_GUARDAR", "Respuesta: $response")

                        // Verificar si viene "success" como int (antiguo) o boolean (nuevo)
                        val success = when {
                            response.has("success") && response.get("success") is Boolean ->
                                response.getBoolean("success")

                            response.has("success") && response.get("success") is Int ->
                                response.getInt("success") == 200

                            else -> false
                        }

                        val message = response.getString("message")

                        if (success) {
                            val accion = response.optString("accion", "guardado")
                            Toast.makeText(
                                this@HomeActivity,
                                "Datos $accion correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            onResult?.invoke(true)
                        } else {
                            Toast.makeText(this@HomeActivity, "Error: $message", Toast.LENGTH_LONG)
                                .show()
                            onResult?.invoke(false)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WS_GUARDAR", "Excepción: ${e.message}", e)
                        Toast.makeText(
                            this@HomeActivity,
                            "Error al procesar respuesta",
                            Toast.LENGTH_LONG
                        ).show()
                        onResult?.invoke(false)
                    }
                },
                { error ->
                    android.util.Log.e("WS_GUARDAR", "Error: ${error.message}")
                    android.util.Log.e(
                        "WS_GUARDAR",
                        "Código HTTP: ${error.networkResponse?.statusCode}"
                    )

                    val errorBody = error.networkResponse?.data?.let {
                        String(it, Charsets.UTF_8)
                    }
                    android.util.Log.e("WS_GUARDAR", "Cuerpo del error: $errorBody")

                    val errorMsg = when (error.networkResponse?.statusCode) {
                        422 -> "Faltan datos obligatorios"
                        500 -> "Error al guardar en el servidor"
                        else -> "Error de conexión: ${error.message}"
                    }
                    Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                    onResult?.invoke(false)
                }
            )

            com.example.generadorconstancias.VolleySingleton.getInstance(this)
                .addToRequestQueue(request)
        }

        private fun crearPDF(
            nombre: String, especialidad: String, noControl: String, curp: String,
            grado: String, grupo: String, promedio: String, tipoConstancia: String
        ): File? {

            val pdfDocument = android.graphics.pdf.PdfDocument()

            try {
                val pageInfo =
                    android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.textSize = 12f

                val marginLeft = 40f
                val marginRight = 40f
                val contentWidth = 595 - marginLeft - marginRight

                val topBitmap = BitmapFactory.decodeResource(resources, R.drawable.top)
                val topScaled = Bitmap.createScaledBitmap(
                    topBitmap,
                    contentWidth.toInt(),
                    (topBitmap.height * contentWidth.toInt() / topBitmap.width).coerceAtMost(120),
                    true
                )
                canvas.drawBitmap(topScaled, marginLeft, 10f, null)
                var y = (topScaled.height + 10).toFloat()

                paint.textAlign = Paint.Align.LEFT
                val numeroOficio = dbHelper.getContador()
                canvas.drawText("Oficio: $numeroOficio(CB238Y)2025", marginLeft, y + 40, paint)
                canvas.drawText("Asunto: CONSTANCIA", marginLeft, y + 60, paint)
                canvas.drawText("Área: SERVS. ESCOLARES", marginLeft, y + 80, paint)
                canvas.drawText("A QUIEN CORRESPONDA:", marginLeft, y + 110, paint)

                paint.textSize = 11f
                canvas.drawText(
                    "La que suscribe MA. DEL PILAR GUTIÉRREZ VERA, Jefe de control escolar del",
                    marginLeft,
                    y + 130,
                    paint
                )
                canvas.drawText(
                    "CENTRO DE BACHILLERATO TECNOLÓGICO industrial y de servicios No. 238,",
                    marginLeft,
                    y + 145,
                    paint
                )
                canvas.drawText("con clave del plantel 11DCT0238Y.", marginLeft, y + 160, paint)

                paint.textSize = 12f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("HACE CONSTAR", 297f, y + 185, paint)

                val textoConstancia = if (tipoConstancia == "Promedio" && promedio.isNotEmpty()) {
                    """
    Que $nombre, está inscrito(a) en el $grado semestre de bachillerato en
    la especialidad de $especialidad, en el grupo $grupo con número de control
    $noControl, en el periodo escolar del 04 de febrero al 18 de julio de 2025, con un
    periodo vacacional del 22 de julio al 22 de agosto del presente año, con un promedio
    de $promedio.
    
    A petición del interesado(a) se extiende la presente para los trámites y fines legales que
    así convengan en la Ciudad de Santa Cruz de Juventino Rosas, Gto., a día ${
                        SimpleDateFormat(
                            "dd 'de' MMMM 'de' yyyy",
                            Locale("es", "MX")
                        ).format(Date())
                    }.
    """.trimIndent()
                } else {
                    """
    Que $nombre, está inscrito(a) en el $grado semestre de bachillerato en
    la especialidad de $especialidad, en el grupo $grupo con número de control
    $noControl, en el periodo escolar del 04 de febrero al 18 de julio de 2025, con un
    periodo vacacional del 22 de julio al 22 de agosto del presente año.
    
    A petición del interesado(a) se extiende la presente para los trámites y fines legales que
    así convengan en la Ciudad de Santa Cruz de Juventino Rosas, Gto., a día ${
                        SimpleDateFormat(
                            "dd 'de' MMMM 'de' yyyy",
                            Locale("es", "MX")
                        ).format(Date())
                    }.
    """.trimIndent()
                }

                val textPaint =
                    TextPaint().apply { color = android.graphics.Color.BLACK; textSize = 11f }
                val staticLayout = StaticLayout.Builder.obtain(
                    textoConstancia,
                    0,
                    textoConstancia.length,
                    textPaint,
                    contentWidth.toInt()
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1f, 1f)
                    .setIncludePad(false)
                    .build()

                canvas.save()
                canvas.translate(marginLeft, y + 210)
                staticLayout.draw(canvas)
                canvas.restore()

                paint.textAlign = Paint.Align.LEFT
                paint.textSize = 11f
                val bottomTextY = y + 210 + staticLayout.height
                canvas.drawText("ATENTAMENTE", marginLeft, bottomTextY + 30, paint)
                canvas.drawText("MA. DEL PILAR GUTIÉRREZ VERA", marginLeft, bottomTextY + 55, paint)
                canvas.drawText("JEFE DE CONTROL ESCOLAR", marginLeft, bottomTextY + 70, paint)

                val botBitmap = BitmapFactory.decodeResource(resources, R.drawable.bot)
                val botHeight =
                    (botBitmap.height * contentWidth.toInt() / botBitmap.width).coerceAtMost(80)
                val botScaled =
                    Bitmap.createScaledBitmap(botBitmap, contentWidth.toInt(), botHeight, true)
                val bottomY = 842f - botScaled.height - 5f
                canvas.drawBitmap(botScaled, marginLeft, bottomY, null)

                pdfDocument.finishPage(page)

                val cacheDir = cacheDir
                val tempFile = File(cacheDir, "preview_temp.pdf")

                tempFile.outputStream().use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                Log.d("PDF", "PDF temporal creado en: ${tempFile.absolutePath}")
                return tempFile

            } catch (e: Exception) {
                Log.e("PDF", "Error al crear PDF", e)
                Toast.makeText(this, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                return null
            } finally {
                pdfDocument.close()
            }
        }


        private fun mostrarHistorialConstancias() {
            android.util.Log.d("HISTORIAL", "Intentando mostrar historial...")

            val constancias = dbHelper.obtenerTodasConstancias()
            android.util.Log.d("HISTORIAL", "Constancias encontradas: ${constancias.size}")

            if (constancias.isEmpty()) {
                Toast.makeText(this, "No hay constancias guardadas", Toast.LENGTH_SHORT).show()
                android.util.Log.d("HISTORIAL", "Lista vacía, mostrando Toast")
                return
            }

            android.util.Log.d("HISTORIAL", "Creando BottomSheet...")

            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottomsheet_historial, null)
            bottomSheet.setContentView(view)

            android.util.Log.d("HISTORIAL", "BottomSheet creado, configurando RecyclerView...")

            // Configurar RecyclerView
            val rvHistorial = view.findViewById<RecyclerView>(R.id.rvHistorial)

            if (rvHistorial == null) {
                android.util.Log.e("HISTORIAL", "ERROR: RecyclerView es NULL!")
                Toast.makeText(this, "Error: RecyclerView no encontrado", Toast.LENGTH_LONG).show()
                return
            }

            android.util.Log.d("HISTORIAL", "RecyclerView encontrado, configurando adapter...")

            rvHistorial.layoutManager = LinearLayoutManager(this)
            rvHistorial.adapter = HistorialAdapter(constancias) { constancia ->
                android.util.Log.d("HISTORIAL", "Click en constancia: ${constancia.nombre}")
                bottomSheet.dismiss()
                mostrarDetalleConstancia(constancia)
            }

            android.util.Log.d("HISTORIAL", "Adapter configurado, mostrando BottomSheet...")


            try {
                bottomSheet.show()
                android.util.Log.d("HISTORIAL", "BottomSheet mostrado exitosamente")
            } catch (e: Exception) {
                android.util.Log.e("HISTORIAL", "ERROR al mostrar BottomSheet: ${e.message}", e)
                Toast.makeText(this, "Error al mostrar historial: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }

        private fun mostrarDetalleConstancia(constancia: Constancia) {
            val promedioText =
                if (constancia.promedio.isNotEmpty()) "\nPromedio: ${constancia.promedio}" else ""

            val mensaje = """
        Tipo: ${constancia.tipo}
        Nombre: ${constancia.nombre}
        Especialidad: ${constancia.especialidad}
        No. Control: ${constancia.noControl}
        CURP: ${constancia.curp}
        Grado: ${constancia.grado}°
        Grupo: ${constancia.grupo}$promedioText
        Número de Oficio: ${constancia.numeroOficio}
        Fecha: ${constancia.fecha}
    """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📄 Detalle de Constancia")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Rehacer PDF") { _, _ ->
                    // Autocompletar campos con los datos de esta constancia
                    txtNombre.setText(constancia.nombre)
                    txtEspecialidad.setText(constancia.especialidad)
                    txtNoControl.setText(constancia.noControl)
                    txtCurp.setText(constancia.curp)
                    txtGrado.setText(constancia.grado)
                    txtGrupo.setText(constancia.grupo)
                    if (constancia.promedio.isNotEmpty()) {
                        txtPromedio.setText(constancia.promedio)
                    }

                    Toast.makeText(
                        this,
                        "Datos cargados, selecciona el tipo de constancia",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton("Eliminar") { _, _ ->
                    confirmarEliminacion(constancia)
                }
                .show()
        }

        private fun confirmarEliminacion(constancia: Constancia) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Eliminar Constancia")
                .setMessage("¿Estás seguro de eliminar la constancia de ${constancia.nombre}?")
                .setPositiveButton("Eliminar") { _, _ ->
                    val eliminado = dbHelper.eliminarConstancia(constancia.id)
                    if (eliminado) {
                        Toast.makeText(this, "Constancia eliminada", Toast.LENGTH_SHORT).show()
                        // Refrescar el historial
                        mostrarHistorialConstancias()
                    } else {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }