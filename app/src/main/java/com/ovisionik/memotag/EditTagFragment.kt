package com.ovisionik.memotag

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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
import com.ovisionik.memotag.utils.AppNetwork.isOnline
import com.ovisionik.memotag.utils.BitmapUtils.compressTo1MIO
import com.ovisionik.memotag.utils.BitmapUtils.removeXPercent
import com.ovisionik.memotag.utils.BitmapUtils.toBitmap
import com.ovisionik.memotag.utils.BitmapUtils.toByteArray
import com.ovisionik.memotag.utils.NoteAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class EditTagFragment : Fragment() {

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
    private lateinit var etDefaultPrice: EditText
    private lateinit var tvTagCreationDate: TextView
    private lateinit var tvBarcode: TextView

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
        fun newInstance(itemId: Int, itemCode: String, codeFormat: String?) =
            EditTagFragment().apply {

                arguments = Bundle().apply {

                    if(itemId != -1){

                        //ID Exists (in the database)
                        val dtb = DatabaseHelper.getInstance(requireContext())
                        val  itemTag = dtb.findItemTagByID(itemId)

                        //Found the item?
                        if (itemTag != null){
                            mItemTag = itemTag
                        }else{
                            //They got us good they gave a fake ID wp me (go fix the code)
                            Log.wtf(
                                "Fake ID",
                                "This should not happened, intent.getIntExtra(\"itemID\", -1) returned an id that was not from db"
                            )

                            throw Exception ("Expected item doesn't exists")
                        }
                    }
                    else{
                        //New Item
                        mItemTag = ItemTag()
                        mItemTag.id = itemId
                        mItemTag.barcode = itemCode
                        mItemTag.barcodeFormat = codeFormat ?: ""
                    }
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        initVar(view)

        // Initialize database and scraper
        db = DatabaseHelper.getInstance(requireContext())
        scraper = ItemTagWebScraper()

        updateViews()
        loadImageDisplay(mDisplayBarcode)

        setupListeners()
    }

    private fun initVar(view: View) {
        etLabel = view.findViewById(R.id.et_label)
        etBrand = view.findViewById(R.id.et_brand)
        tvBarcode = view.findViewById(R.id.tv_barcode)
        etDefaultPrice = view.findViewById(R.id.et_default_price)
        tvTagCreationDate = view.findViewById(R.id.tv_tag_date)
        ivImageDisplay = view.findViewById(R.id.iv_image)
        ivBarcodeDisplay = view.findViewById(R.id.iv_barcode)

        btnSave = view.findViewById(R.id.btn_save)
        btnClose = view.findViewById(R.id.btn_close)
        btnWebSearch = view.findViewById(R.id.btn_web_scrap)

//
//        //Note List related
//        noteRecyclerView = view.findViewById(R.id.tags_note_rv)
//        val noteList = listOf(
//            NoteTag(1,"", "", BigDecimal(0)),
//            NoteTag(2,"Note 2", "", BigDecimal(1.5)),
//            NoteTag(3,"Note 3", "", BigDecimal(50))
//        )
//        val noteAdapter = NoteAdapter(noteList)
//        noteRecyclerView.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = noteAdapter
//        }
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

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnWebSearch.setOnClickListener {
            if (isOnline(requireContext())) {
                Toast.makeText(context, "Experimental", Toast.LENGTH_SHORT).show()
                setWebScraps()
            } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            if (etLabel.text.isNotEmpty()) {
                mItemTag.label = etLabel.text.toString()
            }
            if (etBrand.text.isNotEmpty()) {
                mItemTag.brand = etBrand.text.toString()
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
            //Notify the list viewer?
            setFragmentResult("saveTagKey", bundleOf("tagID" to mItemTag.id))

            parentFragmentManager.popBackStack()
        }
    }

    private fun updateViews() {

        //barcode
        tvBarcode.text = mItemTag.barcode.plus(" " + if(mDisplayBarcode)"◈" else "❖")

        //label
        if (mItemTag.label.isNotBlank())
        {
            etLabel.hint = mItemTag.label
            etLabel.setText(etLabel.hint)
        }

        //brand
        if (mItemTag.brand.isNotBlank())
        {
            etBrand.hint = mItemTag.brand
            etBrand.setText(etBrand.hint)
        }

        //price
        etDefaultPrice.hint = mItemTag.moneyString()

        //date
        tvTagCreationDate.hint = mItemTag.createdOn
    }

    private fun loadImageDisplay(loadBarcodeBitmap: Boolean): Boolean {

        //Show barcode image
        if (loadBarcodeBitmap){
            //create the images only if it's not cashed
            if (ivBarcodeDisplay.drawable == null){

                //barcode format
                val format = BarcodeFormat.valueOf(mItemTag.barcodeFormat)

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

                //Nothing to gen
                if (mItemTag.barcode.isEmpty()){
                    Toast.makeText(requireContext(), "Cant generate barcode", Toast.LENGTH_SHORT).show()
                    return false
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
            }else{
                //From Url

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
                if (mItemTag.imageByteArray.isEmpty())  { mItemTag.imageByteArray   = scrap.imageByteArray  }
            }

            launch(Dispatchers.Main) {
                //Update views
                Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                loadImageDisplay(false)
                updateViews()
            }
        }
    }
}
