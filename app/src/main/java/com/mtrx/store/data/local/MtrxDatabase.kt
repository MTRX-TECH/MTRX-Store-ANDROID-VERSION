package com.mtrx.store.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mtrx.store.data.local.dao.AppDao
import com.mtrx.store.data.local.entity.AppEntity

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MtrxDatabase : RoomDatabase() {
    abstract val appDao: AppDao
}
