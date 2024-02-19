package com.ovisionik.memotag.db

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ovisionik.memotag.data.ItemTag


class DatabaseHelper (mContext: Context) : SQLiteOpenHelper (
    /* context = */ mContext,
    /* name = */ DB_NAME,
    /* factory = */ null,
    /* version = */ DB_VERSION
) {
    // smiler to Static String name = ""
    companion object{

        //DATABASE
        private const val DB_NAME = "memo_tag.db"
        private const val DB_VERSION = 1

        //TABLES Item Tags
        private const val ITEM_TAG_TABLE_NAME = "itemTags"

        //Item tag fields
        private const val ITEM_TAG_ID = "ID"
        private const val ITEM_TAG_BARCODE = "BARCODE"
        private const val ITEM_TAG_BARCODE_FORMAT = "BARCODE_FORMAT"
        private const val ITEM_TAG_IMAGE_BYTES = "IMAGE_BYTES"
        private const val ITEM_TAG_CATEGORY = "CATEGORY"
        private const val ITEM_TAG_LABEL = "LABEL"
        private const val ITEM_TAG_PRICE = "PRICE"
        private const val ITEM_TAG_CREATED_ON = "CREATION_DATE"

        //Table Prices Tags
        private const val PRICE_TAG_TABLE_NAME = "tagPrices"
        private const val PRICE_TAG_ID = "ID"
        private const val PRICE_TAG_ITEM_TAG_ID = "ITEM_TAG_ID"
        private const val PRICE_TAG_PRICE = "PRICE"
        private const val PRICE_TAG_LABEL = "LABEL"
        private const val PRICE_TAG_NOTE = "NOTE"
        private const val PRICE_TAG_CREATED_ON = "CREATION_DATE"
    }
    override fun onCreate(db: SQLiteDatabase?) {

        /* Tables creations */

        val createTableTags = "CREATE TABLE IF NOT EXISTS $ITEM_TAG_TABLE_NAME (" +
                "$ITEM_TAG_ID INTEGER PRIMARY KEY, " +
                "$ITEM_TAG_BARCODE TEXT, " +
                "$ITEM_TAG_BARCODE_FORMAT VARCHAR(10), " +
                "$ITEM_TAG_CATEGORY VARCHAR(10), " +
                "$ITEM_TAG_IMAGE_BYTES BLOB, " +
                "$ITEM_TAG_LABEL VARCHAR(50)," +
                "$ITEM_TAG_PRICE REAL, " +
                "$ITEM_TAG_CREATED_ON VARCHAR(20)" +
                ")"

        val createTablePrices = "CREATE TABLE IF NOT EXISTS $PRICE_TAG_TABLE_NAME (" +
                "$PRICE_TAG_ID INTEGER PRIMARY KEY, " +
                "$PRICE_TAG_ITEM_TAG_ID INTEGER, " +
                "$PRICE_TAG_LABEL VARCHAR(50), " +
                "$PRICE_TAG_NOTE TEXT, " +
                "$PRICE_TAG_PRICE REAL, " +
                "$PRICE_TAG_CREATED_ON VARCHAR(20)" +
                ")"

        //Execute SQL create table commands

        db?.execSQL(createTableTags)
        db?.execSQL(createTablePrices)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        //Table deletion
        db?.execSQL("DROP TABLE IF EXISTS $ITEM_TAG_TABLE_NAME")
        //Table recreation
        onCreate(db)
    }

    /**
     * Return list count
     */
    fun getItemTagsCount(): Long {
        val db = this.readableDatabase
        return DatabaseUtils.queryNumEntries(db, ITEM_TAG_TABLE_NAME)
    }

    /**
     * Insert a new tag item in the DB
     */
    fun insertItemTag(tag: ItemTag): Boolean {

        //Get a writable database instance
        val db = this.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(ITEM_TAG_BARCODE, tag.barcode)
        contentValues.put(ITEM_TAG_BARCODE_FORMAT, tag.barcodeFormat)
        contentValues.put(ITEM_TAG_IMAGE_BYTES, tag.imageByteArray)
        contentValues.put(ITEM_TAG_CATEGORY, tag.category)
        contentValues.put(ITEM_TAG_LABEL, tag.label)
        contentValues.put(ITEM_TAG_PRICE, tag.defaultPrice)
        contentValues.put(ITEM_TAG_CREATED_ON, tag.createdOn)

        //INSERT INTO tags(barcode, label, price, Creation_date) values(tag.barcode, tag.label ...)
        val result = db.insert(ITEM_TAG_TABLE_NAME, null, contentValues)

        //close the db
        db.close()

        return result.toInt() != -1
    }

    /**
     * Update an existing Tag in the DB
     */
    fun updateTag(tag: ItemTag): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        //Check id
        val id = tag.id.toString()

        if (id.isEmpty()){ return false }

        contentValues.put(ITEM_TAG_ID, tag.id)

        contentValues.put(ITEM_TAG_CATEGORY, tag.category)

        contentValues.put(ITEM_TAG_LABEL, tag.label)

        contentValues.put(ITEM_TAG_PRICE, tag.defaultPrice)

        db.update(ITEM_TAG_TABLE_NAME, contentValues, "ID = ?", arrayOf(tag.id.toString()))

        db.close()
        return true
    }

    /**
     * Delete the ItemTag Data from DB
     */
    fun deleteTag(tag: ItemTag) : Boolean {

        val id = tag.id
        val db = this.writableDatabase
        val res = db.delete(ITEM_TAG_TABLE_NAME,"ID= ? ", arrayOf(id.toString()))

        db.close()

        return res > 0
    }

    /**
     * Get a list of all tag item stored in db
     * returns a List<ItemTag>
     */
    fun getAllTags(): List<ItemTag>{

        val tags = mutableListOf<ItemTag>()

        val db = this.readableDatabase

        val query = "SELECT * FROM $ITEM_TAG_TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
            val barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
            val barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
            val byteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
            val category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
            val label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
            val createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))

            val it = ItemTag()

            it.id = id
            it.barcode = barcode
            it.barcodeFormat = barcodeFormat
            it.label = label
            it.defaultPrice = price
            it.createdOn = createdOn
            it.imageByteArray = byteArray
            it.category = category

            tags.add(it)
        }
        cursor.close()
        db.close()

        return tags
    }

    /**
     * returns a Tag_Item
     */
    fun findItemTagByID(id: Int): ItemTag? {

        val itemTag:ItemTag = ItemTag()

        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $ITEM_TAG_TABLE_NAME WHERE $ITEM_TAG_ID = ?"

        val cursor = db.rawQuery(selectQuery, arrayOf(id.toString()))

        if (cursor.count > 0){
            cursor.moveToFirst()
            do {
                itemTag.id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
                itemTag.barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
                itemTag.barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
                itemTag.imageByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
                itemTag.category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
                itemTag.label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
                itemTag.defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
                itemTag.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }

        db.close()

        return if (itemTag.id == -1)
            null
        else
            itemTag
    }

    /**
     * returns a Tag_Item
     */
    fun findTagByBarcode(barcode: String): ItemTag? {

        val itemTag:ItemTag = ItemTag()

        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $ITEM_TAG_TABLE_NAME WHERE $ITEM_TAG_BARCODE = ?"

        val cursor = db.rawQuery(selectQuery, arrayOf(barcode))

        if (cursor.count > 0){
            cursor.moveToFirst()
            do {
                itemTag.id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
                itemTag.barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
                itemTag.barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
                itemTag.category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
                itemTag.label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
                itemTag.imageByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
                itemTag.defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
                itemTag.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }
        cursor.close()
        db.close()

        return if (itemTag.id == -1) null else itemTag
    }

    fun tagBarcodeExists(barcode: String): Boolean {

        val item = findTagByBarcode(barcode)

        return item != null
    }
}
