package com.example.generadorconstancias

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var imgHuella: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        imgHuella = findViewById(R.id.imgHuella)

        // Inicializar todo lo relacionado a autenticación biométrica
        initBiometricPrompt()

        // Ejecutar autenticación automáticamente al iniciar
        authenticateUser()

        // O permitir que el usuario vuelva a intentar manualmente
        imgHuella.setOnClickListener { authenticateUser() }
    }

    /**
     * Inicializa el prompt biométrico y su callback
     */
    private fun initBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                // 🟢 Huella correcta o autenticación por PIN/PATRÓN
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    msg("Autenticación exitosa ✔")

                    // Ir a Home
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                }

                // 🔴 Error fatal (hardware no disponible, cancelación por sistema, etc.)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    // Si el usuario presiona "Cancelar" no cerramos la app
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {

                        msg("Autenticación cancelada")
                        return
                    }

                    // Otros errores graves
                    msg("Error: $errString")
                }

                // 🔄 Huella no coincide
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    msg("Huella no reconocida ❌")
                }
            })

        // Prompt moderno con fallback a PIN/PATRÓN
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación requerida")
            .setSubtitle("Usa huella o PIN/PATRÓN para acceder")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    /**
     * Lanza autenticación solo si el dispositivo lo soporta
     */
    private fun authenticateUser() {
        val biometric = BiometricManager.from(this)

        when (biometric.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {

            BiometricManager.BIOMETRIC_SUCCESS ->
                biometricPrompt.authenticate(promptInfo)

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                msg("Este dispositivo no tiene sensor biométrico")

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                msg("El sensor biométrico no está disponible")

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                msg("No hay huellas ni PIN/PATRÓN configurado en el dispositivo")
        }
    }

    /**
     * Función helper para toasts
     */
    private fun msg(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
