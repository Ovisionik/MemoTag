package com.ovisionik.memotag

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/**
 * Create tag item activity :
 * Edit an empty forum and save the TagItem in the DB
 */
class CreateItemTagActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private lateinit var iv_AddPicture: ImageView

    private var tmpByteArray: ByteArray = ByteArray(0)


    private val picPreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){bmp ->

        if (bmp != null){
            val tmpBitmap = bmp.copy(bmp.config, true);
            iv_AddPicture.setImageBitmap(tmpBitmap)
            tmpByteArray = tmpBitmap.toByteArray()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_item_tag)

        db = DatabaseHelper(this)

        tmpByteArray = ByteArray(0)

        // Get Extra
        val barcodeExtra = intent.getStringExtra("itemCode")
        val codeFormat = intent.getStringExtra("codeFormatName")


        iv_AddPicture = findViewById(R.id.imgViewAddPicture)

        val tv_barcodeTV = findViewById<TextView>(R.id.tv_barcode)
        val quickNoteTV = findViewById<TextView>(R.id.acit_QuickNote_tv)

        //Get the btn
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        val titleET = findViewById<TextView>(R.id.et_label)
        val quickNoteET = findViewById<TextView>(R.id.acit_note_EditText)
        val quickPriceET = findViewById<TextView>(R.id.acit_price_EditText)

        tv_barcodeTV.text = barcodeExtra?.ifEmpty { "Unknown" } ?: "null"

        //Set barcode text view
        tv_barcodeTV.text =  barcodeExtra

        iv_AddPicture.setOnClickListener{
            //contractTakePicture.launch(imageUri)
            picPreview.launch()
        }

        btnCancel.setOnClickListener{
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSave.setOnClickListener{

            //Get all the values from the forum
            val barcode:String = tv_barcodeTV.text.toString()
            val bcFormat = codeFormat.toString()
            val label:String = titleET.text.toString()
            var price: Double = 0.0
            val createdOn:String = LocalDate.now().toString()
            val byteArray = tmpByteArray

            //Check duplicates
            if (db.tagBarcodeExists(barcode)) {
                Toast.makeText(this, "Tag : $barcode is already in DB", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Check empty title/text/name/label

            //Check the price format
            if (!quickPriceET.text.isNullOrEmpty()) { price = quickPriceET.text.toString().toDouble() }

            //If everything is a ok create the tag item
            val itemTag = ItemTag(
                barcode,
                bcFormat,
                label,
                price,
                createdOn,
                imageByteArray = byteArray
            )

            //Add and check if it's saved in the db
            val isInserted = db.insertItemTag(itemTag)

            //Toast the user
            if (isInserted){
                Toast.makeText(this, getString(R.string.register_tag_success_message), Toast.LENGTH_SHORT).show()
                finish()
            }
            else{
                val err = db.addTagDEBUG(itemTag)

                Toast.makeText(this, getString(R.string.register_tag_failed_message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 0, stream)

        return stream.toByteArray()
    }
}