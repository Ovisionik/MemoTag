package com.ovisionik.memotag.scraper
import android.util.Log
import com.ovisionik.memotag.data.ItemTag
import com.ovisionik.memotag.utils.BitmapUtils
import com.ovisionik.memotag.utils.BitmapUtils.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException


class ItemTagWebScraper {

    private var isBusy : Boolean = false
    private var mItemTag : ItemTag = ItemTag()

    suspend fun autoScrap(tag: ItemTag): Result<ItemTag> = withContext(Dispatchers.IO) {
        if (isBusy){
            Log.d("autoScrap.isBusy", "web scraper is busy")
            return@withContext Result.failure(Exception("scrapping is already in progress..."))
        }

        //Init vars
        isBusy = true
        mItemTag = tag.copy()

        ///Search in order of priorities:

        //scrapOpenFoodFacts
        async { scrapOpenFoodFacts(tag.barcode).onSuccess {
            mItemTag = it
        } }.await()
        //Try google image search
        if (mItemTag.imageByteArray.isEmpty() && mItemTag.imageURL.isBlank()){
            async {
                mItemTag.imageURL = googleImageSearch(mItemTag.barcode).orEmpty()
            }.await()
        }

        //scrapBarcodeLookup
        if (tag.hashCode() == mItemTag.hashCode()){
            async{
                val res = scrapBarcodeLookup(mItemTag.barcode)
                res.onSuccess {
                    mItemTag = it
                }
            }.await()
        }

        //Set Image ByteArray
        if (mItemTag.imageByteArray.isEmpty() && mItemTag.imageURL.isNotBlank()){
            async {
                BitmapUtils.getBitmapFromUrlAsync(mItemTag.imageURL)?.
                also { mItemTag.imageByteArray = it.toByteArray() } }
            .await()
        }

        //done free the busy state
        isBusy = false

        if (tag.hashCode() != mItemTag.hashCode()){
            return@withContext Result.success(mItemTag)
        }
        else{
            return@withContext Result.failure(Exception("failed to scrap data"))
        }
    }

    private suspend fun scrapOpenFoodFacts(searchStr: String):Result<ItemTag> = withContext(Dispatchers.IO){

        val itemTag = mItemTag.copy()

        //Look for <div id="prodInfos" >
        val searchUrl = "https://fr.openfoodfacts.org/cgi/search.pl?search_terms=${searchStr.replace(" ", "+")}&search_simple=1&action=process" //Replace " " by +
        val doc = async { getJSoupAsync(searchUrl) }.await()?:
            return@withContext Result.failure(Exception("Failed to get JsoupDoc from OpenFoodFacts"))

        // Check if a div with id="prodInfos" exists
        val prodInfo = doc.getElementById("prodInfos")
            ?: return@withContext Result.failure(Exception("The item doesn't exists in OpenFoodFacts"))

        val imageURL = prodInfo.select(".product_image").attr("src").orEmpty()

        // Get the title from property="food:name"
        val title = prodInfo.select("h2[property=food:name]").text().orEmpty()

        // Get the first brand name from id="field_brands"
        val brand = prodInfo.select("#field_brands_value").first()?.text().orEmpty()

        // Get the first category from id="field_categories"
        val category = prodInfo.select("#field_categories_value a").first()?.text().orEmpty()

        //Remove brand name from title
        // Split the text by "-"
        val splits = title.split(" - ")

        // Remove the last part if it contains "The brand"
         val nameNoBrand= splits.mapIndexed { index, part ->
            if (index == splits.size - 1 && part.contains(brand)) {
                part.substringBeforeLast(" - ")
            } else {
                part
            }
        }.joinToString(" - ")

        //remove any white space b/w a number and a single char
        val contractedName = nameNoBrand.replace("(\\d+(\\.\\d+)?) (\\w)".toRegex()) { match ->
            match.groupValues[1] + match.groupValues[3]
        }

        itemTag.label = contractedName

        itemTag.brand = brand

        itemTag.category = category

        itemTag.imageURL = imageURL

        return@withContext Result.success(itemTag)
    }

    /**
     * scrap the web for the barcode info
     * returns a ItemTag if found
     * returns null if not found
     */
    private suspend fun scrapBarcodeLookup(barcode:String): Result<ItemTag> = withContext(Dispatchers.IO){

        val itemTag = mItemTag.copy()

        try {
            Log.d("Scrapper:", "Start - web scrapper is started")

            val scraperBaseUrl = "https://www.barcodelookup.com/"
            val url = scraperBaseUrl+barcode

            // Fetch the HTML content from the URL
            val document = async { getJSoupAsync(url) }.await()
                ?: return@withContext Result.failure(Exception("Failed to get JsoupDoc at BarcodeLookup"))

            // Extract the data you need from the HTML document
            val title = document.title() // Get the title of the webpage
            //val body = document.body().text() // Get the text content of the webpage body

            //if not found
            if (title.contains("Not Found")) {
                return@withContext Result.failure(Exception("Product not found"))
            }

            val docProductDetails = Jsoup.parse(document.getElementsByClass("col-50 product-details").html())

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

            itemTag.barcode = barcode

            if (label!="null")
                itemTag.label = docProductDetails.select("h4").text()

            if (category!="null")
                itemTag.category = category

            if (brand!="null")
                itemTag.brand = brand

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("Scrapper:", "crash -> ${e.message}")
            return@withContext Result.failure(e)
        }

        Log.d("Scrapper:", "end - returning scraps")
        return@withContext Result.success(itemTag)
    }

    private suspend fun googleImageSearch(searchStr: String ): String? = withContext(Dispatchers.IO){

        val searchUrl = "https://www.google.com/search?tbm=isch&q=${searchStr.replace(" ", "+")}" //Replace " " by +

        val doc = async { getJSoupAsync(searchUrl) }.await()

        if (doc != null){
            val imgTags = doc.select("img[alt='']")
            imgTags.first()?.attr("src")?.also { return@withContext it }
        }
        return@withContext null
    }

    private suspend fun getJSoupAsync(strURL: String): Document? = withContext(Dispatchers.IO){
        try {
            val userAgent ="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"
            val document:Document = Jsoup
                .connect(strURL)
                .userAgent(userAgent)
                .timeout(3 * 1000)
                .get()
            return@withContext document
        }catch (e: Exception){
            e.printStackTrace()
            Log.d("JSoup:", "Exception -> ${e.message}")
            return@withContext null
        }
    }
}
