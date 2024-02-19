package com.ovisionik.memotag.data
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ItemTag (
    var id: Int = -1,
    //Barcode
    var barcode: String = "",

    //Format of the barcode
    var barcodeFormat: String = "",

    //Label/Name/Title
    var label: String = "",

    //TODO ADD IT TO THE DB
    //Brand of the tag
    var brand: String = "",

    //default price
    var defaultPrice:Double = 0.0,

    //Tag Creation Date
    var createdOn: String = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").format(LocalDateTime.now()).toString(),

    //Item category ie food drinks alc etc..
    var category: String = "",

    var imageByteArray:ByteArray = ByteArray(0),

    var imageURL: String = "",

    var note: String = "",

    var priceTags: MutableList<PriceTag>? = null,
){


}