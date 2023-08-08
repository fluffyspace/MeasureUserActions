package eu.kodba.measureuseractions

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ForegroundService: Service(), DialogInterface {
    var window: Window? = null

    var name: String? = null
    val textBegin = "Započni"
    val textStop = "Završi"
    var azurirajNotifikaciju:Job? = null
    val NOTIFICATION_CHANNEL_ID = "example.permanence"
    val FOREGROUND_NOTE_ID = 1
    var exerciseHappening = false
    var exerciseStarted: Long = 0
    var exerciseEnded: Long = 0
    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = null
        set(value){
            if(value == null){
                window?.close()
            } else {
                window?.open()
            }
            field = value
        }
    var switch = false

    val scope = CoroutineScope(Job() + Dispatchers.IO)

    // Create an explicit intent for an Activity in your app
    lateinit var intentForPending: Intent

    lateinit var pendingIntent: PendingIntent

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }
    private var vReceiver: BroadcastReceiver? = null
    override fun onCreate() {
        super.onCreate()
        // create the custom or default notification
        // based on the android version

        // create an instance of Window class
        // and display the content on screen
        window = Window(this, this)
        window!!.setButtonText(textBegin)

        /*vReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.e("ingo","Something just happens")
            }
        }
        registerReceiver(vReceiver, IntentFilter("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT"))*/
    }

    companion object{
        var serviceSharedInstance:ForegroundService? = null
        fun getSharedInstance():ForegroundService?{
            return serviceSharedInstance;
        }
    }

    override fun onStartCommand(intent: Intent?, flags2: Int, startId: Int): Int {
        serviceSharedInstance = this

        switch = intent?.extras?.getBoolean("switch") ?: false
        name = intent?.extras?.getString("name")
        val tmp = intent?.extras?.getString("exercise")
        val tmp2 = intent?.extras?.getString("exercises")
        exercises = tmp2?.let { MainActivity.getExercisesFromJson(it) }
        exercise = tmp?.let { MainActivity.getExerciseFromJson(it) }

        //Log.d("ingo", "servis je dobio $exercise")
        //Log.d("ingo", "servis je dobio $exercises")
        Log.d("ingo", "servis je dobio za switch $switch")


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
        window?.close()
        serviceSharedInstance = null
    }

    fun buildNotification(): Notification{
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
            .setSmallIcon(R.drawable.ic_delete)
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
            exerciseStarted = System.currentTimeMillis()
            window!!.setButtonText(textStop)
            Log.d("ingo", "Exercise started")
            azurirajNotifikaciju = scope.launch {
                // New coroutine that can call suspend functions
                while(true) {
                    try {
                        notifyUser()
                        withContext(Dispatchers.Main){
                            window!!.setVrijemeText(((System.currentTimeMillis() - exerciseStarted)/1000).toString() + " s")
                        }
                        Log.d("ingo", "tried updating")
                    } catch (e: Exception) {
                        Log.e("ingo", "greska sendStatistics")
                    }
                    delay(1000)
                }
            }
        } else {
            azurirajNotifikaciju?.cancel()
            exerciseEnded = System.currentTimeMillis()
            val db = AppDatabase.getInstance(this)
            val messageDao: ActionsDao = db.actionsDao()

            scope.launch {
                // New coroutine that can call suspend functions
                try {
                    val akcija = Actions(exercise = exercise!!.id, timeTook = exerciseEnded-exerciseStarted, timestamp = exerciseEnded, standardOrAlternative = switch)
                    val akcije = messageDao.insertAll(akcija)
                } catch (e: Exception) {
                    Log.e("ingo", "greska sendStatistics")
                }
            }
            Log.d("ingo", "Exercise ended")
            window!!.setButtonText(textBegin)
            obavijesti("Zadatak zabilježen.")
            //startActivity(intentForPending)
        }
        exerciseHappening = !exerciseHappening
    }

}