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
    suspend fun getItemScrapAsync(barcode:String): Result<ItemTag> = withContext(Dispatchers.IO){

        if (isBusy){
            Log.d("isBusy", "getProductValues is already in progress")
            return@withContext Result.failure(Exception("scrapping is already in progress"))
        }

        isBusy = true
        val mItemTag = ItemTag()

        try {
            Log.d("Scrapper:", "Start - web scrapper is started")

            /*
            val filDir = filesDir
            val file = File(filDir, "doc.html")
            val document = Jsoup.parse(file)
            */

            val scraperBaseUrl = "https://www.barcodelookup.com/"
            val url = scraperBaseUrl+barcode

            // Fetch the HTML content from the URL
            val document = Jsoup
                .connect(url)
                .userAgent("Mozilla")
                .timeout(3 * 1000)
                .referrer("http://google.com")
                .get()

            Log.d("Scrapper:", "got document")

            // Extract the data you need from the HTML document
            val title = document.title() // Get the title of the webpage
            //val body = document.body().text() // Get the text content of the webpage body

            //if not found
            if (title.contains("Not Found")){
                isBusy = false
                return@withContext Result.failure(Exception("Product not found"))
            }

            val pImageThumbs = document.getElementById("productImageThumbs")

            val docProductDetails = Jsoup.parse(document.getElementsByClass("col-50 product-details").html())

            var imgURL = "null"
            // Image URL
            if (pImageThumbs != null) {
                // Find the first image tag within the selected element
                val firstImage = pImageThumbs.select("img").first()
                // Get the value of the "src" attribute of the first image
                imgURL = firstImage?.attr("src").toString()
            }

            // Name
            val label = docProductDetails.select("h4").text()
            val brandDiv = document.select("div.product-text-label:contains(Brand: )").first()
            val brandSpan = brandDiv?.select("span.product-text")?.first()
            val brandTextWithLabel = brandSpan?.text()
            val brandTextWithoutLabel = brandTextWithLabel?.removePrefix("Brand: ")
            val brand = brandTextWithoutLabel.toString()

            val category = document.select("div.product-text-label:contains(Category: )").first()
                ?.select("div.product-text-label:contains(Category: )")?.first()
                ?.text()?.removePrefix("Category: ").orEmpty()

            mItemTag.barcode = barcode

            if (imgURL!="null")
                mItemTag.imageURL = imgURL

            if (label!="null")
                mItemTag.label = docProductDetails.select("h4").text()

            if (category!="null")
                mItemTag.category = category

            if (brand!="null")
                mItemTag.brand = brand

        } catch (e: IOException) {
            e.printStackTrace()
            isBusy = false
            Log.d("Scrapper:", "crash -> ${e.message}")
            return@withContext Result.failure(e)
        }

        Log.d("Scrapper:", "end - returning scraps")
        isBusy = false
        return@withContext Result.success(mItemTag)
    }

    suspend fun getBitmapFromUrlAsync(src: String?): Result<Bitmap> = withContext(Dispatchers.IO) {
        var bmp:Bitmap?
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
            return@withContext Result.failure(e)
        }
        return@withContext Result.success(bmp)
    }
}
