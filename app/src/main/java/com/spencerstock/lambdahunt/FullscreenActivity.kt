package com.spencerstock.lambdahunt

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.spencerstock.lambdahunt.Model.Direction
import com.spencerstock.lambdahunt.Model.Room
import com.spencerstock.lambdahunt.Model.Treasure
import com.spencerstock.lambdahunt.Model.WiseDirection
import com.spencerstock.lambdahunt.Retrofit.BackendAPI
import com.spencerstock.lambdahunt.Retrofit.RetrofitClient
import com.spencerstock.lambdahunt.Room.RoomsDatabase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.lang.Exception
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean

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
    private lateinit var timer : CountDownTimer
    private var autoMove : AtomicBoolean = AtomicBoolean(false)

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
    private var mVisible: Boolean = true
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
            prepMove("n")
        }
        south.setOnClickListener {
            prepMove("s")
        }
        west.setOnClickListener {
            prepMove("w")
        }
        east.setOnClickListener {
            prepMove("e")
        }
        play.setOnClickListener {
            autoMove.set(!autoMove.get())
            if(autoMove.get()) {
                play.text = "Pause"
                findNextMove()
            } else play.text = "Play"
        }

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        north.setOnTouchListener(mDelayHideTouchListener)


    }

    private fun prepMove(dir: String) {
        if (dir == "n") {
            if (currentRoom?.n_to != null) {
                wiseMove("n", currentRoom!!.n_to!!)
            } else move("n")
        }
        if (dir == "s") {
            if (currentRoom?.s_to != null) {
                wiseMove("s", currentRoom!!.s_to!!)
            } else move("s")
        }
        if (dir == "e") {
            if (currentRoom?.e_to != null) {
                wiseMove("e", currentRoom!!.e_to!!)
            } else move("e")
        }
        if (dir == "w") {
            if (currentRoom?.w_to != null) {
                wiseMove("w", currentRoom!!.w_to!!)
            } else move("w")
        }
    }

    private fun wiseMove(dir: String, nextRoomId: Int) {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(backendAPI.wiseMove(WiseDirection(dir, nextRoomId.toString()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                (Log.e("wisemove", "wisemove Failed"))
                println(it)
                makeTimer(10f)
            }
            .subscribe({ room: Room -> handleData(room, dir) }, {})
        )
    }

    private fun move(dir: String) {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(backendAPI.move(Direction(dir))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                (Log.e("move", "move Failed"))
                println(it)
                makeTimer(10f)
            }
            .subscribe({ room: Room -> handleData(room, dir) }, {})
        )
    }

    private fun fetchData() {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(backendAPI.rooms
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                (Log.e("fetchData", "FetchData Failed"))
                println(it)
                makeTimer(10f)
            }
            .subscribe({ room: Room -> handleData(room) }, {})

        )
    }
    private fun takeTreasure(treasure: Treasure) {
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(backendAPI.takeTreasure(treasure)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                (Log.e("fetchData", "FetchData Failed"))
                println(it)
                makeTimer(10f)
            }
            .subscribe({ room: Room -> handleData(room) }, {})

        )
    }


    private fun handleData(room: Room, direction: String? = null) {
        if(direction != null) {
            Toast.makeText(this, "Successfully moved $direction", Toast.LENGTH_SHORT).show()
        }

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
        val terrainText = "Terrain: " + room.terrain
        terrain.text = terrainText
        coordinates.text = room.coordinates

        room_exits.text = ""
        if (room.exits!!.contains("n")) {
            room_exits.text = "n "
            if (room.n_to != null) {
                val temp = room_exits.text.toString() + "- ${room.n_to} "
                room_exits.text = temp
            }
        }
        if (room.exits!!.contains("s")) {
            var temp = room_exits.text.toString() + "s "
            room_exits.text = temp
            if (room.s_to != null) {
                temp = room_exits.text.toString() + "- ${room.s_to} "
                room_exits.text = temp
            }
        }
        if (room.exits!!.contains("e")) {
            var temp = room_exits.text.toString() + "e "
            room_exits.text = temp
            if (room.e_to != null) {
                temp = room_exits.text.toString() + "- ${room.e_to} "
                room_exits.text = temp
            }
        }
        if (room.exits!!.contains("w")) {
            var temp = room_exits.text.toString() + "w "
            room_exits.text = temp
            if (room.w_to != null) {
                temp = room_exits.text.toString() + "- ${room.w_to}"
                room_exits.text = temp
            }
        }
        random_procs.removeAllViews()

        for (item in room.items!!) {
            val textView = TextView(this)
            textView.text = item
            random_procs.addView(textView)
            textView.setOnClickListener {
                takeTreasure(Treasure(item))
            }
        }
        for (player in room.players!!) {
            val textView = TextView(this)
            textView.text = player
            random_procs.addView(textView)
        }
        for (message in room.messages!!) {
            val textView = TextView(this)
            textView.text = message
            random_procs.addView(textView)
        }


        makeTimer(room.cooldown)


    }

    private fun findNextMove() {
        var moveMade = false
        if (currentRoom!!.items!!.isNotEmpty()) {
            takeTreasure(Treasure(currentRoom!!.items!![0]))
            moveMade = true
        }

        if (!moveMade) {
            if (currentRoom!!.n_to == null && currentRoom!!.exits!!.contains("n")) {
                prepMove("n")
                moveMade = true
            } else if (currentRoom!!.s_to == null && currentRoom!!.exits!!.contains("s")) {
                prepMove("s")
                moveMade = true
            } else if (currentRoom!!.e_to == null && currentRoom!!.exits!!.contains("e")) {
                prepMove("e")
                moveMade = true
            } else if (currentRoom!!.w_to == null && currentRoom!!.exits!!.contains("w")) {
                prepMove("w")
                moveMade = true
            }
        }

        if (!moveMade) {

            val randChoice =
                kotlin.math.abs(ThreadLocalRandom.current().nextInt() % currentRoom!!.exits!!.size)
            when (randChoice) {
                0 -> { prepMove(currentRoom!!.exits!![0]) }
                1 -> { prepMove(currentRoom!!.exits!![1]) }
                2 -> { prepMove(currentRoom!!.exits!![2]) }
                3 -> { prepMove(currentRoom!!.exits!![3]) }
            }

        }
    }


    private fun makeTimer(seconds: Float) {
        timer = object : CountDownTimer((seconds * 1000).toLong(), 100) {
            override fun onTick(millisUntilFinished: Long) {
                room_timer.text = (millisUntilFinished / 100).toString()
            }

            override fun onFinish() {
                room_timer.text = "Ready"
                if (autoMove.get()) findNextMove()
            }
        }
        timer.start()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            show()
        } else {
            show()
        }
    }

    private fun hide() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
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
