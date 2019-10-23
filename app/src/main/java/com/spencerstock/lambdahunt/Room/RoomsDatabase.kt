package com.spencerstock.lambdahunt.Room

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.spencerstock.lambdahunt.Model.Room
import androidx.room.Room.databaseBuilder
import androidx.room.TypeConverters
import com.spencerstock.lambdahunt.Retrofit.RetrofitClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


@Database(entities = arrayOf(Room::class), version = 5)
@TypeConverters(ListConverter::class)
abstract class RoomsDatabase: RoomDatabase() {

    abstract fun roomsDao(): RoomsDao



    companion object {

        private var INSTANCE: RoomsDatabase? = null
        fun getAppDatabase(context: Context): RoomsDatabase {
            if (INSTANCE == null) {
                INSTANCE = databaseBuilder(
                    context.getApplicationContext(),
                    RoomsDatabase::class.java,
                    "rooms-database"
                )
                    // allow queries on the main thread.
                    // Don't do this on a real app! See PersistenceBasicSample for an example.
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return INSTANCE!!

        }
    }



}