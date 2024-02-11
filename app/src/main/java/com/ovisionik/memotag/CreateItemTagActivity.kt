package com.ovisionik.memotag

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.ovisionik.memotag.data.TagItem
import com.ovisionik.memotag.db.DatabaseHelper
import java.time.LocalDate

class CreateItemTagActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_item_tag)

        db = DatabaseHelper(this)

        // Get Extra
        val barcodeExtra = intent.getStringExtra("barcode")

        val barcodeTV = findViewById<TextView>(R.id.acit_Barcode_tv)
        val quickNoteTV = findViewById<TextView>(R.id.acit_QuickNote_tv)

        //Get the btn
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        val titleET = findViewById<TextView>(R.id.acit_label_EditText)
        val quickNoteET = findViewById<TextView>(R.id.acit_note_EditText)
        val quickPriceET = findViewById<TextView>(R.id.acit_price_EditText)


        barcodeTV.text = barcodeExtra?.ifEmpty { "Unknown" } ?: "null"

        //Set barcode text view
        barcodeTV.text =  barcodeExtra

        btnCancel.setOnClickListener{
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSave.setOnClickListener{

            //Get all the values from the forum
            val barcode:String = barcodeTV.text.toString()
            val label:String = titleET.text.toString()
            var price: Double = 0.0
            val createdOn:String = LocalDate.now().toString()

            //Check duplicates
            if (db.tagBarcodeExists(barcode)) {
                Toast.makeText(this, "Tag : $barcode is already in DB", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Check empty title/text/name/label
            if (label.isEmpty()){
                Toast.makeText(this, "Please give it a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Check the price format
            if (!quickPriceET.text.isNullOrEmpty()) {
                price = quickPriceET.text.toString().toDouble()
            }

            //If everything is a ok create the tag item
            var tagItem = TagItem(barcode, label, price, createdOn)

            //Add and check if it's saved in the db
            val isInserted = db.insertTag(tagItem)

            //Toast the user
            if (isInserted){
                Toast.makeText(this, getString(R.string.register_tag_success_message), Toast.LENGTH_SHORT).show()
                finish()
            }
            else{
                Toast.makeText(this, getString(R.string.register_tag_failed_message), Toast.LENGTH_SHORT).show()
            }

        }
    }
}