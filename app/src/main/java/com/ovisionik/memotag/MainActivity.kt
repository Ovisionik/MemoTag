package com.ovisionik.memotag

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    //FILTERED ITEMS

    private lateinit var db: DatabaseHelper

    private lateinit var recyclerViewTags: RecyclerView

    private lateinit var tagAdapter: RvAdapter

    private lateinit var toolbar: Toolbar
    private lateinit var ivSearchBtn: ImageView
    private lateinit var etFilterList: EditText

    //private var firstLoad: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        val elapsed = measureTimeMillis {
//            Thread.sleep(100)
//        }
//
//        showFragment(ListViewFragment())
//
//        Toast.makeText(this, "Created first frag $elapsed", Toast.LENGTH_SHORT).show()
//
        //Preload DB
        db = DatabaseHelper.getInstance(this)
        Toast.makeText(this, "DB ready", Toast.LENGTH_SHORT).show()

        showFragment(ListViewFragment())

        //Animate loading

//
//        registerForContextMenu(recyclerViewTags)
//
//        etFilterList.setOnEditorActionListener { v, actionId, event ->
//
//            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
//                //do what you want on the press of 'done'
//                v.clearFocus()
//                ivSearchBtn.performClick()
//            }
//            false
//        }
//
//        ivSearchBtn.setOnClickListener {
//
//            //Search only if...
//            if (etFilterList.text.isNotBlank() && etFilterList.text.length > 1) {
//                tagAdapter.filter.filter(etFilterList.text)
//
//            } else{
//                Toast.makeText(this, "search too short: try at least 2 character", Toast.LENGTH_SHORT).show()
//                //update adapter (reset filter/item view)
//                updateRvAdapter()
//                //regain focus
//                etFilterList.text.clear()
//                etFilterList.requestFocus()
//            }
//        }
//
//        //Scan button
//        val fabCameraScan = findViewById<FloatingActionButton>(R.id.fab_scan_barcode)
//
//        fabCameraScan.setOnClickListener {
//            Intent(this, ScanQRCodeActivity::class.java).also {
//                startActivity(it)
//            }
//        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_act_fragment_container, fragment) // Replaces the entire screen content
            .addToBackStack(null) // Allows the user to go back to MainActivity
            .commit()
    }

    private fun openListDisplayFragment() {
        val fragment = ListViewFragment()

        // Begin the fragment transaction
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_act_fragment_container, fragment) // Replaces the entire screen content
            .addToBackStack(null) // Allows the user to go back to MainActivity
            .commit()
    }
    override fun onResume() {
        super.onResume()

        showFragment(ListViewFragment())

        //openListDisplayFragment()
//        //Update/re-filter
//        updateRvAdapter()
//
//        lazyLoadItemTags();
//
//        if (etFilterList.text.isNotBlank()){
//            ivSearchBtn.performClick()
//        }
//
//        Toast.makeText(this, "asd", Toast.LENGTH_LONG).show();
    }

    //TODO:
    private fun lazyLoadItemTags(){

        lifecycleScope.launch(Dispatchers.IO) {
        }
    }

    //TODO: Remove/Adapt to load gradually
    private fun updateRvAdapter() {

        //Update adapter
        val dbTags = db.getAllTags().reversed()

        //update the adapter only if the number of items are different
        if (tagAdapter.filteredTags.hashCode() != dbTags.hashCode()) {

            //Check size if it's the same only item needs to update
            if (tagAdapter.filteredTags.size == dbTags.size) {

                val filTags = tagAdapter.filteredTags
                for (i in dbTags.indices) {
                    if (dbTags[i].id == filTags[i].id && dbTags[i].hashCode() != filTags[i].hashCode()) {
                        //item changed
                        tagAdapter.filteredTags[i] = dbTags[i]
                        recyclerViewTags.adapter?.notifyItemChanged(i)
                    }
                }
            } else {
                //We could check and update each individual item

                //... or update everything in the list
                tagAdapter = RvAdapter(ArrayList(dbTags))
                recyclerViewTags.adapter = tagAdapter
            }
        }
    }

    private fun getDummyItems():ArrayList<ItemTag>{
        val tagList = ArrayList<ItemTag>()
        for(i in 0..3){
            var itm = ItemTag()
            itm.id = i;
            itm.label = i.toString();
            itm.barcode = i.toString();
            tagList.add(itm);
        }
        return  tagList;
    }
}

