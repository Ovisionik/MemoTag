package com.ovisionik.memotag

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import com.ovisionik.memotag.utils.BitmapUtils.toBitmap

class ScanQRCodeActivity : AppCompatActivity() {

    private var mFormatName = ""
    private var mScannedCode = ""
    private var mItemTag = ItemTag()

    private lateinit var fabCancel:FloatingActionButton
    private lateinit var fabRetake:FloatingActionButton
    private lateinit var fabOK:FloatingActionButton

    private lateinit var ivImage:ImageView
    private lateinit var tvTextResult:TextView
    private lateinit var indicView:LinearLayout
    private lateinit var labelIndic:TextView
    private lateinit var brandIndic:TextView
    private lateinit var dPriceIndic:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setVisible(false)
        setContentView(R.layout.activity_scan_qrcode)

        ivImage = findViewById(R.id.iv_code)
        tvTextResult = findViewById(R.id.code_result)

        indicView = findViewById(R.id.ll_indic)
        labelIndic = findViewById(R.id.tv_name_indic_scan)
        brandIndic = findViewById(R.id.tv_brand_indic_scan)
        dPriceIndic = findViewById(R.id.tv_price_indic_scan)

        //Floating action buttons
        fabCancel = findViewById(R.id.fab_cancel)
        fabRetake = findViewById(R.id.fab_redo)
        fabOK = findViewById(R.id.fab_check)

        //Open Camera
        checkCameraPermission(this)

        //Go Back
        fabCancel.setOnClickListener{
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Open Camera
        fabRetake.setOnClickListener {
            //Cleanup view
            resetViews()
            checkCameraPermission(this)

            this.setVisible(true)
        }

        //Create or edit (use) scanned code
        fabOK.setOnClickListener{
            Intent(this, EditTagActivity::class.java).also{
                it.putExtra("itemID", mItemTag.id)
                it.putExtra("codeFormatName", mFormatName)
                it.putExtra("itemCode", mScannedCode)
                startActivity(it)
                finish() //Useless?
            }
        }
    }

    private fun resetViews() {
        tvTextResult.text = ""
        ivImage.setImageResource(R.drawable.qrcode_scan)
        indicView.visibility = View.INVISIBLE
    }

    private val barCodeLauncher = registerForActivityResult(ScanContract()){
            result ->
        if (result.contents == null){
            Toast.makeText(this@ScanQRCodeActivity, "Camera closed", Toast.LENGTH_SHORT).show()
            finish()
        }
        else {
            //Got result
            mFormatName = result.formatName
            mScannedCode = result.contents.toString()

            val tag = ItemTag()

            tag.barcode = mScannedCode
            tag.barcodeFormat = mFormatName

            mItemTag = DatabaseHelper(this).findTagByBarcode(mScannedCode) ?: tag

            updateViews(mItemTag)
        }
    }

    private fun updateViews(it:ItemTag) {
        tvTextResult.text = it.barcode

        //Set image
        if (it.imageByteArray.isNotEmpty()) {
            val bmp = it.imageByteArray.toBitmap()
            ivImage.setImageBitmap(bmp)
        }else{
            ivImage.setImageResource(R.drawable.qrcode_scan)
        }

        if (it.id != -1){
            labelIndic.text     = it.label
            brandIndic.text     = it.brand
            dPriceIndic.text    = it.moneyString()
            indicView.visibility = View.VISIBLE
        }else{
            indicView.visibility = View.INVISIBLE
        }
    }

    private fun showCamera(){
        val option = ScanOptions()
        option.setPrompt("Scan a Code")
        option.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        option.setCameraId(0)
        option.setBeepEnabled(false)
        option.setOrientationLocked(true)

        barCodeLauncher.launch(option)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
            isGranted ->
        if (isGranted)
        {
            showCamera()
        }
    }

    private fun checkCameraPermission(context: Context) {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
            Toast.makeText(this, "Camera required", Toast.LENGTH_SHORT).show()
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        else
        {
            showCamera()
        }
    }
}