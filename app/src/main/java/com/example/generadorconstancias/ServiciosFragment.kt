package com.example.generadorconstancias

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.card.MaterialCardView

class ServiciosFragment : Fragment() {

    private lateinit var syncManager: SyncManager
    private lateinit var dbHelper: DatabaseHelper

    // ViewModel compartido para actualizar otros fragments
    private val viewModel: ConstanciasViewModel by activityViewModels()

    private lateinit var btnSubir: Button
    private lateinit var btnDescargar: Button
    private lateinit var btnSincronizar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstado: TextView
    private lateinit var tvEstadisticas: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_servicios, container, false)

        syncManager = SyncManager(requireContext())
        dbHelper = DatabaseHelper.getInstance(requireContext())

        initViews(view)
        setupListeners()
        actualizarEstadisticas()

        return view
    }

    private fun initViews(view: View) {
        btnSubir = view.findViewById(R.id.btnSubir)
        btnDescargar = view.findViewById(R.id.btnDescargar)
        btnSincronizar = view.findViewById(R.id.btnSincronizar)
        progressBar = view.findViewById(R.id.progressBar)
        tvEstado = view.findViewById(R.id.tvEstado)
        tvEstadisticas = view.findViewById(R.id.tvEstadisticas)
    }

    private fun setupListeners() {
        btnSubir.setOnClickListener {
            mostrarConfirmacionSubida()
        }

        btnDescargar.setOnClickListener {
            mostrarConfirmacionDescarga()
        }

        btnSincronizar.setOnClickListener {
            sincronizarBidireccional()
        }
    }

    // ==================== SUBIR DATOS ====================

    private fun mostrarConfirmacionSubida() {
        val totalConstancias = dbHelper.contarTotalConstancias()

        if (totalConstancias == 0) {
            Toast.makeText(context, "No hay constancias para subir", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Subir datos al servidor")
            .setMessage("¿Deseas subir $totalConstancias constancias al servidor?\n\nEsto sobrescribirá los datos en el servidor.")
            .setPositiveButton("Subir") { _, _ ->
                subirDatos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun subirDatos() {
        mostrarCargando(true, "Subiendo datos...")

        syncManager.subirConstancias(
            onSuccess = { mensaje, cantidad ->
                mostrarCargando(false)
                tvEstado.text = "✅ $mensaje"
                Toast.makeText(context, "✅ $cantidad constancias subidas", Toast.LENGTH_LONG).show()
                actualizarEstadisticas()

                // 👇 Notificar al ViewModel que hubo cambios
                viewModel.cargarHistorial()
            },
            onError = { error ->
                mostrarCargando(false)
                tvEstado.text = "❌ Error: $error"
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ==================== DESCARGAR DATOS ====================

    private fun mostrarConfirmacionDescarga() {
        AlertDialog.Builder(requireContext())
            .setTitle("Descargar datos del servidor")
            .setMessage("¿Cómo deseas descargar los datos?\n\n• Sobrescribir: Elimina datos locales y descarga del servidor\n• Combinar: Agrega datos del servidor sin eliminar locales")
            .setPositiveButton("Sobrescribir") { _, _ ->
                descargarDatos(sobrescribir = true)
            }
            .setNeutralButton("Combinar") { _, _ ->
                descargarDatos(sobrescribir = false)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun descargarDatos(sobrescribir: Boolean) {
        val mensaje = if (sobrescribir) "Descargando y sobrescribiendo..." else "Descargando y combinando..."
        mostrarCargando(true, mensaje)

        syncManager.descargarConstancias(
            sobrescribirLocal = sobrescribir,
            onSuccess = { msg, cantidad ->
                mostrarCargando(false)
                tvEstado.text = "✅ $msg"
                Toast.makeText(context, "✅ $cantidad constancias descargadas", Toast.LENGTH_LONG).show()
                actualizarEstadisticas()

                // 👇 Notificar al ViewModel que hubo cambios
                viewModel.cargarHistorial()
            },
            onError = { error ->
                mostrarCargando(false)
                tvEstado.text = "❌ Error: $error"
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ==================== SINCRONIZACIÓN BIDIRECCIONAL ====================

    private fun sincronizarBidireccional() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sincronización completa")
            .setMessage("Esto subirá tus datos locales al servidor y luego descargará los datos del servidor.\n\n¿Continuar?")
            .setPositiveButton("Sincronizar") { _, _ ->
                ejecutarSincronizacion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarSincronizacion() {
        mostrarCargando(true, "Sincronizando...")

        syncManager.sincronizarBidireccional(
            onSuccess = { mensaje ->
                mostrarCargando(false)
                tvEstado.text = "✅ $mensaje"
                Toast.makeText(context, "Sincronización completada", Toast.LENGTH_LONG).show()
                actualizarEstadisticas()

                // 👇 Notificar al ViewModel que hubo cambios
                viewModel.cargarHistorial()
            },
            onError = { error ->
                mostrarCargando(false)
                tvEstado.text = "❌ Error: $error"
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // ==================== UTILIDADES ====================

    private fun mostrarCargando(mostrar: Boolean, mensaje: String = "") {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        btnSubir.isEnabled = !mostrar
        btnDescargar.isEnabled = !mostrar
        btnSincronizar.isEnabled = !mostrar

        if (mostrar) {
            tvEstado.text = mensaje
        }
    }

    private fun actualizarEstadisticas() {
        val totalConstancias = dbHelper.contarTotalConstancias()
        val totalAlumnos = dbHelper.contarTotalAlumnos() // 👈 Agregar esto
        val estadisticas = dbHelper.obtenerEstadisticas()

        val textoEstadisticas = buildString {
            append("📊 Estadísticas locales:\n\n")
            append("Total de constancias: $totalConstancias\n")
            append("Total de alumnos: $totalAlumnos\n\n") // 👈 Agregar esto

            if (estadisticas.isNotEmpty()) {
                append("Por tipo:\n")
                estadisticas.forEach { (tipo, cantidad) ->
                    append("• $tipo: $cantidad\n")
                }
            }
        }

        tvEstadisticas.text = textoEstadisticas
    }
}