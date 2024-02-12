package com.ovisionik.memotag

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ovisionik.memotag.data.TagItem
import com.ovisionik.memotag.db.DatabaseHelper

class MainActivity : AppCompatActivity() {

    lateinit var  storedTagItems: List<TagItem>

    lateinit var filteredTagItemList: ArrayList<TagItem>

    lateinit var lvAdapter: TagItemListViewAdapter

    lateinit var db : DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)

        //Main List View
        val filteredItemTagListView = findViewById<ListView>(R.id.filteredMainActTextViews)

        //Get/init stored tag list in db
        storedTagItems = db.getAllTags()

        //Filter
        //TODO filter the list as needed
        filteredTagItemList = ArrayList(storedTagItems)

        //Scan button
        val fabCameraScan = findViewById<FloatingActionButton>(R.id.fab_scan_barcode)

        fabCameraScan.setOnClickListener {
            //Intent(this, ScanQRCodeActivity::class.java).also { startActivity(it) }
            Intent(this, CreateItemTagActivity::class.java).also {
                it.putExtra("barcode", "0565610")
                startActivity(it)
            }
        }

        //tags
        lvAdapter = TagItemListViewAdapter(this, R.layout.tag_item_listview_model, filteredTagItemList)
        filteredItemTagListView.adapter = lvAdapter

        filteredItemTagListView.setOnItemClickListener { parent, view, position, id ->
            //Toast.makeText(this, "Clicked on item position: $position", Toast.LENGTH_SHORT).show()
            Intent(this, ItemTagViewActivity::class.java).also {
                it.putExtra("title", filteredTagItemList[position].label)
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

        when (item.itemId){
            R.id.item_delete -> {
                //Delete item
                val tg = filteredTagItemList[position]
                
                if (db.deleteTag(tg))
                {
                    //Deleted
                    filteredTagItemList.removeAt(position)
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