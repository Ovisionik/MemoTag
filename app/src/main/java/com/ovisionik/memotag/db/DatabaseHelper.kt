package com.ovisionik.memotag.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ovisionik.memotag.data.TagItem


class DatabaseHelper (mContext: Context) : SQLiteOpenHelper (
    /* context = */ mContext,
    /* name = */ DB_NAME,
    /* factory = */ null,
    /* version = */ DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase?) {
        //Tables creation
        val createTableTags = "CREATE TABLE IF NOT EXISTS $TAG_TABLE_NAME (" +
                "$TAG_ID INTEGER PRIMARY KEY , " +
                "$TAG_BARCODE TEXT, " +
                "$TAG_LABEL TEXT," +
                "$TAG_PRICE REAL, " +
                "$TAG_CREATED_ON TEXT)"
/*
    """
            CREATE TABLE $TAG_TABLE_NAME(
                $TAG_ID INTEGER PRIMARY KEY,
                $BARCODE TEXT,
                $LABEL TEXT,
                $PRICE REAL,
                $CREATED_ON TEXT
            )
        """.trimIndent()
* */
        db?.execSQL(createTableTags)
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        //Table deletion
        db?.execSQL("DROP TABLE IF EXISTS $TAG_TABLE_NAME")

        //Table recreation
        onCreate(db)
    }

    //TODO DELETE
    //DEBUGONLY
    fun addTagDEBUG(tag: TagItem): String {

        //Get a writable database instance
        val db = this.writableDatabase

        val cv = ContentValues()
        cv.put(TAG_BARCODE, tag.barcode)
        cv.put(TAG_LABEL, tag.label)
        cv.put(TAG_PRICE, tag.price)
        cv.put(TAG_CREATED_ON, tag.createdOn)

        //INSERT INTO tags(barcode, label, price, Creation_date) values(tag.barcode, tag.label ...)

        var err = ""

        try {
            val result = db.insertOrThrow(TAG_TABLE_NAME, null, cv)
        }
        catch (e: android.database.SQLException){
            err = e.message.toString()
        }

        //close the db
        db.close()

        return err
    }

    //Return list count
    fun getTableCount(): Long {
        val db = this.readableDatabase
        val count = DatabaseUtils.queryNumEntries(db, TAG_TABLE_NAME)

        return count
    }

    /**
     * insert data
     * Insert a new tag item in db
     */
    fun insertTag(tag: TagItem): Boolean {

        //Get a writable database instance
        val db = this.writableDatabase

        val cv = ContentValues()
        cv.put(TAG_BARCODE, tag.barcode)
        cv.put(TAG_LABEL, tag.label)
        cv.put(TAG_PRICE, tag.price)
        cv.put(TAG_CREATED_ON, tag.createdOn)

        //INSERT INTO tags(barcode, label, price, Creation_date) values(tag.barcode, tag.label ...)
        val result = db.insert(TAG_TABLE_NAME, null, cv)

        //close the db
        db.close()

        return result.toInt() != -1
    }

    /**
     * update a Tag in DB
     */
    fun updateTag(tag: TagItem): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        //Check id
        val id = tag?.id.toString()

        if (id.isNullOrEmpty()){
            return false
        }

        contentValues.put(TAG_ID, tag.id)
        contentValues.put(TAG_BARCODE, tag.barcode)
        contentValues.put(TAG_LABEL, tag.label)
        contentValues.put(TAG_PRICE, tag.price)
        contentValues.put(TAG_CREATED_ON, tag.createdOn)

        db.update(TAG_TABLE_NAME, contentValues, "ID = ?", arrayOf(tag.id.toString()))

        db.close()
        return true
    }

    /**
     * delete the TagData from DB
     */
    fun deleteTag(tag: TagItem) : Int {

        val id = tag?.id.toString()

        val db = this.writableDatabase
        return db.delete(TAG_TABLE_NAME,"ID = ?", arrayOf(id))
    }

    /**
     * Get a list of all tag item stored in db
     * returns a List<TagItem>
     */
    fun getAllTags(): List<TagItem>{

        val tagList = mutableListOf<TagItem>()

        val db = this.readableDatabase

        val query = "SELECT * FROM $TAG_TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext()){
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(TAG_ID))
            val barcode = cursor.getString(cursor.getColumnIndexOrThrow(TAG_BARCODE))
            val label = cursor.getString(cursor.getColumnIndexOrThrow(TAG_LABEL))
            val price = cursor.getString(cursor.getColumnIndexOrThrow(TAG_PRICE)).toDouble()
            val createdOn = cursor.getString(cursor.getColumnIndexOrThrow(TAG_CREATED_ON))

            tagList.add(TagItem(id,barcode,label,price,null,createdOn))
        }
        cursor.close()
        db.close()

        return tagList
    }
    // TODO:


    /**
     * returns a Tag_Item
     */
    fun findTagByBarcode(barcode: String): TagItem? {

        var tagItem:TagItem = TagItem("","",0.0, "")

        val db = this.readableDatabase

        val selectQuery = "SELECT * FROM $TAG_TABLE_NAME WHERE $TAG_BARCODE=?"

        val cursor = db.rawQuery(selectQuery, arrayOf(barcode))

        if (cursor.count > 0){
            cursor.moveToFirst()
            do {
                tagItem.id = cursor.getInt(cursor.getColumnIndexOrThrow(TAG_ID))
                tagItem.barcode = cursor.getString(cursor.getColumnIndexOrThrow(TAG_BARCODE))
                tagItem.label = cursor.getString(cursor.getColumnIndexOrThrow(TAG_LABEL))
                tagItem.price = cursor.getDouble(cursor.getColumnIndexOrThrow(TAG_PRICE))
                tagItem.createdOn = cursor.getString(cursor.getColumnIndexOrThrow(TAG_CREATED_ON))
            } while ((cursor.moveToNext()))
        }

        db.close()

        return if (tagItem.id == -1) {
            null
        } else{
            tagItem
        }
    }

    fun tagBarcodeExists(barcode: String): Boolean {

        val item = findTagByBarcode(barcode)

        return item != null
    }

    // smiler to Static String name = ""
    companion object{
        private val DB_NAME = "tag_items.db"
        private val DB_VERSION = 1

        //TAGITEM
        private val TAG_TABLE_NAME = "tags"
        private val TAG_ID = "ID"
        private val TAG_BARCODE = "BARCODE"
        private val TAG_LABEL = "LABEL"
        private val TAG_PRICE = "PRICE"
        private val TAG_CREATED_ON = "CREATION_DATE"
    }
}
