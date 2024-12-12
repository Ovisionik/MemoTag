package com.ovisionik.memotag

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
import com.ovisionik.memotag.scraper.ItemTagWebScraper
import com.ovisionik.memotag.utils.AppNetwork
import com.ovisionik.memotag.utils.BitmapUtils.compressTo1MIO
import com.ovisionik.memotag.utils.BitmapUtils.removeXPercent
import com.ovisionik.memotag.utils.BitmapUtils.toBitmap
import com.ovisionik.memotag.utils.BitmapUtils.toByteArray
import com.ovisionik.memotag.utils.NoteAdapter
import com.ovisionik.memotag.utils.PermissionManagerUtils
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class EditTagFragment : Fragment() {

    //Flags
    private var mDisplayBarcode: Boolean = false
    private var cameraIsBusy = false

    //Utils
    private lateinit var db: DatabaseHelper
    private lateinit var scraper: ItemTagWebScraper
    private lateinit var mItemTag: ItemTag

    // Views
    private lateinit var ivImageDisplay: ImageView
    private lateinit var ivBarcodeDisplay: ImageView
    private lateinit var etLabel: EditText
    private lateinit var etBrand: EditText
    private lateinit var etNewBarcode: EditText
    private lateinit var etBarcode: EditText
    private lateinit var etBarcodeFormat: EditText
    private lateinit var etCategory: EditText
    private lateinit var etURL: EditText

    private lateinit var etDefaultPrice: EditText
    private lateinit var tvTagCreationDate: TextView
    private lateinit var tvBarcode: TextView

    private lateinit var btnToggleExtra: Button

    private lateinit var btnSave: Button
    private lateinit var btnClose: Button
    private lateinit var btnWebSearch: ImageButton

    //Note
    private lateinit var noteRecyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter


    private val picPreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        cameraIsBusy = true

        if (bmp != null) {
            val resizedBmp = bmp.removeXPercent(0.3, 0.3)
            val bmp1 = resizedBmp.compressTo1MIO()
            ivImageDisplay.setImageBitmap(bmp1)
            mItemTag.imageByteArray = bmp1.toByteArray()
        }

        cameraIsBusy = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mItemTag.id = it.getInt("itemID")
            mItemTag.barcode = it.getString("itemCode").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_edit_tag, container, false)
        view.isClickable = true

        return view
    }

    companion object {

        fun newInstance(itm : ItemTag) = EditTagFragment().apply {
            mItemTag = itm
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initVar(view)

        // Initialize database and scraper
        db = DatabaseHelper.getInstance(requireContext())
        scraper = ItemTagWebScraper()

        updateViews(mItemTag)
        loadImageDisplay(mDisplayBarcode)

        setupListeners()
    }

    private fun initVar(view: View) {
        etLabel = view.findViewById(R.id.et_label)
        etBrand = view.findViewById(R.id.et_brand)
        etCategory = view.findViewById(R.id.et_category)
        etNewBarcode = view.findViewById(R.id.et_new_barcode)
        etBarcode   = view.findViewById(R.id.et_barcode)
        etBarcodeFormat = view.findViewById(R.id.et_barcode_format)
        etURL   = view.findViewById(R.id.et_url)

        tvBarcode = view.findViewById(R.id.tv_barcode)
        etDefaultPrice = view.findViewById(R.id.et_default_price)
        tvTagCreationDate = view.findViewById(R.id.tv_tag_date)
        ivImageDisplay = view.findViewById(R.id.iv_image)
        ivBarcodeDisplay = view.findViewById(R.id.iv_barcode)

        btnSave = view.findViewById(R.id.btn_save)
        btnClose = view.findViewById(R.id.btn_close)
        btnWebSearch = view.findViewById(R.id.btn_web_scrap)

        btnToggleExtra = view.findViewById(R.id.btn_extra_info)
    }

    private fun setupListeners() {
        ivImageDisplay.setOnClickListener {
            if (!cameraIsBusy) {
                picPreview.launch()
            }
        }

        ivImageDisplay.setOnLongClickListener { view ->
            val popupMenu = PopupMenu(
                view.context,
                view,
                Gravity.END
            )

            try {
                val method = popupMenu.menu.javaClass.getDeclaredMethod(
                    "setOptionalIconsVisible",
                    Boolean::class.javaPrimitiveType
                )
                method.isAccessible = true
                method.invoke(popupMenu.menu, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.inflate(R.menu.delete_popup_menu)
            popupMenu.setOnMenuItemClickListener { mi ->
                when (mi.itemId) {
                    R.id.delete_item -> {
                        mItemTag.imageByteArray = ByteArray(0)
                        loadImageDisplay(false)
                        true
                    }
                    else -> true
                }
            }
            popupMenu.show()
            true
        }

        tvBarcode.setOnClickListener { toggleItemDisplay() }

        tvBarcode.setOnLongClickListener {
            if (tvBarcode.text.isNotBlank()) {
                val clipboard: ClipboardManager =
                    requireActivity().getSystemService(ClipboardManager::class.java)
                val clip = ClipData.newPlainText(mItemTag.barcode, mItemTag.barcode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied: ${mItemTag.barcode}", Toast.LENGTH_SHORT).show()
            }
            true
        }

        val extraLayout = view?.findViewById<LinearLayout>(R.id.ll_extra_info_layout)
        btnToggleExtra.setOnClickListener {
            if (extraLayout?.visibility == View.VISIBLE){
                extraLayout.visibility = View.GONE
            }else{
                extraLayout?.visibility = View.VISIBLE
            }
        }

        btnClose.setOnClickListener {
            val rootView = requireActivity().window.decorView.rootView
            // Check if keyboard is open
            val isKeyboardOpen = isKeyboardVisible(rootView)

            if (isKeyboardOpen) {
                // Close the keyboard
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(rootView.windowToken, 0)
            } else {
                // Close the fragment
                parentFragmentManager.popBackStack()
            }
        }

        btnWebSearch.setOnClickListener {

            if (!PermissionManagerUtils.askForInternetPermission(requireActivity()))
                return@setOnClickListener

            if (AppNetwork.isInternetAvailable(requireContext())) {
                Toast.makeText(context, "Experimental", Toast.LENGTH_SHORT).show()
                setWebScraps()
            } else {
                Toast.makeText(context, "No internet connection :(", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {

            if (etNewBarcode.text.isNotEmpty()) {
                mItemTag.barcode = etNewBarcode.text.toString()
            }else{
                Toast.makeText(context, getString(R.string.barcode_prompt_msg), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (etLabel.text.isNotEmpty()) {
                mItemTag.label = etLabel.text.toString()
            }
            if (etBrand.text.isNotEmpty()) {
                mItemTag.brand = etBrand.text.toString()
            }

            if (etCategory.text.isNotEmpty()) {
                mItemTag.category = etCategory.text.toString()
            }

            if (etURL.text.isNotEmpty()) {
                mItemTag.imageURL = etURL.text.toString()
            }

            if (etDefaultPrice.text.isNotEmpty()) {
                mItemTag.defaultPrice = etDefaultPrice.text.toString().toDoubleOrNull() ?: 0.00
            }

            if (db.tagExists(mItemTag)) {
                db.updateTag(mItemTag)
            } else {
                db.insertItemTag(mItemTag)
            }
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()

            //Notify the list viewer the item with this id changed
            setFragmentResult("saveTagKey", bundleOf("modifiedTag" to mItemTag))

            parentFragmentManager.popBackStack()
        }
    }

    private fun isKeyboardVisible(rootView: View): Boolean {
        val visibleBounds = Rect()
        rootView.getWindowVisibleDisplayFrame(visibleBounds)

        val screenHeight = rootView.height
        val keypadHeight = screenHeight - visibleBounds.bottom

        // If the keypad height is more than 25% of the screen height, it is probably visible
        return keypadHeight > screenHeight * 0.25
    }

    private fun updateViews(itm: ItemTag){

        //barcode
        if (itm.barcode.isNotBlank())
        {
            //Old item ->

            tvBarcode.text = itm.barcode.plus(" " + if(mDisplayBarcode)"◈" else "❖")

            //Show the barcode field?
            view?.findViewById<TextView>(R.id.tv_new_barcode_indic)?.visibility = View.GONE
            view?.findViewById<EditText>(R.id.et_new_barcode)?.visibility = View.GONE
            etNewBarcode.setText(itm.barcode)
            etBarcode.setText(itm.barcode)

        }else{
            //For new item let the user set the barcode/id

            view?.findViewById<TextView>(R.id.tv_barcode_indic)?.visibility = View.GONE
            view?.findViewById<EditText>(R.id.et_barcode)?.visibility = View.GONE

            tvBarcode.isEnabled = false
            etNewBarcode.hint = getString(R.string.barcode_prompt_msg_indic)
            tvBarcode.text = getString(R.string.new_indicator)
        }

        //label
        if (itm.label.isNotBlank())
        {
            etLabel.hint = itm.label
            etLabel.setText(etLabel.hint)
        }else{
            etLabel.hint = getString(R.string.label_prompt_msg)
        }

        //brand
        if (itm.brand.isNotBlank())
        {
            etBrand.hint = itm.brand
            etBrand.setText(etBrand.hint)
        }

        //Format
        if (itm.barcodeFormat.isNotBlank())
        {
            etBarcodeFormat.hint = itm.barcodeFormat
            etBarcodeFormat.setText(etBarcodeFormat.hint)
        }

        //item Url
        if (itm.imageURL.isNotBlank())
        {
            etURL.hint = itm.imageURL
            etURL.setText(etURL.hint)
        }

        //category
        if (itm.category.isNotBlank())
        {
            etCategory.hint = itm.category
            etCategory.setText(etCategory.hint)
        }

        //price
        etDefaultPrice.hint = itm.moneyString()

        //date
        tvTagCreationDate.hint = itm.createdOn
    }
    private fun loadImageDisplay(loadBarcodeBitmap: Boolean): Boolean {

        Log.d("loadImageDisplay", "imageview null : "+"${ivImageDisplay.drawable == null}")
        Log.d("loadImageDisplay", "barcode  : "+ mItemTag.barcode)
        Log.d("loadImageDisplay", "barcode format : ${mItemTag.barcodeFormat}")

        //Show barcode image
        if (loadBarcodeBitmap){
            //create the images only if it's not cashed
            if (ivBarcodeDisplay.drawable == null){

                //Nothing to gen
                if (mItemTag.barcode.isEmpty() || mItemTag.barcodeFormat.isEmpty()){
                    Toast.makeText(requireContext(), "Cant generate barcode", Toast.LENGTH_SHORT).show()
                    return false
                }

                //barcode format
                val format = BarcodeFormat.valueOf(mItemTag.barcodeFormat)
                Log.d("loadImageDisplay", "barcode format : $format")

                //Writer
                val writer: Writer = when (format){
                    BarcodeFormat.AZTEC -> AztecWriter()
                    BarcodeFormat.EAN_13 -> EAN13Writer()
                    BarcodeFormat.EAN_8 -> EAN8Writer()
                    BarcodeFormat.CODE_128 -> Code128Writer()
                    BarcodeFormat.CODABAR -> Code128Writer()
                    BarcodeFormat.QR_CODE -> QRCodeWriter()
                    else -> {
                        Toast.makeText(requireContext(), "Sorry, can't recreate the barcode", Toast.LENGTH_SHORT).show()
                        return false
                    }
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
                    Toast.makeText(requireContext(), "${err.message}", Toast.LENGTH_SHORT).show()
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

                Log.d("loadImageDisplay", "image loaded from ByteArray")
            }else if(mItemTag.imageURL.isNotEmpty()){
                Picasso.get()
                    .load(mItemTag.imageURL) // URL of the image
                    .placeholder(R.drawable.ic_change_circle) // Optional placeholder
                    .error(R.drawable.ic_image_broken) // Optional error image
                    .into(ivImageDisplay) // Target ImageView
                Log.d("loadImageDisplay", "image loaded from Url")
            }
            else{
                //if there is still no image show the place holder
                ivImageDisplay.setImageResource(R.drawable.ic_add_a_photo)
            }
        }
        ivBarcodeDisplay.visibility = if (loadBarcodeBitmap) View.VISIBLE else View.INVISIBLE
        ivImageDisplay.visibility = if (loadBarcodeBitmap) View.INVISIBLE else View.VISIBLE
        return true
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
            Toast.makeText(requireContext(), "Couldn't load the image", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun setWebScraps() {
        // Start a coroutine in the IO context
        lifecycleScope.launch(Dispatchers.IO) {

            val scraper = ItemTagWebScraper()

            val wsResult = async { scraper.autoScrap(mItemTag) }.await()

            wsResult.onSuccess { scrap ->
                //Fill only if blank(empty or white space)
                if (etLabel.text.isBlank())             { mItemTag.label            = scrap.label           }
                if (etBrand.text.isBlank())             { mItemTag.brand            = scrap.brand           }
                if (mItemTag.category.isBlank())        { mItemTag.category         = scrap.category        }
                if (mItemTag.imageURL.isBlank())        { mItemTag.imageURL         = scrap.imageURL        }
                //if (mItemTag.imageByteArray.isEmpty())  { mItemTag.imageByteArray   = scrap.imageByteArray  }
            }

            launch(Dispatchers.Main) {
                //Update views
                Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                loadImageDisplay(false)
                updateViews(mItemTag)
            }
        }
    }
}
