package com.ovisionik.memotag.data
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ItemTag (

    //ID Auto assigned in DB
    var id: Int = -1,

    //Label/Name/Title
    var label: String = "",

    //Brand of the tag
    var brand: String = "",

    //Barcode
    var barcode: String = "",

    //Format of the barcode
    var barcodeFormat: String = "",

    //default price
    var defaultPrice:Double = 0.0,

    //Tag Creation Date
    var createdOn: String = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").format(LocalDateTime.now()).toString(),

    //Item category ie food drinks alc etc..
    var category: String = "",

    //ByteArray representing the image
    var imageByteArray:ByteArray = ByteArray(0),

    //ImageURL
    var imageURL: String = "",

    var note: String = "",

    var priceTags: MutableList<PriceTag>? = null,

){
    fun moneyString(price:Double = defaultPrice):String{

        return getPriceFormattedString(price)
    }
}

private fun getPriceFormattedString(price: Number): String {
    if (price == 0.0){
        return "???"
    }

    val df = DecimalFormat("#,###,##0.00")
    return df.format(price).plus(" €")
}
