package com.ovisionik.memotag

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.db.DatabaseHelper
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class SideMenuFragment : Fragment() {

    interface OnMenuItemSelectedListener {
        fun onMainContentChangeRequest(fragment: Fragment)
    }

    private var listener: OnMenuItemSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the host activity implements the interface
        if (context is OnMenuItemSelectedListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnMenuItemSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun onBtnClick(fragment: Fragment) {
        // Notify the MainActivity to switch to a new fragment
        listener?.onMainContentChangeRequest(fragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_side_menu, container, false)

        val btnHome = view.findViewById<Button>(R.id.menu_btn_home)
        val btnExport = view.findViewById<Button>(R.id.menu_btn_export_data)
        val btnImport = view.findViewById<Button>(R.id.menu_btn_import_data)
        val btnConfig = view.findViewById<Button>(R.id.menu_btn_settings)

        btnHome.setOnClickListener { onBtnClick(ListViewFragment()) }

        btnConfig.setOnClickListener{ onBtnClick(UserSettingsFragment()) }

        btnExport.setOnClickListener {

            val ls = DatabaseHelper.getInstance(requireContext()).getAllTags()
            writeToExcel(ls)

//            Toast.makeText(requireContext(), "Export: ${bd.count()} items", Toast.LENGTH_LONG)
//                .show()
        }

        btnImport.setOnClickListener{

        }

        return view
    }

    //TODO:Fine a place to "export"
    private fun testPOI(context: Context) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Test Sheet")

        // Create data
        val row = sheet.createRow(0)
        row.createCell(0).setCellValue("Hello, Apache POI!")
        row.createCell(1).setCellValue("Kotlin Rocks!")

        // Save the file in the app's private directory
        val excelFile = File(context.filesDir, "test.xlsx")
        FileOutputStream(excelFile).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        println("Excel file saved successfully at: ${excelFile.absolutePath}")
    }

    private fun writeToExcel(itemList: List<ItemTag>) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("ItemTags")

        // App name and report title in the same row (Row 1)
        val appNameRow = sheet.createRow(0)
        appNameRow.createCell(0).setCellValue("MemoTag - ItemTags Report")

//        // Add the modification date in Row 1, Column C
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
//        val currentDate = dateFormat.format(Date())
//        appNameRow.createCell(1).setCellValue("Last Modified: $currentDate") // Column B (Index 1)

        // Blank row (Row 2)
        sheet.createRow(1)

        // Header row (Row 3)
        val headerRow = sheet.createRow(2)
        headerRow.createCell(0).setCellValue("Image Url")
        headerRow.createCell(1).setCellValue("Barcode")
        headerRow.createCell(2).setCellValue("Brand")
        headerRow.createCell(3).setCellValue("Name")
        headerRow.createCell(4).setCellValue("Category")

        // Blank row (Row 4)
        sheet.createRow(3)

        // Write data rows (starting from Row 5)
        itemList.forEachIndexed { index, item ->
            val row = sheet.createRow(4 + index) // Start from Row 5
            row.createCell(0).setCellValue(item.imageURL)
            row.createCell(1).setCellValue(item.barcode)
            row.createCell(2).setCellValue(item.brand)
            row.createCell(3).setCellValue(item.label)
            row.createCell(4).setCellValue(item.category)
        }

        // Save the file
        val excelFile = File(requireContext().filesDir, "ItemTags.xlsx")
        FileOutputStream(excelFile).use { outputStream ->
            workbook.write(outputStream)
        }

        println("Excel file saved at: ${excelFile.absolutePath}")
    }

}