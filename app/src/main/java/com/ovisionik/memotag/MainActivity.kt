package com.ovisionik.memotag

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ovisionik.memotag.db.DatabaseHelper

class MainActivity : AppCompatActivity() {

    //FILTERED ITEMS

    private lateinit var db : DatabaseHelper

    private lateinit var recyclerViewTags: RecyclerView

    private lateinit var tagAdapter: RvAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //init database
        db = DatabaseHelper(this)

        recyclerViewTags = findViewById<RecyclerView?>(R.id.tagItem_rv)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val etFilterList = findViewById<EditText>(R.id.et_filter_rv)
        val ivSearchBtn = findViewById<ImageView>(R.id.iv_search)
        setSupportActionBar(toolbar)

        tagAdapter = RvAdapter(ArrayList(db.getAllTags().reversed()))
        recyclerViewTags.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = tagAdapter
        }

        registerForContextMenu(recyclerViewTags)

        etFilterList.setOnEditorActionListener { v, actionId, event ->

            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                //do what you want on the press of 'done'
                v.clearFocus()
                ivSearchBtn.performClick()
            }
            false
        }

        ivSearchBtn.setOnClickListener{
            val search = etFilterList.text
            tagAdapter.filter.filter(search)
        }

        //Scan button
        val fabCameraScan = findViewById<FloatingActionButton>(R.id.fab_scan_barcode)

        //Setup a barcode picker so get back info from the ScanQRCodeActivity
        val barCodePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ values ->
            cameraScan(values)
        }

        fabCameraScan.setOnClickListener{
            val intent = Intent(this, ScanQRCodeActivity::class.java)
            barCodePicker.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        //Update adapter
        val dbTags = db.getAllTags().reversed()

        //update the adapter only if the number of items are different
        if (tagAdapter.filteredTags.hashCode() != dbTags.hashCode()){

            //Check size if it's the same only item needs to update
            if (tagAdapter.filteredTags.size == dbTags.size){

                val filTags = tagAdapter.filteredTags
                for (i in dbTags.indices){
                    if (dbTags[i].id == filTags[i].id && dbTags[i].hashCode() != filTags[i].hashCode()){
                        //item changed
                        tagAdapter.filteredTags[i] = dbTags[i]
                        recyclerViewTags.adapter?.notifyItemChanged(i)
                    }
                }
            }
            else{
                //We could check and update each individual item

                //... or update everything in the list
                tagAdapter = RvAdapter(ArrayList(dbTags))
                recyclerViewTags.adapter = tagAdapter
            }
        }
    }
    //Setup a barcode picker so get back info from the ScanQRCodeActivity
    private fun cameraScan(values: ActivityResult) {
        val intent = values.data

        val codeFormat = intent?.getStringExtra("codeFormatName")
        val barCode = intent?.getStringExtra("itemCode")

        //Error
        if (barCode.isNullOrEmpty())
        {
            Toast.makeText(this, "Err, no barcode found", Toast.LENGTH_SHORT).show()
            return
        }

        //Check barcode if exists edit if not create new
        if (db.tagBarcodeExists(barCode)){

            //Edit the the one that exists
            Intent(this, EditTagActivity::class.java).also {
                it.putExtra("itemID", db.findTagByBarcode(barCode)?.id) //.id not .label fml
                startActivity(it)
            }

        }else{

            //Create a new tag
            Intent(this, EditTagActivity::class.java).also {
                it.putExtra("itemCode", barCode)
                it.putExtra("codeFormatName", codeFormat)
                startActivity(it)
            }
        }

        onResume()
    }

}

