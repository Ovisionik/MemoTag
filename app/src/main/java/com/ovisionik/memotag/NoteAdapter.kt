package com.ovisionik.memotag.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ovisionik.memotag.R
import com.ovisionik.memotag.data.NoteTag
import java.math.BigDecimal

class NoteAdapter(private val noteList: List<NoteTag>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteTextView: TextView = view.findViewById(R.id.et_add_note)
        val priceTextView: TextView = view.findViewById(R.id.et_add_price_note)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_list_view_item_model, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]
        holder.noteTextView.text = note.note
        holder.priceTextView.text = note.price.toString()


        val cover     = holder.itemView.findViewById<RelativeLayout>(R.id.add_note_field_cover)
        val content   = holder.itemView.findViewById<RelativeLayout>(R.id.add_note_content)
        val hasContent = note.note.isNotBlank() || note.price != BigDecimal(0)

        if(hasContent){
            content.visibility = RelativeLayout.VISIBLE
            cover.visibility = RelativeLayout.GONE
        }else{
            cover.visibility = RelativeLayout.VISIBLE
            content.visibility = RelativeLayout.GONE
        }

        holder.itemView.setOnClickListener {
            //If it has no value add new
            if (!hasContent)
            {
                cover.visibility = RelativeLayout.GONE
                content.visibility = RelativeLayout.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = noteList.size
}