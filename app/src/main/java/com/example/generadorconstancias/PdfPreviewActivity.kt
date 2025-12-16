package com.example.generadorconstancias

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.generadorconstancias.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfPreviewActivity : AppCompatActivity() {

    private lateinit var ivPdfPreview: ImageView
    private lateinit var btnDescargar: Button
    private lateinit var btnCompartir: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvPageInfo: TextView

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfFile: File? = null
    private var currentPageIndex = 0
    private var isRendererClosed = false
    private var constanciaId: Int = -1 // 🔧 NUEVA VARIABLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_preview)

        ivPdfPreview = findViewById(R.id.ivPdfPreview)
        btnDescargar = findViewById(R.id.btnDescargar)
        btnCompartir = findViewById(R.id.btnCompartir)
        btnCancelar = findViewById(R.id.btnCancelar)
        tvPageInfo = findViewById(R.id.tvPageInfo)

        val pdfPath = intent.getStringExtra("PDF_PATH")
        constanciaId = intent.getIntExtra("CONSTANCIA_ID", -1) // 🔧 RECIBIR ID

        if (pdfPath != null) {
            pdfFile = File(pdfPath)
            openPdfRenderer(pdfFile!!)
        } else {
            Toast.makeText(this, "Error: No se encontró el PDF", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnDescargar.setOnClickListener {
            descargarPDF()
        }

        btnCompartir.setOnClickListener {
            compartirPDF()
        }

        btnCancelar.setOnClickListener {
            cancelarConstancia() // 🔧 FUNCIÓN ACTUALIZADA
        }

        // Navegación entre páginas (si tiene más de una)
        ivPdfPreview.setOnClickListener {
            if (pdfRenderer != null && pdfRenderer!!.pageCount > 1) {
                currentPageIndex = (currentPageIndex + 1) % pdfRenderer!!.pageCount
                showPage(currentPageIndex)
            }
        }
    }

    private fun openPdfRenderer(file: File) {
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            showPage(0)
        } catch (e: Exception) {
            Log.e("PDFPreview", "Error al abrir PDF", e)
            Toast.makeText(this, "Error al cargar vista previa", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showPage(index: Int) {
        currentPage?.close()

        currentPage = pdfRenderer?.openPage(index)
        currentPage?.let { page ->
            // Calcular el tamaño de la imagen
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val scale = screenWidth.toFloat() / page.width.toFloat()
            val scaledHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(screenWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            ivPdfPreview.setImageBitmap(bitmap)

            // Mostrar info de página
            val totalPages = pdfRenderer?.pageCount ?: 1
            tvPageInfo.text = "Página ${index + 1} de $totalPages"
        }
    }

    // 🔧 NUEVA FUNCIÓN PARA CANCELAR CONSTANCIA
    private fun cancelarConstancia() {
        try {
            Log.d("CANCEL_DEBUG", "🔴 CANCELANDO constancia ID: $constanciaId")
            Log.d("CANCEL_DEBUG", "📊 Total constancias ANTES: ${DatabaseHelper.getInstance(this).contarTotalConstancias()}")

            // Cerrar el renderer
            currentPage?.close()
            pdfRenderer?.close()
            parcelFileDescriptor?.close()

            // Eliminar el archivo PDF temporal
            val pdfEliminado = pdfFile?.delete() ?: false
            Log.d("CANCEL_DEBUG", "🗑️ PDF eliminado: $pdfEliminado")

            // Eliminar de la base de datos
            if (constanciaId != -1) {
                val db = DatabaseHelper.getInstance(this)
                val eliminado = db.eliminarConstancia(constanciaId)

                Log.d("CANCEL_DEBUG", "🗑️ Constancia eliminada de BD: $eliminado")
                Log.d("CANCEL_DEBUG", "📊 Total constancias DESPUÉS: ${db.contarTotalConstancias()}")

                if (eliminado) {
                    // Decrementar el contador
                    decrementarContador()
                    Toast.makeText(this, "Constancia cancelada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al cancelar constancia", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("CANCEL_DEBUG", "⚠️ ID es -1, no hay nada que eliminar")
                Toast.makeText(this, "PDF descartado", Toast.LENGTH_SHORT).show()
            }

            finish()
        } catch (e: Exception) {
            Log.e("CANCEL_DEBUG", "❌ Error: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔧 NUEVA FUNCIÓN PARA DECREMENTAR CONTADOR
    private fun decrementarContador() {
        try {
            val db = DatabaseHelper.getInstance(this)
            val dbWritable = db.writableDatabase

            // Obtener el contador actual
            val cursor = dbWritable.rawQuery(
                "SELECT contador FROM oficio WHERE id = 1",
                null
            )

            if (cursor.moveToFirst()) {
                val contadorActual = cursor.getInt(0)
                val nuevoContador = if (contadorActual > 1) contadorActual - 1 else 1

                // Actualizar el contador
                val values = ContentValues().apply {
                    put("contador", nuevoContador)
                }
                dbWritable.update("oficio", values, "id = ?", arrayOf("1"))

                Log.d("CONTADOR_DEBUG", "⬇️ Contador decrementado de $contadorActual a $nuevoContador")
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("CONTADOR_DEBUG", "❌ Error al decrementar contador: ${e.message}")
        }
    }

    private fun descargarPDF() {
        try {
            // Cerrar el renderer antes de copiar el archivo
            currentPage?.close()
            pdfRenderer?.close()
            parcelFileDescriptor?.close()

            val noControl = intent.getStringExtra("NO_CONTROL") ?: "Unknown"
            val fileName = "Constancia_${noControl}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) - Usar MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(pdfFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Eliminar temporal
                    pdfFile?.delete()

                    Toast.makeText(this, "PDF descargado en: Descargas/$fileName", Toast.LENGTH_LONG).show()

                    // Abrir el PDF descargado
                    abrirPDFDescargado(uri)

                    finish()
                } else {
                    Toast.makeText(this, "Error al guardar PDF", Toast.LENGTH_SHORT).show()
                }

            } else {
                // Android 9 y anteriores - Guardar directamente en Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val finalFile = File(downloadsDir, fileName)

                FileInputStream(pdfFile).use { input ->
                    FileOutputStream(finalFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Notificar al sistema sobre el nuevo archivo
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.addCompletedDownload(
                    fileName,
                    "Constancia PDF",
                    true,
                    "application/pdf",
                    finalFile.absolutePath,
                    finalFile.length(),
                    true
                )

                // Eliminar temporal
                pdfFile?.delete()

                Toast.makeText(this, "PDF descargado en: Descargas/$fileName", Toast.LENGTH_LONG).show()

                // Abrir con FileProvider
                val uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    finalFile
                )
                abrirPDFDescargado(uri)

                finish()
            }

        } catch (e: Exception) {
            Log.e("PDFPreview", "Error al descargar PDF", e)
            Toast.makeText(this, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun compartirPDF() {
        try {
            // Cerrar el renderer antes de compartir
            currentPage?.close()
            pdfRenderer?.close()
            parcelFileDescriptor?.close()

            val noControl = intent.getStringExtra("NO_CONTROL") ?: "Unknown"
            val nombre = intent.getStringExtra("NOMBRE") ?: ""
            val fileName = "Constancia_${noControl}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

            // Primero guardar en Downloads
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(pdfFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Eliminar temporal
                    pdfFile?.delete()

                    // Compartir usando la URI de MediaStore
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Constancia - $nombre")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
                    finish()
                }
            } else {
                // Android 9 y anteriores
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val finalFile = File(downloadsDir, fileName)

                FileInputStream(pdfFile).use { input ->
                    FileOutputStream(finalFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Eliminar temporal
                pdfFile?.delete()

                val pdfUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    finalFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Constancia - $nombre")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
                finish()
            }

        } catch (e: Exception) {
            Log.e("PDFPreview", "Error al compartir PDF", e)
            Toast.makeText(this, "Error al compartir: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun abrirPDFDescargado(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("PDFPreview", "Error al abrir PDF", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            currentPage?.close()
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        } catch (e: IllegalStateException) {
            // Ya está cerrado, ignorar
            Log.d("PDFPreview", "Renderer ya cerrado")
        }
    }
}