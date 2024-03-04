package com.ovisionik.memotag

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper

class RvAdapter(private var items: List<ItemTag>) : RecyclerView.Adapter<RvAdapter.ViewHolder>(), Filterable {

    var filteredTags: ArrayList<ItemTag> = ArrayList()
    init {
        filteredTags = items as ArrayList<ItemTag>
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.tag_list_view_item_model, parent, false)

        return ViewHolder(itemView)
    }

    override fun getItemCount() = filteredTags.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val mItemTag = filteredTags[position]
        val db = DatabaseHelper(holder.itemView.context)

        holder.itemView.setOnLongClickListener{
            Toast.makeText(it.context, "OnLongClickListener", Toast.LENGTH_SHORT).show()
            true
        }

        //Show option button
        holder.btnMoreOption.setOnClickListener{ view ->
            val popupMenu = PopupMenu(
                view.context,
                view,
                Gravity.NO_GRAVITY, )

            //Show icon (no idea why it's not showing by default)
            try {
                val method = popupMenu.menu.javaClass.getDeclaredMethod(
                    "setOptionalIconsVisible",
                    Boolean::class.javaPrimitiveType
                )
                method.isAccessible = true
                method.invoke(popupMenu.menu, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.inflate(R.menu.list_context_menu)
            popupMenu.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.delete_item -> {

                        if (db.deleteTag(mItemTag)){
                            filteredTags.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, filteredTags.size)
                        }
                        true
                    }
                    else -> true
                }
            }
            popupMenu.show()
        }

        //On item click logic
        holder.itemView.setOnClickListener{ view ->

            val context = view.context
            Intent(context, EditTagActivity::class.java).also {
                it.putExtra("itemID", mItemTag.id)
                context.startActivity(it)
                notifyItemChanged(position)
                return@setOnClickListener
            }
        }
        holder.bindToView(mItemTag)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var tvTitle     : TextView
        private var tvID        : TextView
        private var tvBarcode   : TextView
        private var tvBrand     : TextView
        private var tvCategory  : TextView
        private var tvPrice     : TextView
        private var tvDate      : TextView
        private var ivTagImage  : ImageView

        var btnMoreOption : ImageView

        init {
            tvID            = itemView.findViewById(R.id.id_value)
            tvTitle         = itemView.findViewById(R.id.cv_label_value)
            tvBrand         = itemView.findViewById(R.id.brand_value)
            tvBarcode       = itemView.findViewById(R.id.barcode_value)
            tvCategory      = itemView.findViewById(R.id.category_value)
            tvPrice         = itemView.findViewById(R.id.cv_price_value)
            tvDate          = itemView.findViewById(R.id.cv_date_value)
            ivTagImage      = itemView.findViewById(R.id.cv_tag_image_value)
            btnMoreOption   = itemView.findViewById(R.id.cv_more_option)
        }

        fun bindToView(tag: ItemTag){
            tvID.text       = tag.id.toString()
            tvTitle.text    = tag.label
            tvBrand.text    = tag.brand
            tvBarcode.text  = tag.barcode
            tvCategory.text = tag.category
            tvPrice.text    = tag.moneyString()
            tvDate.text     = tag.createdOn
            ivTagImage.setImageBitmap(BitmapFactory.decodeByteArray(tag.imageByteArray, 0, tag.imageByteArray.size))
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val text = constraint.toString().lowercase()

                filteredTags = items as ArrayList<ItemTag>

                val filterResults = FilterResults()

                if (text.isEmpty() || text.length < 2){
                    filterResults.values = filteredTags
                    return filterResults
                }

                val result = items.filter { filter ->
                    filter.label.lowercase().contains(text)
                            || filter.barcode.lowercase().contains(text)
                }

                filteredTags = ArrayList(result)
                filterResults.values = filteredTags
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredTags = results?.values as ArrayList<ItemTag>
                notifyDataSetChanged()
            }
        }
    }
}
