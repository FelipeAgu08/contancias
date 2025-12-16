package com.example.generadorconstancias

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class FragmentGraficas : Fragment() {

    private lateinit var db: DatabaseHelper
    private lateinit var spinner: Spinner
    private lateinit var tituloGrafica: TextView
    private lateinit var pieChart: PieChart
    private lateinit var horizontalBarChart: HorizontalBarChart
    private lateinit var barChart: BarChart

    // ViewModel compartido
    private val viewModel: ConstanciasViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_graficas, container, false)
        db = DatabaseHelper.getInstance(requireContext())

        spinner = view.findViewById(R.id.spinnerTipoGrafica)
        tituloGrafica = view.findViewById(R.id.tituloGrafica)
        pieChart = view.findViewById(R.id.pieChart)
        horizontalBarChart = view.findViewById(R.id.horizontalBarChart)
        barChart = view.findViewById(R.id.barChart)

        setupSpinner()
        observarCambiosEnHistorial()
        return view
    }

    // Observar cambios en el historial desde el ViewModel
    private fun observarCambiosEnHistorial() {
        viewModel.historial.observe(viewLifecycleOwner) { constancias ->
            // Cuando el historial cambia, actualizar la gráfica actual
            actualizarGraficaActual()
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar también cuando el fragment vuelve a estar visible
        actualizarGraficaActual()
    }


    // Método público para actualizar las gráficas desde otros fragments
    fun actualizarGraficas() {
        actualizarGraficaActual()
    }

    private fun actualizarGraficaActual() {
        // 🔧 Limpiar highlights antes de actualizar
        pieChart.highlightValues(null)
        barChart.highlightValues(null)
        horizontalBarChart.highlightValues(null)
        val posicionActual = spinner.selectedItemPosition
        when (posicionActual) {
            0 -> {
                pieChart.visibility = View.VISIBLE
                cargarPieChart(db.obtenerEstadisticas())
            }
            1 -> {
                pieChart.visibility = View.VISIBLE
                cargarPieChartPorAlumno()
            }
            2 -> {
                pieChart.visibility = View.VISIBLE
                cargarPieChart(db.obtenerConstanciasPorCarrera())
            }
            3 -> {
                barChart.visibility = View.VISIBLE
                cargarBarChartPorSemestreDinamico()
            }
            4 -> {
                barChart.visibility = View.VISIBLE
                cargarBarChartPorMes()
            }
            5 -> {
                barChart.visibility = View.VISIBLE
                cargarBarChartPorAno()
            }
        }
    }

    private fun setupSpinner() {
        val opciones = listOf(
            "Constancias por tipo",
            "Constancias por alumno", // AHORA ES GRÁFICA DE PASTEL
            "Constancias por carrera",
            "Constancias por semestre",
            "Constancias por mes",
            "Constancias por año"
        )

        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opciones)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 🔧 SOLUCIÓN: Limpiar highlights de TODAS las gráficas antes de cambiar
                pieChart.highlightValues(null)
                barChart.highlightValues(null)
                horizontalBarChart.highlightValues(null)
                // Ocultar todos
                pieChart.visibility = View.GONE
                horizontalBarChart.visibility = View.GONE
                barChart.visibility = View.GONE

                when (position) {
                    0 -> {
                        pieChart.visibility = View.VISIBLE
                        tituloGrafica.text = "Constancias por tipo"
                        cargarPieChart(db.obtenerEstadisticas())
                    }
                    1 -> {
                        pieChart.visibility = View.VISIBLE  // CAMBIADO A PIE CHART
                        tituloGrafica.text = "Constancias por alumno"
                        cargarPieChartPorAlumno()  // NUEVA FUNCIÓN
                    }
                    2 -> {
                        pieChart.visibility = View.VISIBLE
                        tituloGrafica.text = "Constancias por carrera"
                        cargarPieChart(db.obtenerConstanciasPorCarrera())
                    }
                    3 -> {
                        barChart.visibility = View.VISIBLE
                        tituloGrafica.text = "Constancias por semestre"
                        cargarBarChartPorSemestreDinamico()
                    }
                    4 -> {
                        barChart.visibility = View.VISIBLE
                        tituloGrafica.text = "Constancias por mes"
                        cargarBarChartPorMes()
                    }
                    5 -> {
                        barChart.visibility = View.VISIBLE
                        tituloGrafica.text = "Constancias por año"
                        cargarBarChartPorAno()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ==================== PieChart ====================
    private fun cargarPieChart(datos: Map<String, Int>) {
        val entries = datos.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 25f
        pieChart.setDrawCenterText(false) // 👈 Limpiar texto central
        pieChart.centerText = "" // 👈 Limpiar texto central
        pieChart.animateY(1000)
        pieChart.setOnChartValueSelectedListener(null)
        pieChart.setTouchEnabled(false) // Desactivar interacción
        pieChart.invalidate()
    }

    // ==================== NUEVA: PieChart por Alumno ====================
    private fun cargarPieChartPorAlumno() {
        android.util.Log.d("GRAFICA_DEBUG", "🔍 Iniciando carga de gráfica por alumno")

        try {
            val cursor = db.readableDatabase.rawQuery(
                "SELECT nombre, no_control, COUNT(*) AS total " +
                        "FROM constancias_detalle " +
                        "GROUP BY no_control " +
                        "ORDER BY total DESC",
                null
            )

            android.util.Log.d("GRAFICA_DEBUG", "📊 Registros encontrados: ${cursor.count}")

            val datos = mutableMapOf<String, Int>()
            val alumnoNoControlsMap = mutableMapOf<String, MutableList<String>>() // 🔧 NUEVO: Lista de controles por alumno

            if (cursor.moveToFirst()) {
                do {
                    val nombreEncriptado = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                    val noControl = cursor.getString(cursor.getColumnIndexOrThrow("no_control"))
                    val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))

                    android.util.Log.d("GRAFICA_DEBUG", "👤 Procesando: NoControl=$noControl, Total=$total")

                    // 🔓 Desencriptar el nombre antes de usar
                    val nombre = try {
                        SecurityUtils.decrypt(nombreEncriptado)
                    } catch (e: Exception) {
                        android.util.Log.e("GRAFICA_DEBUG", "❌ Error desencriptando: ${e.message}")
                        nombreEncriptado
                    }

                    // 🔧 SUMAR constancias en lugar de sobrescribir
                    datos[nombre] = (datos[nombre] ?: 0) + total

                    // Guardar todos los números de control del alumno
                    if (!alumnoNoControlsMap.containsKey(nombre)) {
                        alumnoNoControlsMap[nombre] = mutableListOf()
                    }
                    alumnoNoControlsMap[nombre]?.add(noControl)

                    android.util.Log.d("GRAFICA_DEBUG", "✅ Agregado/Sumado: $nombre = ${datos[nombre]}")

                } while (cursor.moveToNext())
            }
            cursor.close()

            android.util.Log.d("GRAFICA_DEBUG", "📈 Total de datos procesados: ${datos.size}")

            if (datos.isEmpty()) {
                android.util.Log.w("GRAFICA_DEBUG", "⚠️ No hay datos para mostrar")
                pieChart.clear()
                pieChart.setNoDataText("No hay constancias registradas")
                pieChart.setNoDataTextColor(Color.parseColor("#757575"))
                pieChart.invalidate()
                return
            }

            val entries = datos.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "")

            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() +
                    ColorTemplate.COLORFUL_COLORS.toList() +
                    ColorTemplate.JOYFUL_COLORS.toList()

            dataSet.valueTextSize = 11f
            dataSet.valueTextColor = Color.WHITE
            dataSet.sliceSpace = 2f

            val data = PieData(dataSet)

            pieChart.data = data
            pieChart.description.isEnabled = false
            pieChart.setUsePercentValues(true)
            pieChart.isDrawHoleEnabled = true
            pieChart.setHoleColor(Color.WHITE)
            pieChart.holeRadius = 35f
            pieChart.transparentCircleRadius = 40f
            pieChart.setDrawCenterText(true)
            pieChart.centerText = "Todos los\nAlumnos"
            pieChart.setCenterTextSize(14f)
            pieChart.setEntryLabelColor(Color.BLACK)
            pieChart.setEntryLabelTextSize(9f)
            pieChart.animateY(1400)
            pieChart.legend.isEnabled = true
            pieChart.legend.textSize = 10f
            pieChart.legend.setWordWrapEnabled(true)
            pieChart.setTouchEnabled(true)
            pieChart.highlightValues(null)
            pieChart.invalidate()

            android.util.Log.d("GRAFICA_DEBUG", "✅ Gráfica cargada exitosamente")

            // Listener para mostrar detalles al tocar
            pieChart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    if (e is PieEntry) {
                        val nombre = e.label

                        // 🔧 Obtener TODAS las constancias de TODOS los números de control del alumno
                        val noControls = alumnoNoControlsMap[nombre] ?: listOf()
                        val constancias = db.obtenerTodasConstancias().filter {
                            noControls.contains(it.noControl)
                        }

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            mostrarDialogDetalleAlumno(nombre, noControls, constancias)
                        }
                    }
                }
                override fun onNothingSelected() {}
            })

        } catch (e: Exception) {
            android.util.Log.e("GRAFICA_DEBUG", "❌ Error general: ${e.message}")
            e.printStackTrace()

            pieChart.clear()
            pieChart.setNoDataText("Error al cargar datos: ${e.message}")
            pieChart.setNoDataTextColor(Color.parseColor("#F44336"))
            pieChart.invalidate()
        }
    }

    // ==================== Dialog ACTUALIZADO para mostrar múltiples controles ====================
    private fun mostrarDialogDetalleAlumno(nombre: String, noControls: List<String>, constancias: List<Constancia>) {
        val builder = android.app.AlertDialog.Builder(requireContext())

        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }

        // Título con nombre del alumno
        val tvNombre = android.widget.TextView(requireContext()).apply {
            text = nombre
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#2979FF"))
            gravity = android.view.Gravity.CENTER
        }
        container.addView(tvNombre)

        // 🔧 Mostrar TODOS los números de control
        val tvNoControl = android.widget.TextView(requireContext()).apply {
            text = if (noControls.size == 1) {
                "No. Control: ${noControls[0]}"
            } else {
                "Números de Control:\n${noControls.joinToString("\n")}"
            }
            textSize = 14f
            setTextColor(Color.parseColor("#757575"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }
        container.addView(tvNoControl)

        // Separador
        val separador = android.view.View(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        container.addView(separador)

        // Total de constancias
        val total = constancias.size
        val tvTotal = android.widget.TextView(requireContext()).apply {
            text = "Total de constancias: $total"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#212121"))
            setPadding(0, 20, 0, 16)
        }
        container.addView(tvTotal)

        // Agrupar por tipo y mostrar
        val tiposAgrupados = constancias.groupBy { it.tipo }

        tiposAgrupados.forEach { (tipo, lista) ->
            val cardLayout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
            }

            val bullet = android.widget.TextView(requireContext()).apply {
                text = "●"
                textSize = 20f
                setTextColor(Color.parseColor("#4CAF50"))
                setPadding(0, 0, 12, 0)
            }
            cardLayout.addView(bullet)

            val contentLayout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val tvTipo = android.widget.TextView(requireContext()).apply {
                text = tipo
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#212121"))
            }
            contentLayout.addView(tvTipo)

            val tvCantidad = android.widget.TextView(requireContext()).apply {
                text = "Cantidad: ${lista.size}"
                textSize = 13f
                setTextColor(Color.parseColor("#757575"))
            }
            contentLayout.addView(tvCantidad)

            cardLayout.addView(contentLayout)

            val badge = android.widget.TextView(requireContext()).apply {
                text = lista.size.toString()
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#FFFFFF"))
                setBackgroundColor(Color.parseColor("#2979FF"))
                setPadding(16, 8, 16, 8)
                gravity = android.view.Gravity.CENTER
            }
            cardLayout.addView(badge)

            container.addView(cardLayout)
        }

        builder.setView(container)
        builder.setPositiveButton("Cerrar") { dialog, _ ->
            dialog.dismiss()
            pieChart.highlightValues(null)
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)
        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(Color.parseColor("#2979FF"))
            textSize = 16f
        }
    }


    // ==================== BarChart por Semestre dinámico ====================
    private fun cargarBarChartPorSemestreDinamico() {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT grado, COUNT(*) as total FROM constancias_detalle GROUP BY grado", null
        )
        val datos = mutableMapOf<String, Int>()
        if (cursor.moveToFirst()) {
            do {
                val grado = cursor.getString(cursor.getColumnIndexOrThrow("grado"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                datos[grado] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        cargarBarChart(datos)
    }

    private fun cargarBarChartPorMes() {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT strftime('%m-%Y', fecha_registro) as mes, COUNT(*) as total FROM constancias_detalle GROUP BY mes", null
        )
        val datos = mutableMapOf<String, Int>()
        if (cursor.moveToFirst()) {
            do {
                val mes = cursor.getString(cursor.getColumnIndexOrThrow("mes"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                datos[mes] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        cargarBarChart(datos)
    }

    private fun cargarBarChartPorAno() {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT strftime('%Y', fecha_registro) as ano, COUNT(*) as total FROM constancias_detalle GROUP BY ano", null
        )
        val datos = mutableMapOf<String, Int>()
        if (cursor.moveToFirst()) {
            do {
                val ano = cursor.getString(cursor.getColumnIndexOrThrow("ano"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                datos[ano] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        cargarBarChart(datos)
    }

    private fun cargarBarChart(datos: Map<String, Int>) {
        // 👇 Limpiar texto central del pieChart
        pieChart.setDrawCenterText(false)
        pieChart.centerText = ""

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f
        for ((key, value) in datos) {
            entries.add(BarEntry(index, value.toFloat()))
            labels.add(key)
            index += 1f
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
        dataSet.valueTextSize = 12f
        val data = BarData(dataSet)
        data.barWidth = 0.5f

        barChart.data = data
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.setFitBars(true)
        barChart.setOnChartValueSelectedListener(null)
        barChart.setTouchEnabled(false) // Desactivar interacción
        barChart.invalidate()
    }
}