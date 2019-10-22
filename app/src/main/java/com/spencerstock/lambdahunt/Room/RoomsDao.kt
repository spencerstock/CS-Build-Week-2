package com.spencerstock.lambdahunt.Room

import androidx.room.*
import com.spencerstock.lambdahunt.Model.Room

@Dao
interface RoomsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(room: Room)
    @Update
    fun update(room: Room)
    @Query("SELECT * FROM rooms " + " WHERE room_id LIKE :id")
    fun findRoomById(id: Int): Room
    @Query("SELECT * FROM rooms")
    fun getAllRooms(): List<Room>
}