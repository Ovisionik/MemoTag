package com.ovisionik.memotag

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import java.time.LocalDate

/**
 * Create tag item activity :
 * Edit an empty forum and save the TagItem in the DB
 */
class CreateItemTagActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_item_tag)

        db = DatabaseHelper(this)

        // Get Extra
        val barcodeExtra = intent.getStringExtra("itemCode")

        val iv_addPicture = findViewById<ImageView>(R.id.imgViewAddPicture)

        val barcodeTV = findViewById<TextView>(R.id.tv_barcode)
        val quickNoteTV = findViewById<TextView>(R.id.acit_QuickNote_tv)

        //Get the btn
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        val titleET = findViewById<TextView>(R.id.editText_label)
        val quickNoteET = findViewById<TextView>(R.id.acit_note_EditText)
        val quickPriceET = findViewById<TextView>(R.id.acit_price_EditText)








        barcodeTV.text = barcodeExtra?.ifEmpty { "Unknown" } ?: "null"

        //Set barcode text view
        barcodeTV.text =  barcodeExtra

        iv_addPicture.setOnClickListener{
            //take picture
            //TODO

        }

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

            //Check the price format
            if (!quickPriceET.text.isNullOrEmpty()) {
                price = quickPriceET.text.toString().toDouble()
            }

            //If everything is a ok create the tag item
            val itemTag = ItemTag(barcode, label, price, createdOn)

            //Add and check if it's saved in the db
            val isInserted = db.insertItemTag(itemTag)

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

    private fun decodeBitmapFromByteArray(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

}