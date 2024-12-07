package com.ovisionik.memotag

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import com.ovisionik.memotag.utils.ExcelGoogleSheetUtils
import com.ovisionik.memotag.utils.MemoTagThemeUtils
import com.ovisionik.memotag.utils.PermissionManagerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListViewFragment : Fragment(R.layout.fragment_list_view) {

    private lateinit var adapter: RvAdapter
    private lateinit var database: DatabaseHelper
    private val itemTags = mutableListOf<ItemTag>() // Mutable list for dynamic updates
    private var autoLoad = true // Flag to load more data
    private var isLoading = false // Flag to prevent duplicate loads
    private var currentPage = 0 // Track the current page
    private val pageSize = 10 // Number of items to load per page

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MemoTagThemeUtils.applyUserPrefTheme(requireContext())

        database = DatabaseHelper.getInstance(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.tagItem_rv)
        val swipeRL = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        val etFilterList = view.findViewById<EditText>(R.id.et_filter_rv)
        val btnItemFilter = view.findViewById<ImageView>(R.id.iv_search)
        val btnScanQR = view.findViewById<FloatingActionButton>(R.id.fab_scan_barcode)
        val btnAddTag = view.findViewById<FloatingActionButton>(R.id.fab_add_tag)

        showProgressBarView()

        // Initialize RecyclerView and Adapter
        adapter = RvAdapter(itemTags) { clickedItem ->
            gotoEditTag(clickedItem)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Scroll Listener for Pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition >= totalItemCount) && firstVisibleItemPosition >= 0) {
                    loadMoreData()
                }
            }
        })

        // Refresh Listener
        swipeRL.setOnRefreshListener {
            //Clear the search bar
            etFilterList.text.clear()
            //Reset the auto loading mechanism
            currentPage = 0 // Reset pagination
            itemTags.clear() // Clear existing data
            adapter.notifyDataSetChanged()
            autoLoad = true
            loadMoreData()
            swipeRL.isRefreshing = false
        }

        // Initial Load
        loadMoreData()

        // Button Handlers
        btnAddTag.setOnClickListener {
            gotoEditTag(ItemTag())
        }
        btnScanQR.setOnClickListener {
            requestPermissions()
        }
        btnItemFilter.setOnClickListener {

            //Hide keyboard if any
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0)

            val query = etFilterList.text.toString()

            //search / filter
            filterList(query)
        }

        //listen for the item if it was saved in Edit Tag fragment to apply the modifications
        requireActivity().supportFragmentManager.setFragmentResultListener("saveTagKey", this) { key, bundle ->
            val tagID = bundle.getInt("tagID") // Retrieve the data using the same key
            Log.d("ResultListener", "Received key: $key") // Handle the received data
            Log.d("ResultListener", "Received tag: $tagID") // Handle the received data
            //Update adapter? so it has the new modified list
            val modTag = database.findItemTagByID(tagID)
            if(tagID < 0) { //-1 = new item
                btnItemFilter.callOnClick()
            }
            else {
                adapter.itemChanged(modTag as ItemTag)
            }
        }

        //Edit text / Search bar

        //if the user click on ok/done auto press the search btn
        etFilterList.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                // Perform your action here
                btnItemFilter.callOnClick()
                true // Return true to indicate the event was handled
            } else { false }
        }

        //Select all text if clicked on the field
        etFilterList.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etFilterList.post {
                    etFilterList.selectAll()
                }
            }
        }

        checkForAutoUpdateOption()
    }

    private fun checkForAutoUpdateOption() {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val useOnlineGoogleSheet = sharedPreferences.getBoolean("use_online_google_sheet", false)

        if(!useOnlineGoogleSheet)
            return

        CoroutineScope(Dispatchers.Main).launch {

            showProgressBarView()

            ExcelGoogleSheetUtils.readGoogleSheet(getString(R.string.google_sheets_url)) { success, message, data ->
                if (success) {
                    println(message) // Output success message
                    data?.let { itmTgs ->
                        //We've got data
                        println("Fetched ${itmTgs.size} items")

                        val listA = database.getAllTags()

                        val filteredListB = data.filter { itemB ->
                            listA.none { itemA -> itemA.barcode == itemB.barcode }
                        }
                        filteredListB.forEach {
                            database.insertItemTag(it)
                            val insertPosition = itemTags.size
                            itemTags.add(it) // Add new items to your data list
                            adapter.notifyItemRangeInserted(insertPosition, 1) // Notify adapter
                            currentPage++ // Increment page
                        }
                    }
                } else {
                    println("Error: $message") // Output error message
                }
            }

            showProgressBarView(false)
        }
    }

    private fun loadMoreData() {

        if(!autoLoad) {return}

        Log.d("loadMoreData", "Loading more data...")

        showProgressBarView()
        viewLifecycleOwner.lifecycleScope.launch {
            val newItems = withContext(Dispatchers.IO) {
                database.getTagsPaginated(pageSize, currentPage * pageSize)
            }
            if (newItems.isNotEmpty()) {
                val insertPosition = itemTags.size
                itemTags.addAll(newItems) // Add new items to your data list
                adapter.notifyItemRangeInserted(insertPosition, newItems.size) // Notify adapter
                currentPage++ // Increment page

                newItems.forEach{
                    Log.d("loadMoreData", "new item id : ${it.id}")
                }
            } else {
                // Optionally handle if no new items are fetched
                Toast.makeText(requireContext(), "End of the list", Toast.LENGTH_LONG).show()
                autoLoad = false
            }

            isLoading = false
            showProgressBarView(false)
        }
    }

    //Filter the list based on the search text
    private fun filterList(query: String) {

        if (query.isEmpty() || query.length < 2) {
            Log.d("filterList", "query empty (or length <2)")

            //Reset the list view items and return

            currentPage = 0 // Reset pagination
            itemTags.clear() // Clear existing data
            autoLoad = true
            loadMoreData()
            adapter.notifyDataSetChanged()

            return
        }

        Log.d("filterList", "Filtering... with $query")

        showProgressBarView()

        viewLifecycleOwner.lifecycleScope.launch {
            val filteredItems = withContext(Dispatchers.IO) {
                    val dbTags = database.getAllTags()
                    val result = dbTags.filter { filter ->
                        //By label
                        filter.label.lowercase().contains(query)
                                //By Barcode
                                || filter.barcode.lowercase().contains(query)
                                || numberEquals(filter.defaultPrice, query)
                    }
                    Log.d("filterList", "list found $result")
                    result
                }

            // Update adapter with the filtered items
            autoLoad = false
            currentPage = 0
            itemTags.clear()
            adapter.setData(filteredItems)
            adapter.notifyDataSetChanged()
            showProgressBarView(false)
        }
    }

    private fun showProgressBarView(setVisible:Boolean = true){
        val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)

        if (setVisible){
            progressBar?.visibility = View.VISIBLE // Show progress bar
        }else{
            progressBar?.visibility = View.GONE // Hide progress bar
        }
    }

    private fun numberEquals(price: Double, text: String): Boolean {

        val searchNumber = try {
            java.lang.Double.parseDouble(text)
        }catch (err:RuntimeException){
            return false
        }

        return searchNumber.equals(price)
    }

    private fun gotoEditTag(tag: ItemTag) {
        val fragment = EditTagFragment.newInstance(tag)
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_and_slide_in,
                R.anim.fade_and_slide_out,
                R.anim.fade_and_slide_in,
                R.anim.fade_and_slide_out
            )
            .add(R.id.content_frame, fragment)
            .addToBackStack(null)
            .commit()
        println("Edit fragment launched for ${tag.barcode}")
    }

    private fun requestPermissions() {
        val camPerm = PermissionManagerUtils.askForCameraPermission(requireActivity())
        if(!camPerm)
            return
        // Permission is already granted; proceed with camera functionality
        launchQRScannerActivity()
    }

    private fun launchQRScannerActivity() {
        val intent = Intent(requireContext(), ScanQRCodeActivity::class.java)
        scanQRCodeLauncher.launch(intent)
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
}