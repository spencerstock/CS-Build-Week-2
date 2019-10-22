package com.spencerstock.lambdahunt.Model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.spencerstock.lambdahunt.Room.ListConverter

@Entity(tableName = "rooms")
data class Room(

    @PrimaryKey
    var room_id : Int,
    var title : String,
    var description : String,
    var coordinates : String,
    var elevation : Int,
    var terrain : String,
    @Ignore
    var players : List<String>?,
    @Ignore
    var items : List<String>?,
    @Ignore
    var exits : List<String>?,
    var cooldown : Int,
    @Ignore
    var errors : List<String>?,
    @Ignore
    var messages : List<String>?
)
{
    constructor(room_id: Int) : this(room_id,"","","",0,"",null,null,null,0,null,null)
}