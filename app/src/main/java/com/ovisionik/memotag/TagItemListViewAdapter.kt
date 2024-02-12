package com.ovisionik.memotag

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.ovisionik.memotag.data.TagItem

class TagItemListViewAdapter(
    val mContext: Context,
    val resource: Int,
    val value: ArrayList<TagItem>,

    ): ArrayAdapter<TagItem>(mContext, resource, value) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val itemtag = value[position]
        val itemtagview = LayoutInflater.from(mContext).inflate(resource, parent, false)

        val lv_id = itemtagview.findViewById<TextView>(R.id.lv_item_tag_id)
        val lv_label = itemtagview.findViewById<TextView>(R.id.lv_item_tag_Label)
        val lv_barcode = itemtagview.findViewById<TextView>(R.id.lv_item_tag_barcode)
        val lv_price = itemtagview.findViewById<TextView>(R.id.lv_item_tag_price)
        val lv_createdOn = itemtagview.findViewById<TextView>(R.id.lv_item_tag_created_on)


        lv_id.text = itemtag.id.toString()
        lv_label.text = itemtag.label
        lv_barcode.text = itemtag.barcode.toString()
        lv_price.text = itemtag.price.toString() + "€"
        lv_createdOn.text = itemtag.createdOn

        return itemtagview
    }
}