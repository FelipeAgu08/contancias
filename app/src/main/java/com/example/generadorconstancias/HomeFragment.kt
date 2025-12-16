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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.generadorconstancias.databinding.FragmentHomeBinding
import com.example.generadorconstancias.utils.TratadoDatos
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FragmentHome : Fragment() {

    // Dirección IP del servidor (ajusta si necesitas otra)
    private var IP: String = "http://192.168.0.173"

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private var currentImageUri: Uri? = null

    private val listaConstancias = listOf("Promedio", "Certificado", "Estudios")

    // ActivityResultLaunchers
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                currentImageUri = uri
                binding.ivSelectedImage.setImageURI(uri)
            } else {
                Toast.makeText(requireContext(), "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentImageUri?.let { binding.ivSelectedImage.setImageURI(it) }
            } else {
                Toast.makeText(requireContext(), "No se tomó la foto", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        viewModel.actualizarHistorial(dbHelper.obtenerTodasConstancias())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper.getInstance(requireContext())

        setupSpinner()
        setupButtons()
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listaConstancias
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnOpciones.adapter = adapter

        binding.spnOpciones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (listaConstancias[position]) {
                    "Promedio" -> binding.textInputLayoutPromedio.visibility = View.VISIBLE
                    else -> {
                        binding.textInputLayoutPromedio.visibility = View.GONE
                        binding.txtPromedio.setText("")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.textInputLayoutPromedio.visibility = View.GONE
            }
        }
    }

    private fun setupButtons() {
        binding.btnSelectGallery.setOnClickListener {
            pickImageFromGallery()
        }

        binding.btnTakePhoto.setOnClickListener {
            if (allPermissionsGranted()) {
                takePhoto()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnGetText.setOnClickListener {
            recognizeTextFromCurrentImage()
        }

        binding.btnGenerarPdf.setOnClickListener {
            generarConstanciaPDF()
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun takePhoto() {
        val photoUri = createImageUri()
        currentImageUri = photoUri
        if (photoUri != null) {
            takePhotoLauncher.launch(photoUri)
        }
    }

    private fun createImageUri(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_${timeStamp}.jpg"
        val storageDir = requireContext().getExternalFilesDir("Pictures")
        val file = File(storageDir, fileName)
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun recognizeTextFromCurrentImage() {
        val uri = currentImageUri
        if (uri == null) {
            Toast.makeText(requireContext(), "No hay imagen seleccionada/capturada", Toast.LENGTH_SHORT).show()
            return
        }

        TextRecognitionHelper.recognizeTextFromUri(requireContext(), uri)
            .addOnSuccessListener { visionText ->
                val recognized = visionText.text.uppercase(Locale.getDefault())
                Log.e("OCR", recognized)
                val tratado = TratadoDatos()
                safeSetText(binding.txtNombre, tratado.obtenerNombre(recognized), 40)
                safeSetText(binding.txtEspecialidad, tratado.obtenerEspecialidad(recognized), 50)
                safeSetText(binding.txtNoControl, tratado.obtenerNoControl(recognized), 50)
                safeSetText(binding.txtCurp, tratado.obtenerCurp(recognized), 23)
                safeSetText(binding.txtGrado, tratado.obtenerGrado(recognized), 50)
                safeSetText(binding.txtGrupo, tratado.obtenerGrupo(recognized), 50)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun safeSetText(field: com.google.android.material.textfield.TextInputEditText, value: String, maxLength: Int) {
        if (value.length <= maxLength) {
            field.setText(value)
        } else {
            field.error = "No se pudo reconocer"
        }
    }

    // ============================================
    // FUNCIÓN PRINCIPAL: Generar Constancia PDF
    // ============================================
    private fun generarConstanciaPDF() {

        val nombre = binding.txtNombre.text.toString().trim()
        val especialidad = binding.txtEspecialidad.text.toString().trim()
        val noControl = binding.txtNoControl.text.toString().trim()
        val curp = binding.txtCurp.text.toString().trim()
        val grado = binding.txtGrado.text.toString().trim()
        val grupo = binding.txtGrupo.text.toString().trim()
        val promedioCapturado = binding.txtPromedio.text.toString().trim()
        val tipoConstancia = listaConstancias[binding.spnOpciones.selectedItemPosition]

        // Campos obligatorios
        if (nombre.isEmpty() || especialidad.isEmpty() || noControl.isEmpty() ||
            curp.isEmpty() || grado.isEmpty() || grupo.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Completa todos los campos.", Toast.LENGTH_LONG).show()
            return
        }

        // Si es constancia de promedio → pedir promedio
        if (tipoConstancia == "Promedio" && promedioCapturado.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el promedio.", Toast.LENGTH_LONG).show()
            return
        }


        // Generar PDF
        generarYMostrarPDF(
            nombre,
            especialidad,
            noControl,
            curp,
            grado,
            grupo,
            promedioCapturado,
            tipoConstancia
        )
    }


    private fun generarYMostrarPDF(
        nombre: String, especialidad: String, noControl: String, curp: String,
        grado: String, grupo: String, promedio: String, tipoConstancia: String
    ) {
        Log.d("PDF_DEBUG", "🔵 INICIANDO generarYMostrarPDF")

        val pdfFile = crearPDF(
            nombre, especialidad, noControl, curp,
            grado, grupo, promedio, tipoConstancia
        )

        if (pdfFile != null) {
            Log.d("PDF_DEBUG", "📄 PDF creado exitosamente")

            var idConstancia: Long = -1

            try {
                Log.d("PDF_DEBUG", "💾 ANTES de insertarConstancia - Total constancias: ${dbHelper.contarTotalConstancias()}")

                idConstancia = dbHelper.insertarConstancia(
                    tipo = tipoConstancia,
                    nombre = nombre,
                    especialidad = especialidad,
                    noControl = noControl,
                    curp = curp,
                    grado = grado,
                    grupo = grupo,
                    promedio = promedio
                )

                Log.d("PDF_DEBUG", "💾 DESPUÉS de insertarConstancia - Total constancias: ${dbHelper.contarTotalConstancias()}")
                Log.d("PDF_DEBUG", "✅ ID retornado: $idConstancia")

            } catch (e: Exception) {
                Log.e("PDF_DEBUG", "❌ Error: ${e.message}", e)
            }

            val intent = Intent(requireContext(), PdfPreviewActivity::class.java)
            intent.putExtra("PDF_PATH", pdfFile.absolutePath)
            intent.putExtra("NO_CONTROL", noControl)
            intent.putExtra("NOMBRE", nombre)
            intent.putExtra("CONSTANCIA_ID", idConstancia.toInt())

            Log.d("PDF_DEBUG", "🚀 Abriendo PdfPreviewActivity con ID: $idConstancia")
            startActivity(intent)

        } else {
            Log.e("PDF_DEBUG", "❌ Error: PDF es null")
            Toast.makeText(requireContext(), "Error al generar el PDF", Toast.LENGTH_LONG).show()
        }
    }

    // ============================================
    // CONSULTAR PROMEDIO DEL WEB SERVICE
    // ============================================

    private val viewModel: ConstanciasViewModel by activityViewModels()
    private fun guardarEnBaseDeDatosLocal(
        noControl: String, nombre: String, curp: String,
        especialidad: String, grado: String, grupo: String, promedio: String
    ) {
        try {
            val guardado = dbHelper.insertarOActualizarAlumno(
                noControl, nombre, curp, especialidad, grado, grupo, promedio
            )

            if (guardado) {
                Log.d("DB_LOCAL", "Alumno guardado localmente: $noControl")
            } else {
                Log.e("DB_LOCAL", "Error al guardar localmente")
            }
        } catch (e: Exception) {
            Log.e("DB_LOCAL", "Excepción al guardar: ${e.message}")
        }
    }


    private fun crearPDF(
        nombre: String, especialidad: String, noControl: String, curp: String,
        grado: String, grupo: String, promedio: String, tipoConstancia: String
    ): File? {
        val pdfDocument = android.graphics.pdf.PdfDocument()

        try {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            paint.textSize = 12f

            val marginLeft = 40f
            val marginRight = 40f
            val contentWidth = 595 - marginLeft - marginRight

            // Encabezado
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

            // 🔐 GENERAR CÓDIGO DE VERIFICACIÓN
            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val codigoVerificacion = SecurityUtils.generarCodigoVerificacionCompacto(
                folio = numeroOficio,
                nombre = nombre,
                noControl = noControl,
                fecha = fechaActual
            )

            // Formatear código para que sea más legible (Ejemplo: A1B2-C3D4-E5F6-G7H8)
            val codigoFormateado = SecurityUtils.formatearCodigoLegible(codigoVerificacion)

            Log.d("PDF_CODIGO", "Código de verificación generado: $codigoFormateado")

            // Información del oficio
            canvas.drawText("Oficio: $numeroOficio(CB238Y)2025", marginLeft, y + 40, paint)
            canvas.drawText("Asunto: CONSTANCIA", marginLeft, y + 60, paint)
            canvas.drawText("Área: SERVS. ESCOLARES", marginLeft, y + 80, paint)
            canvas.drawText("A QUIEN CORRESPONDA:", marginLeft, y + 110, paint)

            paint.textSize = 11f
            canvas.drawText(
                "La que suscribe MA. DEL PILAR GUTIÉRREZ VERA, Jefe de control escolar del",
                marginLeft, y + 130, paint
            )
            canvas.drawText(
                "CENTRO DE BACHILLERATO TECNOLÓGICO industrial y de servicios No. 238,",
                marginLeft, y + 145, paint
            )
            canvas.drawText("con clave del plantel 11DCT0238Y.", marginLeft, y + 160, paint)

            paint.textSize = 12f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("HACE CONSTAR", 297f, y + 185, paint)

            // Texto de la constancia
            val textoConstancia = if (tipoConstancia == "Promedio" && promedio.isNotEmpty()) {
                """
Que $nombre, está inscrito(a) en el $grado semestre de bachillerato en
la especialidad de $especialidad, en el grupo $grupo con número de control
$noControl, en el periodo escolar del 04 de febrero al 18 de julio de 2025, con un
periodo vacacional del 22 de julio al 22 de agosto del presente año, con un promedio
de $promedio.

A petición del interesado(a) se extiende la presente para los trámites y fines legales que
así convengan en la Ciudad de Santa Cruz de Juventino Rosas, Gto., a día ${
                    SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(Date())
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
                    SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(Date())
                }.
            """.trimIndent()
            }

            val textPaint = TextPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = 11f
            }
            val staticLayout = StaticLayout.Builder.obtain(
                textoConstancia, 0, textoConstancia.length, textPaint, contentWidth.toInt()
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1f, 1f)
                .setIncludePad(false)
                .build()

            canvas.save()
            canvas.translate(marginLeft, y + 210)
            staticLayout.draw(canvas)
            canvas.restore()

            // Firma
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 11f
            val bottomTextY = y + 210 + staticLayout.height
            canvas.drawText("ATENTAMENTE", marginLeft, bottomTextY + 30, paint)
            canvas.drawText("MA. DEL PILAR GUTIÉRREZ VERA", marginLeft, bottomTextY + 55, paint)
            canvas.drawText("JEFE DE CONTROL ESCOLAR", marginLeft, bottomTextY + 70, paint)

            // 🔐 CÓDIGO DE VERIFICACIÓN (Agregado al final)
            paint.textSize = 8f
            paint.color = android.graphics.Color.GRAY
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "Código de Verificación: $codigoFormateado",
                297f,
                bottomTextY + 100,
                paint
            )

            // Pie de página
            val botBitmap = BitmapFactory.decodeResource(resources, R.drawable.bot)
            val botHeight = (botBitmap.height * contentWidth.toInt() / botBitmap.width).coerceAtMost(80)
            val botScaled = Bitmap.createScaledBitmap(botBitmap, contentWidth.toInt(), botHeight, true)
            val bottomY = 842f - botScaled.height - 5f
            canvas.drawBitmap(botScaled, marginLeft, bottomY, null)

            pdfDocument.finishPage(page)

            // Guardar PDF
            val cacheDir = requireContext().cacheDir
            val tempFile = File(cacheDir, "preview_temp.pdf")
            tempFile.outputStream().use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            Log.d("PDF", "PDF temporal creado en: ${tempFile.absolutePath}")
            Log.d("PDF", "🔐 Código de verificación: $codigoFormateado")

            return tempFile

        } catch (e: Exception) {
            Log.e("PDF", "Error al crear PDF", e)
            Toast.makeText(requireContext(), "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    // Agregar función para verificar constancia (opcional)
    private fun verificarConstancia() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        val input = android.widget.EditText(requireContext())
        input.hint = "Ingresa el código de verificación"

        builder.setTitle("🔐 Verificar Constancia")
            .setMessage("Ingresa el código de verificación de 16 caracteres")
            .setView(input)
            .setPositiveButton("Verificar") { _, _ ->
                val codigoIngresado = input.text.toString().replace("-", "").trim()

                if (codigoIngresado.length != 16) {
                    Toast.makeText(requireContext(), "⚠️ Código inválido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Buscar en la base de datos
                val constancias = dbHelper.obtenerTodasConstancias()
                val encontrada = constancias.find {
                    it.codigoVerificacion.equals(codigoIngresado, ignoreCase = true)
                }

                if (encontrada != null) {
                    Toast.makeText(
                        requireContext(),
                        "✅ Constancia válida para: ${encontrada.nombre}",
                        Toast.LENGTH_LONG
                    ).show()
                    mostrarDetalleConstancia(encontrada)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Código no encontrado o inválido",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarHistorialConstancias() {
        Log.d("HISTORIAL", "Intentando mostrar historial...")

        val constancias = dbHelper.obtenerTodasConstancias()
        Log.d("HISTORIAL", "Constancias encontradas: ${constancias.size}")

        if (constancias.isEmpty()) {
            Toast.makeText(requireContext(), "No hay constancias guardadas", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottomsheet_historial, null)
        bottomSheet.setContentView(view)

        val rvHistorial = view.findViewById<RecyclerView>(R.id.rvHistorial)
        if (rvHistorial == null) {
            Log.e("HISTORIAL", "ERROR: RecyclerView es NULL!")
            Toast.makeText(requireContext(), "Error: RecyclerView no encontrado", Toast.LENGTH_LONG).show()
            return
        }

        rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        rvHistorial.adapter = HistorialAdapter(constancias) { constancia ->
            bottomSheet.dismiss()
            mostrarDetalleConstancia(constancia)
        }

        bottomSheet.show()
    }

    private fun mostrarDetalleConstancia(constancia: Constancia) {
        val promedioText = if (constancia.promedio.isNotEmpty()) "\nPromedio: ${constancia.promedio}" else ""

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

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📄 Detalle de Constancia")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Rehacer PDF") { _, _ ->
                binding.txtNombre.setText(constancia.nombre)
                binding.txtEspecialidad.setText(constancia.especialidad)
                binding.txtNoControl.setText(constancia.noControl)
                binding.txtCurp.setText(constancia.curp)
                binding.txtGrado.setText(constancia.grado)
                binding.txtGrupo.setText(constancia.grupo)
                if (constancia.promedio.isNotEmpty()) {
                    binding.txtPromedio.setText(constancia.promedio)
                }
                Toast.makeText(requireContext(), "Datos cargados, selecciona el tipo de constancia", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Eliminar") { _, _ ->
                confirmarEliminacion(constancia)
            }
            .show()
    }

    private fun confirmarEliminacion(constancia: Constancia) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Eliminar Constancia")
            .setMessage("¿Estás seguro de eliminar la constancia de ${constancia.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                val eliminado = dbHelper.eliminarConstancia(constancia.id)
                if (eliminado) {
                    Toast.makeText(requireContext(), "Constancia eliminada", Toast.LENGTH_SHORT).show()
                    mostrarHistorialConstancias()
                } else {
                    Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
