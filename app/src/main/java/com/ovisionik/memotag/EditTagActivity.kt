package com.ovisionik.memotag

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
import com.ovisionik.memotag.utils.AppNetwork.isOnline
import com.ovisionik.memotag.utils.BitmapUtils.compressTo1MIO
import com.ovisionik.memotag.utils.BitmapUtils.getBitmapFromUrlAsync
import com.ovisionik.memotag.utils.BitmapUtils.removeXPercent
import com.ovisionik.memotag.utils.BitmapUtils.toBitmap
import com.ovisionik.memotag.utils.BitmapUtils.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


class EditTagActivity : AppCompatActivity() {

    private var mDisplayBarcode: Boolean = false

    //Utils//
    private lateinit var db                 :DatabaseHelper
    private lateinit var scraper            :BarcodeScraper
    private lateinit var mItemTag           :ItemTag

    // 'normal' Views //
    private lateinit var ivImageDisplay     :ImageView
    private lateinit var ivBarcodeDisplay   :ImageView
    private lateinit var lvPriceTags        :ListView
    private lateinit var etLabel            :EditText
    private lateinit var etBrand            :EditText
    private lateinit var etDefaultPrice     :EditText
    private lateinit var tvTagCreationDate  :TextView
    private lateinit var tvBarcode          :TextView

    //button views//
    private lateinit var btnSave            :Button
    private lateinit var btnClose           :Button
    private lateinit var btnWebSearch       :ImageButton

    private var cameraIsBusy = false

    private fun initVar() {

        //--Views--//
        etLabel = findViewById(R.id.et_label)
        etBrand = findViewById(R.id.et_brand)
        tvBarcode = findViewById(R.id.tv_barcode)
        etDefaultPrice = findViewById(R.id.et_default_price)
        tvTagCreationDate = findViewById(R.id.tv_tag_date)
        lvPriceTags = findViewById(R.id.price_tags_lv)
        ivImageDisplay = findViewById(R.id.iv_image)
        ivBarcodeDisplay = findViewById(R.id.iv_barcode)

        //--Buttons views--//
        btnSave = findViewById(R.id.btn_save)
        btnClose = findViewById(R.id.btn_close)
        btnWebSearch = findViewById(R.id.btn_web_scrap)
    }

    private val picPreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ){ bmp ->
        cameraIsBusy = true

        if (bmp != null){
            val resizedBmp = bmp.removeXPercent(0.3,0.3)
            val bmp1 = resizedBmp.compressTo1MIO()
            ivImageDisplay.setImageBitmap(bmp1)
            mItemTag.imageByteArray = bmp1.toByteArray()
        }

        cameraIsBusy = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_item_tag)

        //get possible the extras
        val iID = intent.getIntExtra("itemID", -1)
        val barcode = intent.getStringExtra("itemCode")
        val codeFormat = intent.getStringExtra("codeFormatName")

        //init database
        db = DatabaseHelper(this)
        scraper = BarcodeScraper()
        mItemTag = ItemTag()

        if (iID != -1){
            //If it's not on the db found return
            val  itemTag = db.findItemTagByID(iID)

            //Found the item?
            if (itemTag != null){
                mItemTag = itemTag
            }else{
                //They got us good they gave a fake ID wp me (go fix the code)
                Log.wtf(
                    "Fake ID",
                    "This should not happened, intent.getIntExtra(\"itemID\", -1) returned an id that was not from db"
                )
                finish()
                return
            }
        }
        else{
            if (barcode.isNullOrBlank()){
                Toast.makeText(this, "Error no barcode to work with", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val itemTag = ItemTag()
            itemTag.barcode = barcode
            itemTag.barcodeFormat = codeFormat?: ""

            mItemTag = itemTag
        }

        //Initialize private hooks
        initVar()

        //Set/Update view
        updateViews()

        //Hide barcode & show image
        loadImageDisplay(mDisplayBarcode)

        ivImageDisplay.setOnClickListener{

            if (!cameraIsBusy){
                picPreview.launch()
            }
        }

        //Toggle Image and Barcode view
        tvBarcode.setOnClickListener { toggleItemDisplay() }

        //Copy the barcode on long-click
        tvBarcode.setOnLongClickListener{
            if (tvBarcode.text.isNotBlank()) {
                val clipboard: ClipboardManager =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(mItemTag.barcode, mItemTag.barcode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied: ${mItemTag.barcode}", Toast.LENGTH_SHORT).show()
            }
            true
        }

        //Close
        btnClose.setOnClickListener { finish() }

        //Scrap product value from the web
        btnWebSearch.setOnClickListener{

            if (isOnline(this)) {
                Toast.makeText(this, "Experimental", Toast.LENGTH_SHORT).show()
                webScrap()
            }else{
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        //Save/Update the current view in the DB
        btnSave.setOnClickListener {

            //update_mTags
            if (etLabel.text.isNotEmpty()){
                mItemTag.label = etLabel.text.toString()
            }
            if (etBrand.text.isNotEmpty()){
                mItemTag.brand = etBrand.text.toString()
            }
            if (etDefaultPrice.text.isNotEmpty()){
                mItemTag.defaultPrice = etDefaultPrice.text.toString().toDouble()
            }

            //If exists update else create
            if (db.tagExists(mItemTag)){
                db.updateTag(mItemTag)
            }
            else{
                db.insertItemTag(mItemTag)
            }

            super.onResume()
            finish()
        }

        //TODO Tag Prices List view adapter
    }

    /**
     * Update all the binding for the visual elements
     */
    private fun updateViews() {

        //barcode
        tvBarcode.text = mItemTag.barcode.plus(" " + if(mDisplayBarcode)"◈" else "❖")

        //label
        if (mItemTag.label.isNotBlank())
        {
            etLabel.hint = mItemTag.label
            etLabel.setText(etLabel.hint)
        }

        //brand
        if (mItemTag.brand.isNotBlank())
        {
            etBrand.hint = mItemTag.brand
            etBrand.setText(etBrand.hint)
        }

        //price
        etDefaultPrice.hint = mItemTag.moneyString()

        //date
        tvTagCreationDate.hint = mItemTag.createdOn
    }

    /**
     * load image for the image display
     *
     * loadBarcodeBitmap? load barcode image else load item image
     * returns true successful
     */
    private fun loadImageDisplay(loadBarcodeBitmap: Boolean): Boolean {

        //Show barcode image
        if (loadBarcodeBitmap){
            //create the images only if it's not cashed
            if (ivBarcodeDisplay.drawable == null){

                //barcode format
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
                        return false
                    }
                }

                //Nothing to gen
                if (mItemTag.barcode.isEmpty()){
                    Toast.makeText(this, "Cant generate barcode", Toast.LENGTH_SHORT).show()
                    return false
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
                    ivBarcodeDisplay.setImageBitmap(bmp)

                }catch (err : WriterException){
                    Toast.makeText(this@EditTagActivity, "${err.message}", Toast.LENGTH_SHORT).show()
                    err.printStackTrace()
                    //we have nothing to show
                    return false
                }
            }
        }
        else{
            //Show product image
            if (mItemTag.imageByteArray.isNotEmpty())
            {
                //From ByteArray
                ivImageDisplay.setImageBitmap(
                    mItemTag.imageByteArray.toBitmap())
            }else{
                //From Url


                //if there is still no image show the place holder
                ivImageDisplay.setImageResource(R.drawable.ic_add_a_photo)
            }
        }
        ivBarcodeDisplay.visibility = if (loadBarcodeBitmap) View.VISIBLE else View.INVISIBLE
        ivImageDisplay.visibility = if (loadBarcodeBitmap) View.INVISIBLE else View.VISIBLE
        return true
    }

    private fun toggleItemDisplay() {

        //!mDisplayBarcode because we are trying to toggle
        val success = loadImageDisplay(!mDisplayBarcode)
        if (success) {
            val toggleSymbolExpand = "❖"
            val toggleSymbolCollapse = "◈"
            tvBarcode.text = mItemTag.barcode.plus(" " + if(mDisplayBarcode) toggleSymbolExpand else toggleSymbolCollapse)

            //Toggle the boolean
            mDisplayBarcode = !mDisplayBarcode
        }else{
            //Couldn't load
            Toast.makeText(this, "Couldn't load the image", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun webScrap() {
        // Start a coroutine in the IO context
        lifecycleScope.launch(Dispatchers.IO) {
            val imgLnk = async { scraper.googleImageSearch(mItemTag.barcode) }.await()
            if (imgLnk.isNotEmpty()){
                async { getBitmapFromUrlAsync(imgLnk).onSuccess {
                    //compress it to at least 1mio
                    val cBmp = it.compressTo1MIO()
                    mItemTag.imageByteArray = cBmp.toByteArray()
                }}.await()
            }


            val result = async { scraper.getItemScrapAsync(mItemTag.barcode) }.await()

            result.onFailure {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@EditTagActivity, "${it.message}", Toast.LENGTH_SHORT).show()
                }
            }

            result.onSuccess { scrap ->
                //Fill only if blank(empty or white space)
                if (etLabel.text.isBlank())         { mItemTag.label    = scrap.label       }
                if (etBrand.text.isBlank())         { mItemTag.brand    = scrap.brand       }
                if (mItemTag.category.isBlank())    { mItemTag.category = scrap.category    }
                /*
                                    //if (mItemTag.imageURL.isBlank())    { mItemTag.imageURL = scrap.imageURL    }
                                    async{
                                        //If mItemTag doesn't have a ByteArray
                                        if (mItemTag.imageByteArray.isEmpty()){

                                            //Add store the image from url to the bitarray if it was empty
                                            getBitmapFromUrlAsync(scrap.imageURL).onSuccess { bmp ->
                                                //compress it to at least 1mio
                                                val cBmp = bmp.compressTo1MIO()
                                                //Finally add it to ItemTag's imageByteArray
                                                mItemTag.imageByteArray = cBmp.toByteArray()
                                            }
                                        }
                                    }.await()
                */
                launch(Dispatchers.Main) {
                    //Update views
                    Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
                    loadImageDisplay(false)
                    updateViews()
                }
            }
        }
    }
}