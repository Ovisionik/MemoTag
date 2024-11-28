package com.ovisionik.memotag.utils


//TODO:WIP
object WorkbookHelper {
//    fun exportItemTagListToExcel(itemTagList: List<ItemTag>, file: File): Int {
//
//        try {
//            Log.d("ExportDebug", "Starting Excel export. Items: ${itemTagList.size}")
//            for (item in itemTagList) {
//                Log.d("ExportDebug", "Exporting ItemTag: $item")
//            }
//
//            // Create a new workbook and sheet
//            val workbook = XSSFWorkbook()
//            val sheet = workbook.createSheet("Item Tags")
//
//            // Create the header row
//            val headerRow = sheet.createRow(0)
//            headerRow.createCell(0).setCellValue("ID")
//            headerRow.createCell(1).setCellValue("Label")
//            headerRow.createCell(2).setCellValue("Brand")
//            headerRow.createCell(3).setCellValue("Barcode")
//            headerRow.createCell(4).setCellValue("Default Price")
//            headerRow.createCell(5).setCellValue("Created On")
//
//            // Populate the data rows
//            for ((index, itemTag) in itemTagList.withIndex()) {
//                val row = sheet.createRow(index + 1) // Data rows start at row 1
//                row.createCell(0).setCellValue((index + 1).toDouble()) // Assuming ID is a sequence
//                row.createCell(1).setCellValue(itemTag.label ?: "N/A")
//                row.createCell(2).setCellValue(itemTag.brand ?: "N/A")
//                row.createCell(3).setCellValue(itemTag.barcode ?: "N/A")
//                row.createCell(4).setCellValue(itemTag.defaultPrice)
//                row.createCell(5).setCellValue(itemTag.createdOn ?: "N/A")
//            }
//
//            // Set fixed column widths
//            sheet.setColumnWidth(0, 256 * 10) // ID column
//            sheet.setColumnWidth(1, 256 * 20) // Label column
//            sheet.setColumnWidth(2, 256 * 20) // Brand column
//            sheet.setColumnWidth(3, 256 * 30) // Barcode column
//            sheet.setColumnWidth(4, 256 * 15) // Default Price column
//            sheet.setColumnWidth(5, 256 * 20) // Created On column
//
//            // Write the workbook to the file
//            FileOutputStream(file).use { outputStream ->
//                workbook.write(outputStream)
//                workbook.close()
//                Log.d("Export", "Excel file created successfully at: ${file.absolutePath}")
//            }
//            return 1
//        } catch (e: Exception) {
//            Log.e("ExportError", "Error exporting Excel file", e)
//            return -1
//        }
//    }
}