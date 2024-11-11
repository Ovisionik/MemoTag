package com.ovisionik.memotag.data
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var defaultPrice: Double = 0.00,

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
    fun moneyString(price:Number = defaultPrice):String{
        return getLocalPriceFormatted(price)
    }
}

private fun getLocalPriceFormatted(price: Number): String {
    if (price == 0.00){
        return "???"
    }

    val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return numberFormat.format(price)
}