package com.ovisionik.memotag

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
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
    private lateinit var btnWebSearch       :Button

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
    ){
            bmp ->
        if (bmp != null){
            val resizedBmp = bmp.removeXPercent(0.3,0.3)
            ivImageDisplay.setImageBitmap(resizedBmp)
            mItemTag.imageByteArray = resizedBmp.toByteArray()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_item_tag)


        //init database
        db = DatabaseHelper(this)
        scraper = BarcodeScraper()
        mItemTag = ItemTag()

        //get possible the extras
        val iID = intent.getIntExtra("itemID", -1)
        val barcode = intent.getStringExtra("itemCode")
        val codeFormat = intent.getStringExtra("codeFormatName")

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

            mItemTag= itemTag
        }

        //Initialize private hooks
        initVar()

        //Set/Update view
        updateViews()

        //Hide barcode & show image
        loadImageDisplay(mDisplayBarcode)

        ivImageDisplay.setOnClickListener{
            picPreview.launch()
        }

        //Toggle Image and Barcode view
        tvBarcode.setOnClickListener { toggleItemDisplay() }

        //Close
        btnClose.setOnClickListener { finish() }

        //Scrap product value from the web
        btnWebSearch.setOnClickListener{

            if (!isOnline(this)) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Start a coroutine in the IO context
            lifecycleScope.launch(Dispatchers.IO){

                val result = async { scraper.asyncGetItemScrap(mItemTag.barcode) }.await()

                result.onFailure {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@EditTagActivity, "${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                result.onSuccess { scrap ->
                    //Fill only if blank(empty or white space)
                    if (mItemTag.label.isBlank())       { mItemTag.label    = scrap.label       }
                    if (mItemTag.brand.isBlank())       { mItemTag.brand    = scrap.brand       }
                    if (mItemTag.category.isBlank())    { mItemTag.category = scrap.category    }
                    if (mItemTag.imageURL.isBlank())    { mItemTag.imageURL = scrap.imageURL    }

                    async{
                        //Add store the image from url to the bitarray if it was empty
                        scraper.asyncGetBitmapFromURL(scrap.imageURL).onSuccess { bmp ->
                            if (mItemTag.imageByteArray.isEmpty()){
                                mItemTag.imageByteArray = bmp.toByteArray()
                            }
                        }
                    }.await()

                    launch(Dispatchers.Main) {
                        //Update views
                        Toast.makeText(applicationContext, "Done", Toast.LENGTH_SHORT).show()
                        loadImageDisplay(false)
                        updateViews()
                    }
                }
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
        etLabel.hint = mItemTag.label

        //brand
        etBrand.hint = mItemTag.brand

        //price
        etDefaultPrice.hint = getPriceFormattedString(mItemTag.defaultPrice)

        //date
        tvTagCreationDate.hint = mItemTag.createdOn
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
                //if there is still no image show the place holder
                ivImageDisplay.setImageResource(R.drawable.ic_add_a_photo)
            }
        }
        ivBarcodeDisplay.visibility = if (loadBarcodeBitmap) View.VISIBLE else View.INVISIBLE
        ivImageDisplay.visibility = if (loadBarcodeBitmap) View.INVISIBLE else View.VISIBLE
        return true
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
    private fun Bitmap.removeXPercent(width: Double, height: Double): Bitmap {

        if (width > 1 || height > 1) { return this }
        //input = % to remove, not to keep, so let's get how much we need to cut
        val keepWidth = 1.0 - width
        val keepHeight = 1.0 - height

        val desPxW = this.width * keepWidth
        val desPxH = this.height * keepHeight

        val midW = this.width/2 - desPxW/2
        val midH = this.height/2 - desPxH/2

        return Bitmap.createBitmap(this, midW.toInt(), midH.toInt(), desPxW.toInt(), desPxH.toInt())
    }
    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }
    private fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, this.size)
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
        return false
    }
}