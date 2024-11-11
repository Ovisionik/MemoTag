package com.ovisionik.memotag

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper

class ListViewFragment : Fragment(R.layout.fragment_list_view) {

    private lateinit var db: DatabaseHelper

    private lateinit var listDisplay:List<ItemTag>

    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: RvAdapter

    private var isLoading = false
    private var currentPage = 1
    private val pageSize = 20  // Load 20 items at a time

    private lateinit var toolbar: Toolbar
    private lateinit var ivSearchBtn: ImageView
    private lateinit var etFilterList: EditText

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init database
        db              =   DatabaseHelper.getInstance(requireContext())
        listDisplay     =   db.getAllTags().reversed().toCollection(ArrayList())
        adapter         =   RvAdapter(listDisplay)

        recyclerView    =   view.findViewById<RecyclerView>(R.id.tagItem_rv)

        val toolbar         =   view.findViewById<Toolbar>(R.id.toolbar)
        val etFilterList    =   view.findViewById<EditText>(R.id.et_filter_rv)
        val btnItemFilter   =   view.findViewById<ImageView>(R.id.iv_search)
        val btnScanQR       =   view.findViewById<FloatingActionButton>(R.id.fab_scan_barcode)

        //Setup adapter for ListView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        loadDataFromDatabase()

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

        //Search button
        btnItemFilter.setOnClickListener(){
            //Has some text to search?
            if (etFilterList.text.isNullOrBlank()) {
                etFilterList.requestFocus()
            }
            adapter.filter.filter(etFilterList.text)
        }
    }

    private fun filterItemShown(text: String) {

    }

    //TODO: Remove on resume here and load data and make it reload this fragment from the mainActivity
    override fun onResume() {
        super.onResume()

        //Update adapter
        val dbTags = db.getAllTags().reversed()

        //update the adapter only if the number of items are different
        if (adapter.filteredTags.hashCode() != dbTags.hashCode()) {

            //Check size if it's the same only item needs to update
            if (adapter.filteredTags.size == dbTags.size) {

                val filTags = adapter.filteredTags
                for (i in dbTags.indices) {
                    if (dbTags[i].id == filTags[i].id && dbTags[i].hashCode() != filTags[i].hashCode()) {
                        //item changed
                        adapter.filteredTags[i] = dbTags[i]
                        recyclerView.adapter?.notifyItemChanged(i)
                    }
                }
            } else {
                //We could check and update each individual item

                //... or update everything in the list
                loadDataFromDatabase()
            }
        }
    }

    private fun loadDataFromDatabase() {
        val databaseHelper = DatabaseHelper.getInstance(requireContext())
        val itemTags = databaseHelper.getAllTags().reversed().toCollection(ArrayList()) // Fetch data
        adapter.setData(itemTags) // Update adapter data
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

    private fun launchQRScannerActivity() {
        val intent = Intent(requireContext(), ScanQRCodeActivity::class.java)
        startActivity(intent)
    }
}