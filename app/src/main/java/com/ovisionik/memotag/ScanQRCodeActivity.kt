package com.ovisionik.memotag

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

public class ScanQRCodeActivity : AppCompatActivity() {

    private var mCodeResult = ""

            var codeResult = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setVisible(false)
        setContentView(R.layout.activity_scan_qrcode)

        // 3 fabs
        val fab_cancel = findViewById<FloatingActionButton>(R.id.fab_cancel)
        val fab_retake = findViewById<FloatingActionButton>(R.id.fab_redo)
        val fab_ok = findViewById<FloatingActionButton>(R.id.fab_check)

        //Scan Result Indicator
        val textResult = findViewById<TextView>(R.id.code_result)

        //Go Back
        fab_cancel.setOnClickListener{
            finish()
        }

        // Open Camera
        fab_retake.setOnClickListener {
            checkCameraPermission(this)
            textResult.text = mCodeResult
        }

        //Create or edit (use) scanned code
        fab_ok.setOnClickListener{
            codeResult = mCodeResult
            Toast.makeText(this, "$codeResult", Toast.LENGTH_SHORT).show()
        }

        //Open Camera
        checkCameraPermission(this)

        this.setVisible(true)
    }

    private val barCodeLauncher = registerForActivityResult(ScanContract()){
            result ->
        if (result.contents == null){
            Toast.makeText(this@ScanQRCodeActivity, "Scan cancelled", Toast.LENGTH_SHORT).show()

            //Nothing to do exit
            finish()
        }
        else {
            //Got result
            mCodeResult = result.contents
        }
    }

    private fun showCamera(){
        val option = ScanOptions()
        option.setPrompt("Scan a Code")
        //option.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        option.setCameraId(0)
        option.setBeepEnabled(false)
        option.setOrientationLocked(false)

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