package com.example.generadorconstancias

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityUtils {

    private const val KEY_ALIAS = "app_secure_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    /**
     * Genera o recupera la clave desde Android Keystore
     * (almacenamiento seguro respaldado por hardware)
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        // Si la clave ya existe, la retorna
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Si no existe, la crea
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encripta texto usando AES-GCM
     * Retorna: IV + TextoEncriptado (en Base64)
     */
    fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        // Encriptar
        val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

        // Obtener el IV generado automáticamente
        val iv = cipher.iv

        // Combinar IV + datos encriptados
        val combined = iv + encryptedBytes

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Desencripta texto usando AES-GCM
     * Espera: IV + TextoEncriptado (en Base64)
     */
    fun decrypt(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

        // Extraer IV (primeros 12 bytes para GCM)
        val iv = combined.copyOfRange(0, 12)
        val encryptedBytes = combined.copyOfRange(12, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Opcional: Eliminar la clave (útil para logout o reset)
     */
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        keyStore.deleteEntry(KEY_ALIAS)
    }

    // ============================================
    // 🔐 CÓDIGO DE VERIFICACIÓN PARA CONSTANCIAS
    // ============================================

    /**
     * Genera un código de verificación único basado en:
     * - Número de folio
     * - Nombre del alumno
     * - Número de control
     * - Fecha
     *
     * Retorna un código encriptado en formato compacto
     */
    fun generarCodigoVerificacion(
        folio: Int,
        nombre: String,
        noControl: String,
        fecha: String
    ): String {
        // Crear cadena con los datos concatenados
        val datosOriginales = "$folio|$nombre|$noControl|$fecha"

        // Encriptar los datos
        val datosEncriptados = encrypt(datosOriginales)

        // Opcional: Acortar el código para que sea más manejable
        // Tomamos solo los primeros 32 caracteres del Base64
        return datosEncriptados.take(32)
    }

    /**
     * Genera un código de verificación más corto usando HASH
     * Este es más compacto para mostrarlo en el PDF
     */
    fun generarCodigoVerificacionCompacto(
        folio: Int,
        nombre: String,
        noControl: String,
        fecha: String
    ): String {
        // Crear cadena con los datos
        val datosOriginales = "$folio|$nombre|$noControl|$fecha"

        // Generar hash SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(datosOriginales.toByteArray(Charsets.UTF_8))

        // Convertir a Base64 y tomar solo los primeros 16 caracteres
        val hashBase64 = Base64.encodeToString(hashBytes, Base64.NO_WRAP)

        return hashBase64.take(16).uppercase()
    }

    /**
     * Verifica si un código de verificación es válido
     * comparándolo con los datos proporcionados
     */
    fun verificarCodigo(
        codigo: String,
        folio: Int,
        nombre: String,
        noControl: String,
        fecha: String
    ): Boolean {
        val codigoGenerado = generarCodigoVerificacionCompacto(folio, nombre, noControl, fecha)
        return codigo.equals(codigoGenerado, ignoreCase = true)
    }

    /**
     * Genera un código QR-friendly (más largo pero recuperable)
     * Incluye toda la información encriptada
     */
    fun generarCodigoQR(
        folio: Int,
        nombre: String,
        noControl: String,
        fecha: String,
        curp: String
    ): String {
        // Incluir más datos para el QR
        val datosCompletos = "$folio|$nombre|$noControl|$curp|$fecha"

        // Encriptar todo
        return encrypt(datosCompletos)
    }

    /**
     * Decodifica un código QR y retorna los datos originales
     */
    fun decodificarCodigoQR(codigoQR: String): Map<String, String>? {
        return try {
            val datosDesencriptados = decrypt(codigoQR)
            val partes = datosDesencriptados.split("|")

            if (partes.size >= 5) {
                mapOf(
                    "folio" to partes[0],
                    "nombre" to partes[1],
                    "noControl" to partes[2],
                    "curp" to partes[3],
                    "fecha" to partes[4]
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SECURITY", "Error al decodificar QR: ${e.message}")
            null
        }
    }

    /**
     * Genera un formato legible del código de verificación
     * Ejemplo: "A1B2-C3D4-E5F6-G7H8"
     */
    fun formatearCodigoLegible(codigo: String): String {
        return codigo
            .uppercase()
            .chunked(4)
            .joinToString("-")
    }

    /**
     * Genera un código con timestamp para evitar duplicados
     */
    fun generarCodigoConTimestamp(
        folio: Int,
        nombre: String,
        noControl: String,
        fecha: String
    ): String {
        val timestamp = System.currentTimeMillis()
        val datosOriginales = "$folio|$nombre|$noControl|$fecha|$timestamp"

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(datosOriginales.toByteArray(Charsets.UTF_8))
        val hashBase64 = Base64.encodeToString(hashBytes, Base64.NO_WRAP)

        // Formato: FOLIO-HASH(12chars)
        return "$folio-${hashBase64.take(12).uppercase()}"
    }
}