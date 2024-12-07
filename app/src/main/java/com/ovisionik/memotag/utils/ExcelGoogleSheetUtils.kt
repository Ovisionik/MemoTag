package com.ovisionik.memotag.utils
import com.opencsv.CSVReader
import com.ovisionik.memotag.data.ItemTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

object ExcelGoogleSheetUtils {

    private var inProgress: Boolean = false

    /**
     * Export ItemTags data to an Excel file.
     * @param itemList The list of ItemTag objects to be exported.
     * @param outputFileName The name of the output Excel file.
     * @param onComplete A callback to handle completion (success/failure).
     */
    suspend fun exportToExcel(
        itemList: List<ItemTag>,
        outputFileName: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (inProgress) {
            onComplete(false, "Operation already in progress")
            return
        }
        inProgress = true
        try {
            withContext(Dispatchers.IO) {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("ItemTags")

                // App name and report title in the same row (Row 1)
                val appNameRow = sheet.createRow(0)
                appNameRow.createCell(0).setCellValue("My App Name - ItemTags Report")

                // Add the modification date in Row 1, Column C
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                appNameRow.createCell(2).setCellValue("Last Modified: $currentDate")

                // Blank row (Row 2)
                sheet.createRow(1)

                // Header row (Row 3)
                val headerRow = sheet.createRow(2)
                headerRow.createCell(0).setCellValue("Image Url")
                headerRow.createCell(1).setCellValue("Barcode")
                headerRow.createCell(2).setCellValue("Brand")
                headerRow.createCell(3).setCellValue("Name")
                headerRow.createCell(4).setCellValue("Category")

                // Write data rows (starting from Row 4)
                itemList.forEachIndexed { index, item ->
                    val row = sheet.createRow(3 + index) // Start from Row 4
                    row.createCell(0).setCellValue(item.imageURL)
                    row.createCell(1).setCellValue(item.barcode)
                    row.createCell(2).setCellValue(item.brand)
                    row.createCell(3).setCellValue(item.label)
                    row.createCell(4).setCellValue(item.category)
                }

                // Save the file
                val excelFile = File(outputFileName)
                FileOutputStream(excelFile).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()
            }
            onComplete(true, "Excel file saved successfully at: $outputFileName")
        } catch (e: Exception) {
            onComplete(false, e.message)
        } finally {
            inProgress = false
        }
    }

    suspend fun readGoogleSheet(
        publicSheetUrl: String,
        onComplete: (Boolean, String?, List<ItemTag>?) -> Unit
    ) {
        if (inProgress) {
            onComplete(false, "Operation already in progress", null)
            return
        }
        inProgress = true
        try {
            val data: List<ItemTag> = withContext(Dispatchers.IO) {
                // Fetch CSV data from the public URL
                val url = URL(publicSheetUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.inputStream.bufferedReader().use { reader ->
                    val csvData = reader.readText()
                    parseCSVData(csvData)
                }
            }

            onComplete(true, "Google Sheet read successfully", data)
        } catch (e: Exception) {
            onComplete(false, e.message, null)
        } finally {
            inProgress = false
        }
    }

    /**
     * Parse CSV data into a list of ItemTag.
     * @param csvData The raw CSV string fetched from Google Sheet.
     * @return A list of ItemTag objects.
     */
    private fun parseCSVData(csvData: String): List<ItemTag> {
        val itemList = mutableListOf<ItemTag>()
        val csvReader = CSVReader(StringReader(csvData))

        csvReader.use { reader ->
            val rows = reader.readAll()
            rows.forEachIndexed { index, row ->
                if (index <= 3) return@forEachIndexed // Skip header row
                if (row.size >= 4) {
                    itemList.add(
                        ItemTag(
                            imageURL = row[0],
                            barcode = row[1],
                            brand = row[2],
                            label = row[3],
                            category = row[4]
                        )
                    )
                }
            }
        }
        return itemList
    }

}
