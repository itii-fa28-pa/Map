package fr.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MarkerAdapter(
    private val markers: List<Marker>,
    private val onValidate: (Marker) -> Unit,
    private val onReject: (Marker) -> Unit,
    private val onMap: (Marker) -> Unit
) : RecyclerView.Adapter<MarkerAdapter.MarkerViewHolder>() {

    inner class MarkerViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val title: TextView = view.findViewById(R.id.tvTitle)
        val description: TextView = view.findViewById(R.id.tvDescription)

        val validate: Button = view.findViewById(R.id.btnValidate)
        val reject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MarkerViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marker, parent, false)

        return MarkerViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MarkerViewHolder,
        position: Int
    ) {
        val marker = markers[position]

        holder.title.text = marker.title
        holder.description.text = marker.description

        holder.validate.setOnClickListener {
            onValidate(marker)
        }

        holder.reject.setOnClickListener {
            onReject(marker)
        }
    }

    override fun getItemCount(): Int {
        return markers.size
    }
}