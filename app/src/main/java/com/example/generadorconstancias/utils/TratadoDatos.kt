package com.example.generadorconstancias.utils


class TratadoDatos {

    fun obtenerNombre(texto: String): String {
        return texto.substringAfter("ALUMNO").substringBefore("CURP").replace("\n", " ")
    }

    fun obtenerNoControl(texto: String): String {
        return texto.substringAfter("CONTROL").substring(0,14)
    }

    fun obtenerEspecialidad(texto: String): String {
        val regex = Regex("GRUPO:[^\n]*\n([A-ZÁÉÍÓÚÑ ]+)\\s+ESPECIALIDAD", RegexOption.DOT_MATCHES_ALL)
        return regex.find(texto)?.groupValues?.get(1)?.trim() ?: "Especialidad no encontrada"
    }

    fun obtenerCurp(texto: String): String {
        val rawCurp = texto.substringAfter("CURP").substringBefore("GRADO").replace("\n", "").trim()
        val curpChars = rawCurp.toCharArray()

        // Correcciones comunes en la parte numérica de la CURP (posiciones 4 a 9)
        for (i in 4..9) {
            when (curpChars.getOrNull(i)) {
                'O', 'D' -> curpChars[i] = '0'
                'I', 'L' -> curpChars[i] = '1'
                'Z' -> curpChars[i] = '2'
                'S' -> curpChars[i] = '5'
                'B' -> curpChars[i] = '8'
            }
        }

        return String(curpChars)
    }


    fun obtenerGrado(texto: String): String {
        val regex = Regex("GRADO:?\\s*(\\d+)")
        return regex.find(texto)?.groupValues?.get(1)?.trim() ?: "Grado no encontrado"
    }

    fun obtenerGrupo(texto: String): String {
        val regex = Regex("GRUPO:?\\s*([A-Z])")
        return regex.find(texto)?.groupValues?.get(1)?.trim() ?: "Grupo no encontrado"
    }

}