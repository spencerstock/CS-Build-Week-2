package com.spencerstock.lambdahunt.Retrofit

import com.spencerstock.lambdahunt.Model.Direction
import com.spencerstock.lambdahunt.Model.Room
import com.spencerstock.lambdahunt.Model.WiseDirection
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface BackendAPI {
    @get:Headers(
        "Authorization: token 89db1ddef40b9c538dac43796bbce93001a3ee49"
    )
    @get:GET("adv/init")
    val rooms: Observable<Room>


    @Headers(
        "Content-Type: application/json",
        "Authorization: token 89db1ddef40b9c538dac43796bbce93001a3ee49"
    )
    @POST("adv/move")
    fun move(@Body body: Direction): Observable<Room>

    @Headers(
        "Content-Type: application/json",
        "Authorization: token 89db1ddef40b9c538dac43796bbce93001a3ee49"
    )
    @POST("adv/move")
    fun wiseMove(@Body body: WiseDirection): Observable<Room>

}