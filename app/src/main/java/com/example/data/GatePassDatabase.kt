package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [VehicleRecord::class],
  version = 1,
  exportSchema = false,
)
abstract class GatePassDatabase : RoomDatabase() {
  abstract fun vehicleDao(): VehicleDao

  companion object {
    @Volatile
    private var INSTANCE: GatePassDatabase? = null

    fun getInstance(context: Context): GatePassDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          GatePassDatabase::class.java,
          "gatepass_verify.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
