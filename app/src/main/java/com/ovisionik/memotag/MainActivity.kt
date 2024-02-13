package com.ovisionik.memotag

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper

class MainActivity : AppCompatActivity() {

    //FILTERED ITEMS
    lateinit var filteredItemTagList: ArrayList<ItemTag>

    lateinit var lvAdapter: TagItemListViewAdapter

    lateinit var db : DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        //Scan button
        val fabCameraScan = findViewById<FloatingActionButton>(R.id.fab_scan_barcode)

        //Setup a barcode picker so get back info from the ScanQRCodeActivity
        val barCodePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ values ->

            val intent = values.data

            val codeFormat = intent?.getStringExtra("codeFormatName")
            val barCode = intent?.getStringExtra("itemCode")

            //Error
            if (barCode.isNullOrEmpty())
            {
                Toast.makeText(this, "Err, no barcode found", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            //Check barcode if exists edit if not create new

            if (db.tagBarcodeExists(barCode)){

                //Edit the the one that exists
                Intent(this, ItemTagViewActivity::class.java).also {
                    it.putExtra("itemID", db.findTagByBarcode(barCode)?.id) //.id not .label fml
                    startActivity(it)
                }

            }else{

                //Create a new tag
                Intent(this, CreateItemTagActivity::class.java).also {
                    it.putExtra("itemCode", barCode)
                    it.putExtra("codeFormatName", codeFormat)
                    startActivity(it)

                    //update list
                    filteredItemTagList = ArrayList(db.getAllTags())
                }
            }
        }

        fabCameraScan.setOnClickListener{
            val intent = Intent(this, ScanQRCodeActivity::class.java)
            barCodePicker.launch(intent)

        }
    }

    override fun onResume() {
        super.onResume()

        refreshTagListView()
    }

    private fun refreshTagListView() {

        //Get/init stored tag list in db
        val storedItemTags = db.getAllTags()

        //Filter
        //TODO filter the list as needed
        filteredItemTagList = ArrayList(storedItemTags)

        //Main List View
        val filteredItemTagListView = findViewById<ListView>(R.id.filteredMainActTextViews)

        lvAdapter = TagItemListViewAdapter(this, R.layout.tag_item_listview_model, filteredItemTagList)
        filteredItemTagListView.adapter = lvAdapter

        filteredItemTagListView.setOnItemClickListener { parent, view, position, id ->
            //Toast.makeText(this, "Clicked on item position: $position", Toast.LENGTH_SHORT).show()
            Intent(this, ItemTagViewActivity::class.java).also {
                it.putExtra("itemID", filteredItemTagList[position].id)
                startActivity(it)
            }
        }

        registerForContextMenu(filteredItemTagListView)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menuInflater.inflate(R.menu.list_context_menu, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        val info: AdapterContextMenuInfo = item.menuInfo as AdapterContextMenuInfo
        val position = info.position

        //
        when (item.itemId){
            R.id.item_delete -> {
                //Delete item
                val tg = filteredItemTagList[position]
                
                if (db.deleteTag(tg))
                {
                    //Deleted
                    filteredItemTagList.removeAt(position)
                    lvAdapter.notifyDataSetChanged()
                }
                else{
                    Toast.makeText(this, "Failed to delete : ${tg.label}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onContextItemSelected(item)
    }
}