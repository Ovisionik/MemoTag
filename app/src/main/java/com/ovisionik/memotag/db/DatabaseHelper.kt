package com.ovisionik.memotag.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ovisionik.memotag.data.ItemTag

class DatabaseHelper private constructor(mContext: Context) : SQLiteOpenHelper(
    mContext,
    DB_NAME,
    null,
    DB_VERSION
) {

    companion object {
        private const val DB_NAME = "memo_tag.db"
        private const val DB_VERSION = 2

        // Define the singleton instance variable
        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        // Get the singleton instance of DatabaseHelper
        fun getInstance(context: Context): DatabaseHelper {
            // Return the existing instance if it’s already initialized
            return INSTANCE ?: synchronized(this) {
                // Create a new instance if it has not been initialized yet
                val instance = DatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        // Constants for table and column names
        private const val ITEM_TAG_TABLE_NAME = "itemTags"
        private const val ITEM_TAG_ID = "ID"
        private const val ITEM_TAG_LABEL = "LABEL"
        private const val ITEM_TAG_BRAND = "BRAND"
        private const val ITEM_TAG_BARCODE = "BARCODE"
        private const val ITEM_TAG_BARCODE_FORMAT = "BARCODE_FORMAT"
        private const val ITEM_TAG_IMAGE_BYTES = "IMAGE_BYTES"
        private const val ITEM_TAG_IMAGE_URL = "IMAGE_URL"
        private const val ITEM_TAG_CATEGORY = "CATEGORY"
        private const val ITEM_TAG_PRICE = "PRICE"
        private const val ITEM_TAG_CREATED_ON = "CREATION_DATE"

        private const val PRICE_TAG_TABLE_NAME = "tagPrices"
        private const val PRICE_TAG_ID = "ID"
        private const val PRICE_TAG_ITEM_TAG_ID = "ITEM_TAG_ID"
        private const val PRICE_TAG_PRICE = "PRICE"
        private const val PRICE_TAG_LABEL = "LABEL"
        private const val PRICE_TAG_NOTE = "NOTE"
        private const val PRICE_TAG_CREATED_ON = "CREATION_DATE"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableTags = "CREATE TABLE IF NOT EXISTS $ITEM_TAG_TABLE_NAME (" +
                "$ITEM_TAG_ID INTEGER PRIMARY KEY, " +
                "$ITEM_TAG_LABEL VARCHAR(50)," +
                "$ITEM_TAG_BRAND VARCHAR(50), " +
                "$ITEM_TAG_BARCODE TEXT, " +
                "$ITEM_TAG_BARCODE_FORMAT VARCHAR(10), " +
                "$ITEM_TAG_CATEGORY VARCHAR(10), " +
                "$ITEM_TAG_IMAGE_BYTES BLOB, " +
                "$ITEM_TAG_IMAGE_URL TEXT, " +
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

        db?.execSQL(createTableTags)
        db?.execSQL(createTablePrices)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("ALTER TABLE $ITEM_TAG_TABLE_NAME ADD COLUMN $ITEM_TAG_BRAND VARCHAR(50) DEFAULT ''")
        db?.execSQL("ALTER TABLE $ITEM_TAG_TABLE_NAME ADD COLUMN $ITEM_TAG_IMAGE_URL TEXT DEFAULT ''")
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
        contentValues.put(ITEM_TAG_LABEL, tag.label)
        contentValues.put(ITEM_TAG_BRAND, tag.brand)
        contentValues.put(ITEM_TAG_BARCODE, tag.barcode)
        contentValues.put(ITEM_TAG_BARCODE_FORMAT, tag.barcodeFormat)
        contentValues.put(ITEM_TAG_IMAGE_BYTES, tag.imageByteArray)
        contentValues.put(ITEM_TAG_IMAGE_URL, tag.imageURL)
        contentValues.put(ITEM_TAG_CATEGORY, tag.category)
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

        contentValues.put(ITEM_TAG_CATEGORY, tag.category)
        contentValues.put(ITEM_TAG_LABEL, tag.label)
        contentValues.put(ITEM_TAG_BRAND, tag.brand)
        contentValues.put(ITEM_TAG_PRICE, tag.defaultPrice)
        contentValues.put(ITEM_TAG_IMAGE_URL, tag.imageURL)
        contentValues.put(ITEM_TAG_IMAGE_BYTES, tag.imageByteArray)
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
            val label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
            val brand = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BRAND))
            val barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
            val barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
            val imageURL = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_URL))
            val byteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
            val category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
            val createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))

            val it = ItemTag()

            it.id = id
            it.label = label
            it.brand = brand
            it.barcode = barcode
            it.barcodeFormat = barcodeFormat
            it.defaultPrice = price
            it.createdOn = createdOn
            it.imageURL = imageURL
            it.imageByteArray = byteArray
            it.category = category

            tags.add(it)
        }
        cursor.close()
        db.close()

        return tags
    }

    fun fetchItems(limit: Int, offset: Int): List<ItemTag> {
        val tags = mutableListOf<ItemTag>()
        val db = this.readableDatabase

        // Updated query to include LIMIT and OFFSET
        val query = "SELECT * FROM $ITEM_TAG_TABLE_NAME LIMIT ? OFFSET ?"
        val cursor = db.rawQuery(query, arrayOf(limit.toString(), offset.toString()))

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
            val label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
            val brand = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BRAND))
            val barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
            val barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
            val imageURL = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_URL))
            val byteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
            val category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
            val createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))

            val itemTag = ItemTag()

            itemTag.id = id
            itemTag.label = label
            itemTag.brand = brand
            itemTag.barcode = barcode
            itemTag.barcodeFormat = barcodeFormat
            itemTag.defaultPrice = price
            itemTag.createdOn = createdOn
            itemTag.imageURL = imageURL
            itemTag.imageByteArray = byteArray
            itemTag.category = category

            tags.add(itemTag)
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
                itemTag.label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
                itemTag.brand = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BRAND))
                itemTag.barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
                itemTag.barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
                itemTag.imageURL = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_URL))
                itemTag.imageByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
                itemTag.category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
                itemTag.defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
                itemTag.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }

        cursor.close()
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

        val itemTag = ItemTag()

        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $ITEM_TAG_TABLE_NAME WHERE $ITEM_TAG_BARCODE = ?"

        val cursor = db.rawQuery(selectQuery, arrayOf(barcode))

        if (cursor.count > 0){
            cursor.moveToFirst()
            do {
                itemTag.id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
                itemTag.label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
                itemTag.brand = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BRAND))
                itemTag.barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
                itemTag.barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT))
                itemTag.imageURL = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_URL))
                itemTag.imageByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES))
                itemTag.category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY))
                itemTag.defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
                itemTag.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }
        cursor.close()
        db.close()

        return if (itemTag.id == -1) null else itemTag
    }

    fun tagExists(tag: ItemTag): Boolean {
        val dbTag = findItemTagByID(tag.id)
        return (dbTag?.id == tag.id)
    }
    fun tagBarcodeExists(barcode: String): Boolean {

        val item = findTagByBarcode(barcode)

        return item != null
    }

    fun getTagsPaginated(limit: Int, offset: Int): List<ItemTag> {
        val db = readableDatabase
        val itemList = mutableListOf<ItemTag>()
        val query = "SELECT * FROM $ITEM_TAG_TABLE_NAME ORDER BY id DESC LIMIT $limit OFFSET $offset"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val item = cursorToItemTag(cursor)
                itemList.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return itemList
    }

    private fun cursorToItemTag(cursor: Cursor): ItemTag {
        return ItemTag(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID)),
            label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL)),
            brand = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BRAND)),
            barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE)),
            barcodeFormat = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE_FORMAT)),
            defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE)),
            createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CATEGORY)),
            imageByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_BYTES)),
            imageURL = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_IMAGE_URL)),
            note = "" // Add default or custom handling for note if it's not stored in the database.
        )
    }

}