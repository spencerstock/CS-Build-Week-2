package com.spencerstock.lambdahunt

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.spencerstock.lambdahunt.Model.Direction
import com.spencerstock.lambdahunt.Model.Room
import com.spencerstock.lambdahunt.Retrofit.BackendAPI
import com.spencerstock.lambdahunt.Retrofit.RetrofitClient
import com.spencerstock.lambdahunt.Room.RoomsDatabase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.lang.Exception
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity() {

    private lateinit var backendAPI: BackendAPI
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var roomsDatabase: RoomsDatabase
    private lateinit var roomsList: MutableList<Room>
    private var currentRoom: Room? = null
    private lateinit var timer: Timer

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        roomsDatabase = RoomsDatabase.getAppDatabase(this)

        roomsList = roomsDatabase.roomsDao().getAllRooms()
        val retrofit = RetrofitClient.instance
        backendAPI = retrofit.create(BackendAPI::class.java)
        fetchData()
        north.setOnClickListener {
            move("n")
        }
        south.setOnClickListener {
            move("s")
        }
        west.setOnClickListener {
            move("w")
        }
        east.setOnClickListener {
            move("e")
        }

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        north.setOnTouchListener(mDelayHideTouchListener)


    }

    private fun move(dir: String) {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(backendAPI.move(Direction(dir))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { room: Room -> handleData(room, dir) }
        )
    }

    private fun fetchData() {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(backendAPI.rooms
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { room: Room -> handleData(room) }

        )
    }

    private fun handleData(room: Room, direction: String? = null) {

        if (currentRoom != null) { //currentRoom is the now the room we came from, room is the room we moved to
            if (direction.equals("e")) {
                currentRoom!!.e_to = room.room_id
                room.w_to = currentRoom!!.room_id
            } else if (direction.equals("w")) {
                currentRoom!!.w_to = room.room_id
                room.e_to = currentRoom!!.room_id
            } else if (direction.equals("n")) {
                currentRoom!!.n_to = room.room_id
                room.s_to = currentRoom!!.room_id
            } else if (direction.equals("s")) {
                currentRoom!!.s_to = room.room_id
                room.n_to = currentRoom!!.room_id
            }
            insertRoom(updateRoom(currentRoom!!))
        }


        currentRoom = room
        insertRoom(updateRoom(room))
        displayRoom(room)
    }

    private fun insertRoom(currentRoom: Room) {
        roomsDatabase.roomsDao().insert(currentRoom)
        roomsList = roomsDatabase.roomsDao().getAllRooms()
    }

    private fun updateRoom(currentRoom: Room): Room {
        try {
            val dbCopyOfRoom = roomsDatabase.roomsDao().findRoomById(currentRoom.room_id)

            if (currentRoom.w_to == null) {
                currentRoom.w_to = dbCopyOfRoom.w_to
            }
            if (currentRoom.e_to == null) {
                currentRoom.e_to = dbCopyOfRoom.e_to
            }
            if (currentRoom.n_to == null) {
                currentRoom.n_to = dbCopyOfRoom.n_to
            }
            if (currentRoom.s_to == null) {
                currentRoom.s_to = dbCopyOfRoom.s_to
            }
        } catch (e: Exception) {
            println("Room attempted to update but was not found")
            println(e)
        }

        return currentRoom
    }

    private fun displayRoom(room: Room) {
        room_name.text = room.title
        room_desc.text = room.description
        room_exits.text = room.exits.toString()

        val timer = object : CountDownTimer(room.cooldown * 1000.toLong(), 100) {
            override fun onTick(millisUntilFinished: Long) {
                room_timer.text = (millisUntilFinished / 100).toString()
            }

            override fun onFinish() {
                room_timer.text = "Ready"
            }
        }
        timer.start()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
