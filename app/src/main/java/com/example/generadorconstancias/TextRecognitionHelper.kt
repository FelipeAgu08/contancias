package com.example.generadorconstancias

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

object TextRecognitionHelper {
    //procesar imagen en tiempo real
    fun processImage(image: InputImage): Task<Text> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return recognizer.process(image)
    }
    //procesar imagen de galeria o capturada
    fun recognizeTextFromUri(context: Context, uri: Uri): Task<Text> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
        } catch (e: IOException) {
            throw e
        }
    }
}
