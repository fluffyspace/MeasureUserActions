package eu.kodba.measureuseractions

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.torrydo.floatingbubbleview.CloseBubbleBehavior
import com.torrydo.floatingbubbleview.FloatingBubbleListener
import com.torrydo.floatingbubbleview.helper.ViewHelper
import com.torrydo.floatingbubbleview.service.expandable.BubbleBuilder
import com.torrydo.floatingbubbleview.service.expandable.ExpandableBubbleService
import com.torrydo.floatingbubbleview.service.expandable.ExpandedBubbleBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForegroundService: ExpandableBubbleService(), DialogInterface {
    override fun configBubble(): BubbleBuilder {
        bubbleLayout = LayoutInflater.from(this).inflate(R.layout.activity_bubble, null)

        button = bubbleLayout!!.findViewById(R.id.window_close)
        vrijeme = bubbleLayout!!.findViewById(R.id.vrijeme)
        error = bubbleLayout!!.findViewById(R.id.error)

        error!!.setOnClickListener { view: View ->
            errorClicked()
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        button!!.findViewById<View>(R.id.window_close).setOnClickListener { view: View ->
            buttonClicked()
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        /*val imgView = ViewHelper.fromDrawable(this, R.drawable.baseline_timer_24, 60, 60)
        imgView.setOnClickListener {
            expand()
        }*/
        return BubbleBuilder(this)

            // set bubble view
            .bubbleView(bubbleLayout!!)

            // set style for the bubble, fade animation by default
            .bubbleStyle(null)

            // set start location for the bubble, (x=0, y=0) is the top-left
            .startLocation(100, 100)    // in dp
            .startLocationPx(100, 100)  // in px

            // enable auto animate bubble to the left/right side when release, true by default
            .enableAnimateToEdge(true)

            // set close-bubble view
            .closeBubbleView(ViewHelper.fromDrawable(this, com.torrydo.floatingbubbleview.R.drawable.ic_close_bubble, 60, 60))

            // set style for close-bubble, null by default
            .closeBubbleStyle(null)

            // DYNAMIC_CLOSE_BUBBLE: close-bubble moving based on the bubble's location
            // FIXED_CLOSE_BUBBLE (default): bubble will automatically move to the close-bubble when it reaches the closable-area
            .closeBehavior(CloseBubbleBehavior.DYNAMIC_CLOSE_BUBBLE)

            // the more value (dp), the larger closeable-area
            .distanceToClose(100)

            // enable bottom background, false by default
            .bottomBackground(true)

            .addFloatingBubbleListener(object : FloatingBubbleListener {
                override fun onFingerMove(x: Float, y: Float) {} // The location of the finger on the screen which triggers the movement of the bubble.
                override fun onFingerUp(x: Float, y: Float) {}   // ..., when finger release from bubble
                override fun onFingerDown(x: Float, y: Float) {} // ..., when finger tap the bubble
            })

    }

    override fun configExpandedBubble(): ExpandedBubbleBuilder? {
        return null
    }

    fun showingErrorButton(noyes: Boolean) {
        this.error?.visibility = if (noyes) View.VISIBLE else View.GONE
    }

    fun setButtonText(text: String?) {
        this.button?.text = text
    }

    fun setZadatakInfo(text: String?) {
        //zadatakinfo.setText(text);
    }

    fun setVrijemeText(text: String?) {
        this.vrijeme?.text = text
    }

    var error: Button? = null
    var button: Button? = null
    var vrijeme: TextView? = null

    //var window: Window? = null
    var bubbleLayout: View? = null

    var name: String? = null
    val textBegin = "Započni"
    val textStop = "Završi"
    var azurirajNotifikaciju: Job? = null
    val NOTIFICATION_CHANNEL_ID = "example.permanence"
    val FOREGROUND_NOTE_ID = 1
    var exerciseHappening = false
    var exerciseStarted: Long = 0
    var exerciseEnded: Long = 0
    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = null
        set(value){
            if(value == null){
                removeAll()
            } else {
                minimize()
            }
            field = value
        }
    var app: String? = ""

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    lateinit var soundPool: SoundPool
    var start = 0
    var finish = 1

    // Create an explicit intent for an Activity in your app
    lateinit var intentForPending: Intent

    lateinit var pendingIntent: PendingIntent

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }
    private var vReceiver: BroadcastReceiver? = null
    override fun onCreate() {
        super.onCreate()

        minimize()
        // create the custom or default notification
        // based on the android version

        // create an instance of Window class
        // and display the content on screen
        setButtonText(textBegin)


        /*vReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.e("ingo","Something just happens")
            }
        }
        registerReceiver(vReceiver, IntentFilter("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT"))*/
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(
                AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
            )
            .setContentType(
                AudioAttributes.CONTENT_TYPE_SONIFICATION
            )
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                audioAttributes
            )
            .build()
        start = soundPool
            .load(
                this,
                R.raw.start,
                1
            )
        finish = soundPool.load(
            this,
            R.raw.finish,
            1
        )
    }

    companion object{
        var serviceSharedInstance:ForegroundService? = null
        fun getSharedInstance():ForegroundService?{
            return serviceSharedInstance;
        }
    }

    override fun onStartCommand(intent: Intent?, flags2: Int, startId: Int): Int {
        serviceSharedInstance = this

        app = intent?.extras?.getString("app")
        name = intent?.extras?.getString("name")
        val tmp = intent?.extras?.getString("exercise")
        val tmp2 = intent?.extras?.getString("exercises")
        exercises = tmp2?.let { MainActivity.getExercisesFromJson(it) }
        exercise = tmp?.let { MainActivity.getExerciseFromJson(it) }
        if(exercise != null){
            setZadatakInfo(StringBuilder("${exercise!!.id}. ${exercise!!.name}").toString())
        }

        Log.d("ingo", "servis je dobio $exercise")
        Log.d("ingo", "servis je dobio $exercises")
        Log.d("ingo", "servis je dobio za aplikaciju $app")

        intentForPending = packageManager.getLaunchIntentForPackage(this.packageName) ?: Intent(this, ZadatakActivity::class.java).apply {
            //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intentForPending.putExtra("exercise", Gson().toJson(exercise)).putExtra("exercises", Gson().toJson(exercises))
        pendingIntent = PendingIntent.getActivity(this, 0, intentForPending, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        //Log.d("ingo", "putting $exercise to intent")
        notifyUser()

        return super.onStartCommand(intent, flags2, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        azurirajNotifikaciju?.cancel()
        removeAll()
        serviceSharedInstance = null
    }

    fun buildNotification(): Notification {
        val channelName = "Background Service"
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(chan)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val contentTitle = if(exerciseHappening) "Vrijeme prošlo: " + (System.currentTimeMillis() - exerciseStarted) + " ms" else "Vježba nije pokrenuta"
        return notificationBuilder.setOngoing(true)
            .setContentTitle("Vježba ${exercise?.name}")
            .setContentText(contentTitle) // this is important, otherwise the notification will show the way
            //.addAction(R.drawable.ic_delete, "Otvori zadatak", pendingIntent)
            .setContentIntent(pendingIntent)
            // you want i.e. it will show some default notification
            .setSmallIcon(R.drawable.baseline_timer_24)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun obavijesti(text: String){
        Handler(Looper.getMainLooper()).post(Runnable {
            Toast.makeText(
                this@ForegroundService.applicationContext,
                text,
                Toast.LENGTH_LONG
            ).show()
        })
    }

    private fun notifyUser() {
        var isForegroundNotificationVisible = false
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifications = notificationManager.activeNotifications
        for (notification in notifications) {
            if (notification.id == FOREGROUND_NOTE_ID) {
                isForegroundNotificationVisible = true
                break
            }
        }
        Log.v(
            "ingo",
            "Is foreground visible: $isForegroundNotificationVisible"
        )
        if (isForegroundNotificationVisible) {
            notificationManager.notify(FOREGROUND_NOTE_ID, buildNotification())
        } else {
            startForeground(FOREGROUND_NOTE_ID, buildNotification())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buttonClicked() {
        if(exercise == null){
            obavijesti("Vježba nije odabrana.")
            return
        }
        MainActivity.getSharedInstance()?.buttonClicked()
        if(!exerciseHappening){

            soundPool.play(
                start, 1f, 1f, 0, 0, 1f);

            exerciseStarted = System.currentTimeMillis()
            setButtonText(textStop)
            setZadatakInfo(StringBuilder("${exercise!!.id}. ${exercise!!.name}").toString())
            showingErrorButton(true)
            Log.d("ingo", "Exercise started")
            azurirajNotifikaciju = scope.launch {
                // New coroutine that can call suspend functions
                while(true) {
                    try {
                        notifyUser()
                        withContext(Dispatchers.Main){
                            setVrijemeText(((System.currentTimeMillis() - exerciseStarted)/1000).toString() + " s")
                        }
                        Log.d("ingo", "tried updating")
                    } catch (e: Exception) {
                        Log.e("ingo", "greska sendStatistics")
                    }
                    delay(1000)
                }
            }
        } else {
            stopExercise(false)
        }
        exerciseHappening = !exerciseHappening
    }

    fun stopExercise(error: Boolean){
        soundPool.play(
            finish, 1f, 1f, 0, 0, 1f);

        azurirajNotifikaciju?.cancel()
        exerciseEnded = System.currentTimeMillis()
        val db = AppDatabase.getInstance(this)
        val messageDao: ActionsDao = db.actionsDao()

        scope.launch {
            // New coroutine that can call suspend functions
            try {
                val akcija = Actions(exercise = exercise!!.id, timeTook = exerciseEnded-exerciseStarted, timestamp = (exerciseEnded/1000).toLong(), application = app ?: "ne radi", error=error)
                messageDao.insertAll(akcija)
            } catch (e: Exception) {
                Log.e("ingo", "greska sendStatistics")
            }
        }
        Log.d("ingo", "Exercise ended")
        setButtonText(textBegin)
        showingErrorButton(false)
        obavijesti("Zadatak zabilježen.")
        //startActivity(intentForPending)
    }

    override fun errorClicked() {
        stopExercise(true)
        exerciseHappening = !exerciseHappening
    }
}