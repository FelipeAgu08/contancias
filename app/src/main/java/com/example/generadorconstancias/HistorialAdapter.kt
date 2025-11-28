package com.example.generadorconstancias

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistorialAdapter(
    private val constancias: List<Constancia>,
    private val onItemClick: (Constancia) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_constancia, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val constancia = constancias[position]
        holder.bind(constancia)

        // Click listener para ver detalles
        holder.itemView.setOnClickListener {
            onItemClick(constancia)
        }
    }

    override fun getItemCount(): Int = constancias.size

    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTipoConstancia: TextView = itemView.findViewById(R.id.tvTipoConstancia)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvNoControl: TextView = itemView.findViewById(R.id.tvNoControl)
        private val tvEspecialidad: TextView = itemView.findViewById(R.id.tvEspecialidad)
        private val tvGradoGrupo: TextView = itemView.findViewById(R.id.tvGradoGrupo)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvNumeroOficio: TextView = itemView.findViewById(R.id.tvNumeroOficio)

        fun bind(constancia: Constancia) {
            tvTipoConstancia.text = constancia.tipo
            tvNombre.text = constancia.nombre
            tvNoControl.text = "No. Control: ${constancia.noControl}"
            tvEspecialidad.text = "Especialidad: ${constancia.especialidad}"
            tvGradoGrupo.text = "Grado: ${constancia.grado}° Grupo: ${constancia.grupo}"
            tvFecha.text = formatearFecha(constancia.fecha)
            tvNumeroOficio.text = "Oficio: ${constancia.numeroOficio}"

            // Cambiar color según tipo de constancia
            when (constancia.tipo.lowercase()) {
                "estudios" -> tvTipoConstancia.setTextColor(0xFF1976D2.toInt()) // Azul
                "buena conducta" -> tvTipoConstancia.setTextColor(0xFF388E3C.toInt()) // Verde
                "promedio" -> tvTipoConstancia.setTextColor(0xFFF57C00.toInt()) // Naranja
                else -> tvTipoConstancia.setTextColor(0xFF5E35B1.toInt()) // Púrpura
            }
        }

        private fun formatearFecha(fecha: String): String {
            return try {
                // Si la fecha viene en formato ISO (2024-11-16 23:55:09)
                val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = formatoEntrada.parse(fecha)
                formatoSalida.format(date ?: Date())
            } catch (e: Exception) {
                // Si ya viene en otro formato, retornar tal cual
                fecha
            }
        }
    }
}