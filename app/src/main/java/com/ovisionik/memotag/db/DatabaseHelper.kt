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
    override fun onCreate(db: SQLiteDatabase?) {

        /* Tables creations */

        val createTableTags = "CREATE TABLE IF NOT EXISTS $ITEM_TAG_TABLE_NAME (" +
                "$ITEM_TAG_ID INTEGER PRIMARY KEY, " +
                "$ITEM_TAG_BARCODE TEXT, " +
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

    //TODO DELETE
    //DEBUGONLY
    fun addTagDEBUG(tag: ItemTag): String {

        //Get a writable database instance
        val db = this.writableDatabase

        val mContentValues = ContentValues()
        mContentValues.put(ITEM_TAG_BARCODE, tag.barcode)
        mContentValues.put(ITEM_TAG_LABEL, tag.label)
        mContentValues.put(ITEM_TAG_PRICE, tag.defaultPrice)
        mContentValues.put(ITEM_TAG_CREATED_ON, tag.createdOn)

        //INSERT INTO tags(barcode, label, price, Creation_date) values(tag.barcode, tag.label ...)

        var err = ""

        try {
            val result = db.insertOrThrow(ITEM_TAG_TABLE_NAME, null, mContentValues)
        }
        catch (e: android.database.SQLException){
            err = e.message.toString()
        }

        //close the db
        db.close()

        return err
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

        val cv = ContentValues()
        cv.put(ITEM_TAG_BARCODE, tag.barcode)
        cv.put(ITEM_TAG_LABEL, tag.label)
        cv.put(ITEM_TAG_PRICE, tag.defaultPrice)
        cv.put(ITEM_TAG_CREATED_ON, tag.createdOn)

        //INSERT INTO tags(barcode, label, price, Creation_date) values(tag.barcode, tag.label ...)
        val result = db.insert(ITEM_TAG_TABLE_NAME, null, cv)

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
        val id = tag?.id.toString()

        if (id.isNullOrEmpty()){
            return false
        }

        contentValues.put(ITEM_TAG_ID, tag.id)
        contentValues.put(ITEM_TAG_BARCODE, tag.barcode)
        contentValues.put(ITEM_TAG_LABEL, tag.label)
        contentValues.put(ITEM_TAG_PRICE, tag.defaultPrice)
        contentValues.put(ITEM_TAG_CREATED_ON, tag.createdOn)

        db.update(ITEM_TAG_TABLE_NAME, contentValues, "ID = ?", arrayOf(tag.id.toString()))

        db.close()
        return true
    }

    /**
     * Delete the ItemTag Data from DB
     */
    fun deleteTag(tag: ItemTag) : Boolean {

        val id = tag?.id.toString()

        val db = this.writableDatabase

        val res = db.delete(ITEM_TAG_TABLE_NAME,"ID = ?", arrayOf(id))

        return res > 0
    }

    /**
     * Get a list of all tag item stored in db
     * returns a List<ItemTag>
     */
    fun getAllTags(): List<ItemTag>{

        val tagList = mutableListOf<ItemTag>()

        val db = this.readableDatabase

        val query = "SELECT * FROM $ITEM_TAG_TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
            val barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
            val label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
            val price = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE)).toDouble()
            val createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))

            tagList.add(ItemTag(id,barcode, "",label,price,null,createdOn))
        }
        cursor.close()
        db.close()

        return tagList
    }

    /**
     * returns a Tag_Item
     */
    fun findItemTagByID(id: Int): ItemTag? {

        var itemTag:ItemTag = ItemTag("", "", 0.0, "")

        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $ITEM_TAG_TABLE_NAME WHERE $ITEM_TAG_ID = ?"

        val cursor = db.rawQuery(selectQuery, arrayOf(id.toString()))

        if (cursor.count > 0){
            cursor.moveToFirst()
            do {
                itemTag.id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
                itemTag.barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
                itemTag.label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
                itemTag.defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
                itemTag.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }

        db.close()

        return if (itemTag.id == -1) {
            null
        } else{
            itemTag
        }
    }

    /**
     * returns a Tag_Item
     */
    fun findTagByBarcode(barcode: String): ItemTag? {

        var itemTag:ItemTag = ItemTag("", "", 0.0, "")

        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $ITEM_TAG_TABLE_NAME WHERE $ITEM_TAG_BARCODE = ?"

        val cursor = db.rawQuery(selectQuery, arrayOf(barcode))

        if (cursor.count > 0){
            cursor.moveToFirst()
            do {
                itemTag.id = cursor.getInt(cursor.getColumnIndexOrThrow(ITEM_TAG_ID))
                itemTag.barcode = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_BARCODE))
                itemTag.label = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_LABEL))
                itemTag.defaultPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ITEM_TAG_PRICE))
                itemTag.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(ITEM_TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }

        db.close()

        return if (itemTag.id == -1) {
            null
        } else{
            itemTag
        }
    }

    fun tagBarcodeExists(barcode: String): Boolean {

        val item = findTagByBarcode(barcode)

        return item != null
    }

    // smiler to Static String name = ""
    companion object{

        //DATABASE
        private val DB_NAME = "memo_tag.db"
        private val DB_VERSION = 1

        //TABLES Item Tags
        private val ITEM_TAG_TABLE_NAME = "tags"

        //Item tag fields
        private val ITEM_TAG_ID = "ID"
        private val ITEM_TAG_BARCODE = "BARCODE"
        private val ITEM_TAG_LABEL = "LABEL"
        private val ITEM_TAG_PRICE = "PRICE"
        private val ITEM_TAG_CREATED_ON = "CREATION_DATE"

        //Table Prices Tags
        private val PRICE_TAG_TABLE_NAME = "tagPrices"
        private val PRICE_TAG_ID = "ID"
        private val PRICE_TAG_ITEM_TAG_ID = "ITEM_TAG_ID"
        private val PRICE_TAG_PRICE = "PRICE"
        private val PRICE_TAG_LABEL = "LABEL"
        private val PRICE_TAG_NOTE = "NOTE"
        private val PRICE_TAG_CREATED_ON = "CREATION_DATE"
    }
}
