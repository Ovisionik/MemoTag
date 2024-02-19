package com.ovisionik.memotag

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import java.io.ByteArrayOutputStream


/**
 * Create tag item activity :
 * Edit an empty forum and save the TagItem in the DB
 */
class CreateItemTagActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private lateinit var iv_AddPicture: ImageView

    private var tmpByteArray: ByteArray = ByteArray(0)

    private val picPreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()
    ){
        bmp ->
        if (bmp != null){
            val resizedBmp = bmp.removeXPercent(0.3,0.3)
            iv_AddPicture.setImageBitmap(resizedBmp)
            tmpByteArray = resizedBmp.toByteArray()
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

        val et_title = findViewById<EditText>(R.id.et_label)
        val et_quickNote = findViewById<EditText>(R.id.acit_note_EditText)
        val et_quickPrice = findViewById<EditText>(R.id.acit_price_EditText)

        tv_barcodeTV.text = barcodeExtra?.ifEmpty { "Unknown" } ?: "null"

        //Set barcode text view
        tv_barcodeTV.text =  barcodeExtra

        iv_AddPicture.setOnClickListener{
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
            val label:String = et_title.text.toString()
            var price: Double = 0.0
            val byteArray = tmpByteArray

            //Check duplicates
            if (db.tagBarcodeExists(barcode)) {
                Toast.makeText(this, "Tag : $barcode is already in DB", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Check empty title/text/name/label

            //Check the price format
            if (!et_quickPrice.text.isNullOrEmpty()) { price = et_quickPrice.text.toString().toDouble() }

            //If everything is a ok create the tag item
            val itemTag = ItemTag(
                barcode = barcode,
                barcodeFormat =  bcFormat,
                label =  label,
                defaultPrice = price,
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
                Toast.makeText(this, getString(R.string.register_tag_failed_message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }

    /**
     * Resize a bitmap to remove x% of it's width/height from the sides
     * ie i want to crop/reduce the height by 20% -> height: 0.2
     * double must be between 0.0 and 1.0
     */
    fun Bitmap.removeXPercent(width: Double, height: Double): Bitmap {

        if (width > 1 || height > 1)
        {
            return this
        }

        //input = % to remove, not to keep, so let's get how much we need to cut

        val keepWidth = 1.0 - width

        val keepHeight = 1.0 - height

        val desPxW = this.width * keepWidth
        val desPxH = this.height * keepHeight

        val midW = this.width/2 - desPxW/2
        val midH = this.height/2 - desPxH/2

        return Bitmap.createBitmap(this, midW.toInt(), midH.toInt(), desPxW.toInt(), desPxH.toInt())
    }

}