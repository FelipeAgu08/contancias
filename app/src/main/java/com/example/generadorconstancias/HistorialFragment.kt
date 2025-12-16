package com.example.generadorconstancias

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.generadorconstancias.databinding.FragmentHistorialBinding

class FragmentHistorial : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())
        // Agregar margen superior programáticamente
        val params = binding.rvHistorial.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = 100   // ← 32dp aprox (puedes subirlo a 48 o 64)
        binding.rvHistorial.layoutParams = params
        mostrarHistorialConstancias()
    }

    // 🔄 **Cada vez que vuelves al historial, se refresca**
    override fun onResume() {
        super.onResume()
        mostrarHistorialConstancias()
    }

    // 🔄 **Función pública, puede llamarse desde otros fragments**
    fun actualizarHistorial() {
        mostrarHistorialConstancias()
    }

    private fun mostrarHistorialConstancias() {
        val constancias = dbHelper.obtenerTodasConstancias()

        if (constancias.isEmpty()) {
            binding.txtMensaje.visibility = View.VISIBLE
            binding.rvHistorial.visibility = View.GONE
            binding.txtMensaje.text = "No hay constancias guardadas"
            return
        }

        binding.txtMensaje.visibility = View.GONE
        binding.rvHistorial.visibility = View.VISIBLE

        binding.rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorial.adapter = HistorialAdapter(constancias) { constancia ->
            mostrarDetalleConstancia(constancia)
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
Oficio: ${constancia.numeroOficio}
Fecha: ${constancia.fecha}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("📄 Detalle de Constancia")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .setNegativeButton("Eliminar") { _, _ ->
                autenticarAntesDeEliminar(constancia)
            }
            .show()
    }

    // 🔐 MÉTODO DE AUTENTICACIÓN BIOMÉTRICA + PIN
    private fun autenticarAntesDeEliminar(constancia: Constancia) {

        val executor = ContextCompat.getMainExecutor(requireContext())

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    // Eliminamos después de autenticar correctamente
                    if (dbHelper.eliminarConstancia(constancia.id)) {
                        Toast.makeText(requireContext(), "Constancia eliminada", Toast.LENGTH_SHORT).show()
                        mostrarHistorialConstancias()
                    } else {
                        Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Autenticación falló", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirmar eliminación")
            .setSubtitle("Autenticación requerida")
            // IMPORTANTE: NO usar botón negativo si activas PIN/huella del sistema
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun eliminarConstancia(constancia: Constancia) {
        if (dbHelper.eliminarConstancia(constancia.id)) {
            Toast.makeText(requireContext(), "Constancia eliminada", Toast.LENGTH_SHORT).show()
            mostrarHistorialConstancias()
        } else {
            Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
