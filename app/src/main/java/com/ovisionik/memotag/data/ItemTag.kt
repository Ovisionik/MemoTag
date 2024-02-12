package com.ovisionik.memotag.data
import android.graphics.Bitmap
import java.time.LocalDate

data class ItemTag (
    //Barcode
    var barcode: String,

    //Label/Name/Title
    var label: String,

    //default price
    var defaultPrice:Double = 0.0,

    //Tag Creation Date
    var createdOn: String = LocalDate.now().toString(),

    ) {
    //Id
    var id:Int = -1

    //Id
    var priceTags: MutableList<PriceTag>? = null

    //Item category
    var category:String = ""

    var batcodeFormat: String = ""

    var bitmapImg:Bitmap? = null

    constructor(id: Int, barcode: String, barcodeFormat:String = "", label: String, price: Double = 0.0, priceTags: List<PriceTag>?, createdOn: String): this(barcode, label, price,createdOn){
        this.id = id

        this.defaultPrice = price

        //TODO this.priceTags = priceTags.a
        this.category = ""

        this.batcodeFormat = barcodeFormat
    }
}
