package com.ovisionik.memotag

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.Writer
import com.google.zxing.WriterException
import com.google.zxing.aztec.AztecWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.oned.EAN13Writer
import com.google.zxing.oned.EAN8Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import com.ovisionik.memotag.scraper.BarcodeScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat


class ViewItemTagActivity : AppCompatActivity() {

    var showBarcode: Boolean = false

    private lateinit var mItemTag: ItemTag
    private lateinit var db : DatabaseHelper

    lateinit var scraper       :BarcodeScraper

    //Views
    lateinit var iv_ImageDisplay        :ImageView
    lateinit var lv_price_tags          :ListView
    lateinit var et_label               :EditText
    lateinit var et_brand               :EditText
    lateinit var et_defaultPrice        :EditText
    lateinit var tv_price_tags_label    :TextView
    lateinit var tv_tag_date            :TextView
    lateinit var tv_barcode             :TextView

    //btn views
    lateinit var btnSave        :Button
    lateinit var btnClose       :Button
    lateinit var btnWebSearch   :Button

    private val picPreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ){
            bmp ->
        if (bmp != null){
            val resizedBmp = bmp.removeXPercent(0.3,0.3)
            iv_ImageDisplay.setImageBitmap(resizedBmp)
            mItemTag.imageByteArray = resizedBmp.toByteArray()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_tag_view)

        //init database
        db = DatabaseHelper(this)

        //Get a scraper
        scraper = BarcodeScraper()

        //Get extra tag id
        val iID = intent.getIntExtra("itemID", -1)
        val itemTag = db.findItemTagByID(iID)
        if (itemTag == null)
        {
            Toast.makeText(this, "no such item exists", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mItemTag = itemTag

        //Get fields
        tv_price_tags_label = findViewById(R.id.tv_PriceTags_label)
        et_label = findViewById(R.id.et_label)
        et_brand = findViewById(R.id.et_brand)
        et_defaultPrice = findViewById(R.id.et_default_price)
        tv_tag_date = findViewById(R.id.tv_tag_date)
        lv_price_tags = findViewById(R.id.price_tags_lv)
        iv_ImageDisplay = findViewById(R.id.iv_image)
        tv_barcode = findViewById(R.id.tv_barcode)

        //buttons
        btnSave = findViewById(R.id.btn_save)
        btnClose = findViewById(R.id.btn_close)
        btnWebSearch = findViewById(R.id.btn_web_scrap)

        //Set/Update view
        updateViews()


        iv_ImageDisplay.setOnClickListener{
            picPreview.launch()
        }

        //Toggle Image and Barcode view
        tv_barcode.setOnClickListener { toggleItemDisplay() }

        //Close
        btnClose.setOnClickListener { finish() }

        //Scrap product value from the web
        btnWebSearch.setOnClickListener{

            // Start a coroutine in the IO context
            lifecycleScope.launch(Dispatchers.IO){

                val result = async { scraper.asyncGetItemScrap(mItemTag.barcode) }.await()

                result.onFailure {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@ViewItemTagActivity, "${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                result.onSuccess { scrap ->
                    if (scrap == null)
                        return@launch

                    //Fill only if empty
                    if (mItemTag.label.isEmpty())       { mItemTag.label    = scrap.label       }
                    if (mItemTag.brand.isEmpty())       { mItemTag.brand    = scrap.brand       }
                    if (mItemTag.category.isEmpty())    { mItemTag.category = scrap.category    }
                    if (mItemTag.imageURL.isEmpty())    { mItemTag.imageURL = scrap.imageURL    }

                    async{
                        //Add store the image from url to the bitarray if it was empty
                        scraper.asyncGetBitmapFromURL(scrap.imageURL).onSuccess { bmp ->
                            if (bmp!=null && mItemTag.imageByteArray.isEmpty()){
                                mItemTag.imageByteArray = bmp.toByteArray()
                            }
                        }
                    }.await()

                    //Update views
                    updateViews()
                }
            }
        }

        //Save/Update the current view in the DB
        btnSave.setOnClickListener {

            //update_mTags
            if (et_label.text.isNotEmpty()){
                mItemTag.label = et_label.text.toString()
            }
            if (et_brand.text.isNotEmpty()){
                mItemTag.brand = et_brand.text.toString()
            }
            if (et_defaultPrice.text.isNotEmpty()){
                mItemTag.defaultPrice = et_defaultPrice.text.toString().toDouble()
            }

            db.updateTag(mItemTag)

            finish()
        }

        //TODO Tag Prices List view adapter
    }


    /**
     * Update all the binding for the visual elements
     */
    private fun updateViews() {

        lifecycleScope.launch(Dispatchers.Main) {
            //Item picture display
            if (mItemTag.imageByteArray.isNotEmpty() && !showBarcode){
                val bmp = mItemTag.imageByteArray.toBitmap()
                iv_ImageDisplay.setImageBitmap(bmp)
            }

            var toggleSymbol:String = "◀"
            if (showBarcode){ toggleSymbol = "⧋" }

            tv_barcode.text = mItemTag.barcode.plus(toggleSymbol)

            et_label.hint = mItemTag.label

            et_defaultPrice.hint = getPriceFormattedString(mItemTag.defaultPrice)

            tv_tag_date.hint = mItemTag.createdOn

            et_brand.hint = mItemTag.brand
        }
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }
    private fun toggleItemDisplay() {
        showBarcode = !showBarcode

        if (showBarcode){
            //Format
            val format = BarcodeFormat.valueOf(mItemTag.barcodeFormat)

            //Writer
            val writer: Writer = when (format){
                BarcodeFormat.AZTEC -> AztecWriter()
                BarcodeFormat.EAN_13 -> EAN13Writer()
                BarcodeFormat.EAN_8 -> EAN8Writer()
                BarcodeFormat.CODE_128 -> Code128Writer()
                BarcodeFormat.CODABAR -> Code128Writer()
                BarcodeFormat.QR_CODE -> QRCodeWriter()
                else -> {
                    Toast.makeText(this, "Sorry, can't recreate the barcode", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            if (mItemTag.barcode.isEmpty()){
                Toast.makeText(this, "Cant generate barcode", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val bitMatrix = writer.encode(mItemTag.barcode, format, 1024, 512)
                val width = bitMatrix.width
                val height = bitMatrix.height

                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width){
                    for (y in 0 until height){
                        bmp.setPixel(x,y, if (bitMatrix[x,y]) Color.BLACK else Color.WHITE)
                    }
                }
                if (showBarcode){ tv_barcode.text = mItemTag.barcode.plus( " ⧋") }
                iv_ImageDisplay.setImageBitmap(bmp)

            }catch (err : WriterException){
                Toast.makeText(this, "${err.message}", Toast.LENGTH_SHORT).show()
                err.printStackTrace()

                //Reset the view since we have nothing to show
                showBarcode = false
            }
        }
        else{
            updateViews()
        }
    }

    private fun getPriceFormattedString(price: Number): String {
        val df = DecimalFormat("#,###,##0.00")
        return df.format(price).plus(" €")
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

    fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, this.size)
    }
}