package com.ovisionik.memotag.scraper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.ovisionik.memotag.data.ItemTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
class BarcodeScraper {

    private var isBusy : Boolean = false

    /**
     * scrap the web for the barcode info
     * returns a ItemTag if found
     * returns null if not found
     */
    suspend fun asyncGetItemScrap(barcode:String): Result<ItemTag?> = withContext(Dispatchers.IO){

        if (isBusy){
            Log.d("isBusy", "getProductValues is already in progress")
            return@withContext Result.failure(Exception("scrapping is already in progress"))
        }

        isBusy = true
        val mItemTag = ItemTag()
        try {
            /*
            val filDir = filesDir
            val file = File(filDir, "doc.html")
            val document = Jsoup.parse(file)
            */

            val scraperBaseUrl:String = "https://www.barcodelookup.com/"
            val url = scraperBaseUrl+barcode

            // Fetch the HTML content from the URL
            val document = Jsoup
                .connect(url)
                .userAgent("Mozilla")
                .timeout(3 * 1000)
                .referrer("http://google.com")
                .get()

            // Extract the data you need from the HTML document
            val title = document.title() // Get the title of the webpage
            val body = document.body().text() // Get the text content of the webpage body

            //if not found
            if (title.contains("Not Found")){
                isBusy = false
                return@withContext Result.failure(Exception("Product not found"))
            }

            val pImageThumbs = document.getElementById("productImageThumbs")

            val docProductDetails = Jsoup.parse(document.getElementsByClass("col-50 product-details").html())

            // Image URL
            if (pImageThumbs != null) {
                // Find the first image tag within the selected element
                val firstImage = pImageThumbs.select("img").first()
                // Get the value of the "src" attribute of the first image
                mItemTag.imageURL = firstImage?.attr("src").toString()
            }

            // Name
            mItemTag.label = docProductDetails.select("h4").text()

            mItemTag.barcode = barcode

            //mItemTag.barcode = title.removePrefix("EAN ").toString().removeSuffix(" | Barcode Lookup")

            val brandDiv = document.select("div.product-text-label:contains(Brand: )").first()
            val brandSpan = brandDiv?.select("span.product-text")?.first()
            val brandTextWithLabel = brandSpan?.text()
            val brandTextWithoutLabel = brandTextWithLabel?.removePrefix("Brand: ")
            mItemTag.brand = brandTextWithoutLabel.toString()

            mItemTag.category = document.select("div.product-text-label:contains(Category: )").first()
                ?.select("div.product-text-label:contains(Category: )")?.first()
                ?.text()?.removePrefix("Category: ").orEmpty()

        } catch (e: IOException) {
            Log.d("Error", "${e.message}")
            e.printStackTrace()
            return@withContext Result.failure(e)
        }

        return@withContext Result.success(mItemTag)
    }

    suspend fun asyncGetBitmapFromURL(src: String?): Result<Bitmap?> = withContext(Dispatchers.IO) {
        var bmp:Bitmap? = null
        try {
            val url = URL(src)
            val connection =
                url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            bmp = BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext Result.success(bmp)
    }
}
