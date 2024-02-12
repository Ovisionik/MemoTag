package com.ovisionik.memotag

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.set
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import java.math.RoundingMode
import java.text.DecimalFormat

class ItemTagViewActivity : AppCompatActivity() {

    private lateinit var mItemTag: ItemTag

    private lateinit var db : DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_tag_view)

        //init databe
        db = DatabaseHelper(this)

        //Get extra tag id
        val iID = intent.getIntExtra("itemID", -1)

        //Get item tag
        val itemTag = db.findItemTagByID(iID)

        if (itemTag == null)
        {
            Toast.makeText(this, "no such item exists", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mItemTag = itemTag

        //Get fields
        val tv_barcode = findViewById<TextView>(R.id.tv_barcode)
        val tv_price_tags_label = findViewById<TextView>(R.id.tv_PriceTags_label)
        val tv_defaultPrice = findViewById<TextView>(R.id.tv_default_price)

        val te_label = findViewById<TextView>(R.id.editText_label)
        val lv_price_tags = findViewById<ListView>(R.id.price_tags_lv)

        //btns
        val btn_save = findViewById<Button>(R.id.btn_save)
        val btn_close = findViewById<Button>(R.id.btn_close)

        tv_barcode.text = mItemTag.barcode
        te_label.text = mItemTag.label
        tv_defaultPrice.text = mItemTag.defaultPrice.toString()

        btn_save.setOnClickListener {
            Toast.makeText(this, "Not yet set", Toast.LENGTH_SHORT).show()
        }

        btn_close.setOnClickListener {
            finish()
        }
        //TODO Tag Prices List view adapter
    }
}