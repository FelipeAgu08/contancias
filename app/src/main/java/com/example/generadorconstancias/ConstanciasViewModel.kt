package com.example.generadorconstancias

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ConstanciasViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper: DatabaseHelper = DatabaseHelper.getInstance(application)

    private val _historial = MutableLiveData<List<Constancia>>()
    val historial: LiveData<List<Constancia>> get() = _historial

    fun actualizarHistorial(nuevoHistorial: List<Constancia>) {
        _historial.value = nuevoHistorial
    }

    fun cargarHistorial() {
        // Cargar todas las constancias de la base de datos
        val constancias = dbHelper.obtenerTodasConstancias()
        _historial.value = constancias
    }
}