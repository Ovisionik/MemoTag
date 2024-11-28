package com.ovisionik.memotag

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListViewFragment : Fragment(R.layout.fragment_list_view) {

    private lateinit var loadedList:List<ItemTag>

    private lateinit var adapter: RvAdapter

    private lateinit var database: DatabaseHelper

    private var isLoading = false
    private var currentPage = 1
    private val pageSize = 20  // Load 20 items at a time

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val toolbar         =   view.findViewById<Toolbar>(R.id.toolbar)
//        val recyclerView    =   view.findViewById<RecyclerView>(R.id.tagItem_rv)

        val swipeRL         =   view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        val etFilterList    =   view.findViewById<EditText>(R.id.et_filter_rv)
        val btnItemFilter   =   view.findViewById<ImageView>(R.id.iv_search)
        val btnScanQR       =   view.findViewById<FloatingActionButton>(R.id.fab_scan_barcode)

        btnScanQR.setOnClickListener(){
            //Check perms and launchQRScannerActivity
            requestPermissions()
        }

        etFilterList.setOnEditorActionListener { v, actionId, event ->

            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                //do what you want on the press of 'done'
                v.clearFocus()
                btnItemFilter.performClick()
            }
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Run a background task on IO dispatcher

            val result = withContext(Dispatchers.IO) {
                //performHeavyTask()

                adapter = RvAdapter(loadedList) { clickedItem ->
                    // Replace the current fragment with EditTagFragment
                    gotoEditTag(clickedItem)
                }

                //Setup adapter for ListView
                val recyclerView    =   view.findViewById<RecyclerView>(R.id.tagItem_rv)
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = adapter
            }
        }

        //Search button
        btnItemFilter.setOnClickListener(){
            //Has some text to search?
            if (etFilterList.text.isNullOrBlank()) {
                etFilterList.requestFocus()
            }
            adapter.filter.filter(etFilterList.text)
        }

        requireActivity().supportFragmentManager.setFragmentResultListener("saveTagKey", this) { key, bundle ->
            val tagID = bundle.getInt("tagID") // Retrieve the data using the same key
            Log.d("ResultListener", "Received key: $key") // Handle the received data
            Log.d("ResultListener", "Received tag: $tagID") // Handle the received data
            //Update adapter? so it has the new modified list

            val modTag = database.findItemTagByID(tagID)

            if(tagID < 0){
                //-1 = new item
                loadDataFromDatabase()
            }
            else
            {
                adapter.itemChanged(modTag as ItemTag)
            }
        }

        swipeRL.setOnRefreshListener{
            loadDataFromDatabase()

            // Stop the refresh animation once done
            swipeRL.isRefreshing = false
        }
    }

    companion object {
        fun newInstance(initialList :List<ItemTag>, databaseHelper: DatabaseHelper) = ListViewFragment().apply{
            loadedList = initialList
            database = databaseHelper
        }
    }

    private fun loadDataFromDatabase() {

        //For whatever reason if the func was called before init -> don't do anything
        if(!::adapter.isInitialized)
            return

        loadedList = database.getAllTags().reversed().toCollection(ArrayList()) // Fetch data
        adapter.setData(loadedList) // Update adapter data
    }

    private fun requestPermissions() {
        val cameraPermission = Manifest.permission.CAMERA
        val permissions = mutableListOf<String>()

        // Check if Camera permission is already granted
        if (ContextCompat.checkSelfPermission(requireContext(), cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            // If the user has denied the permission with "Don't ask again," explain why the app needs it
            if (shouldShowRequestPermissionRationale(cameraPermission)) {
                showPermissionRationaleDialog()
            } else {
                permissions.add(cameraPermission)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            // Permission is already granted; proceed with camera functionality
            launchQRScannerActivity()
        }
    }
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage("Camera permission is required to use this feature. Please allow it in app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open the app's settings page
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check the results for each requested permission
        if (permissions[Manifest.permission.CAMERA] == true) {
            // Camera permission granted, proceed with the camera functionality
            launchQRScannerActivity()
        } else {
            // Permission denied, inform the user
            Toast.makeText(requireContext(), "Camera permission is needed for the app to work", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanQRCodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val action = result.data?.getStringExtra("action")
            val barcode = result.data?.getStringExtra("code").toString()

            val db  = DatabaseHelper.getInstance(requireContext())
            val itm = db.findTagByBarcode(barcode)

            if (action == "editTag_fragment" && itm !=null) {
                gotoEditTag(itm)
            }else{
                val newTag = ItemTag()
                newTag.barcode = barcode
                gotoEditTag(newTag)
            }
        }
    }

    private fun gotoEditTag(tag: ItemTag){

        val fragment = EditTagFragment.newInstance(tag)
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_and_slide_in,  // Animation for fragment entering
                R.anim.fade_and_slide_out, // Animation for fragment exiting
                R.anim.fade_and_slide_in,  // Animation for fragment entering (when popping back)
                R.anim.fade_and_slide_out   // Animation for fragment exiting (when popping back)
            )
            .add(R.id.content_frame, fragment)
            .addToBackStack(null) // Add transaction to the back stack
            .commit()
    }

    private fun launchQRScannerActivity() {
        val intent = Intent(requireContext(), ScanQRCodeActivity::class.java)
        scanQRCodeLauncher.launch(intent)
    }
}