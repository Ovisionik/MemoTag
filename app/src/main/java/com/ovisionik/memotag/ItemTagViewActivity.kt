package com.ovisionik.memotag

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class ItemTagViewActivity : AppCompatActivity() {

    var barcode:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_tag_view)

        val lvlabel = findViewById<TextView>(R.id.textView2)

        barcode = intent.getStringExtra("barcode")
        val itemTitle = intent.getStringExtra("title")
        lvlabel.text = itemTitle

    }
}