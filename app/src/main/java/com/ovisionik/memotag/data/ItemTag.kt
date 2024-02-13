package com.ovisionik.memotag.data
import java.time.LocalDate

data class ItemTag (
    //Barcode
    var barcode: String,

    var barcodeFormat: String = "",

    //Label/Name/Title
    var label: String = "",

    //default price
    var defaultPrice:Double = 0.0,

    //Tag Creation Date
    var createdOn: String = LocalDate.now().toString(),

    var imageByteArray:ByteArray = ByteArray(0),
    ) {
    //Id
    var id:Int = -1

    //Id
    var priceTags: MutableList<PriceTag>? = null

    //Item category ie food drinks alc etc..
    var category:String = ""

    constructor(id: Int, barcode: String, barcodeFormat:String, label: String, price: Double = 0.0, priceTags: List<PriceTag>?, createdOn: String): this(barcode){
        this.id = id

        this.barcode = barcode

        this.label = label

        this.defaultPrice = price

        this.priceTags = priceTags?.toMutableList()

        this.createdOn = createdOn

        //TODO this.priceTags = priceTags.a
        this.category = ""

        this.barcodeFormat = barcodeFormat
    }
}
