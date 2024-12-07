package com.ovisionik.memotag.data

import android.os.Parcel
import android.os.Parcelable
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ItemTag(

    // ID Auto assigned in DB
    var id: Int = -1,

    // Label/Name/Title
    var label: String = "",

    // Brand of the tag
    var brand: String = "",

    // Barcode
    var barcode: String = "",

    // Format of the barcode
    var barcodeFormat: String = "",

    // Default price
    var defaultPrice: Double = 0.00,

    // Tag Creation Date
    var createdOn: String = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").format(LocalDateTime.now()).toString(),

    // Item category ie food drinks alc etc..
    var category: String = "",

    // ByteArray representing the image
    var imageByteArray: ByteArray = ByteArray(0),

    // ImageURL
    var imageURL: String = "",

    var note: String = "",

    //var noteTags: MutableList<NoteTag>? = null

) : Parcelable {

    // Constructor that reads from Parcel
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createByteArray() ?: ByteArray(0),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        //parcel.createTypedArrayList(NoteTag.CREATOR) ?: mutableListOf()
    )

    // Method to write the object to a Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(label)
        parcel.writeString(brand)
        parcel.writeString(barcode)
        parcel.writeString(barcodeFormat)
        parcel.writeDouble(defaultPrice)
        parcel.writeString(createdOn)
        parcel.writeString(category)
        parcel.writeByteArray(imageByteArray)
        parcel.writeString(imageURL)
        parcel.writeString(note)
        //parcel.writeTypedList(noteTags)
    }

    // Method that describes the contents (usually returns 0)
    override fun describeContents(): Int {
        return 0
    }

    // Companion object for the CREATOR
    companion object CREATOR : Parcelable.Creator<ItemTag> {
        override fun createFromParcel(parcel: Parcel): ItemTag {
            return ItemTag(parcel)
        }

        override fun newArray(size: Int): Array<ItemTag?> {
            return arrayOfNulls(size)
        }
    }

    // Function to format price as currency
    fun moneyString(price: Number = defaultPrice): String {
        return getLocalPriceFormatted(price)
    }
}

// Helper function for local price formatting
private fun getLocalPriceFormatted(price: Number): String {
    if (price == 0.00) {
        return "???"
    }

    val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return numberFormat.format(price)
}
