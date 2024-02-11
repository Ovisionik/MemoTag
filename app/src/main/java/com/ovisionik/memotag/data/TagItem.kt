package com.ovisionik.memotag.data
import java.math.BigDecimal
import java.time.LocalDate

data class TagItem (
    //ID
    //val id: Int, //Auto assigned by DB

    //Barcode
    var barcode: String,

    //Label/Name/Title
    var label: String,

    //Note
    //val note: String = "",

    //default price
    var price:Double = 0.0,

    //Tag Creation Date
    var createdOn: String = LocalDate.now().toString(),

) {
    //Id
    var id:Int = -1
    var priceTags: List<PriceTag>? = null

    constructor(id: Int, barcode: String, label: String, price: Double, priceTags: List<PriceTag>?, createdOn: String): this(barcode, label, price, createdOn){
        this.id = id
        this.priceTags = priceTags
    }
}

data class PriceTag(
    var price: BigDecimal = BigDecimal("0.0"),
    var note: String = "",
    var createdOn: LocalDate = LocalDate.now(),
)