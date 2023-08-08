package eu.kodba.measureuseractions

import android.app.Notification.Action
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import eu.kodba.measureuseractions.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Type


class MainActivity : AppCompatActivity(), DialogInterface, OnActionClick,
    DeleteDialog.NoticeDialogListener {

    private lateinit var binding: ActivityMainBinding

    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()
    private var actionsAdapter: ActionsAdapter? = null
    var selected_recycler_view_item = -1

    var url: String? = null
    var name: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        binding.openExercises.isEnabled = false
        binding.exercisesStatus.text = "Zadaci se učitavaju..."



        checkOverlayPermission();
        Log.d("ingo", "extras ${intent.extras}")

        val urlByIntent = intent.extras?.getString("url")
        Log.d("ingo", "urlByIntent $urlByIntent")
        if(urlByIntent != null){
            changeSettings("url", urlByIntent)
        }
        val nameByIntent = intent.extras?.getString("name")
        Log.d("ingo", "nameByIntent $nameByIntent")
        if(nameByIntent != null){
            changeSettings("name", nameByIntent)
        }
        if(nameByIntent == null || urlByIntent == null){
            url = getPreferences(MODE_PRIVATE).getString("url", urlByIntent ?: "")
            name = getPreferences(MODE_PRIVATE).getString("name", nameByIntent ?: "")
            Log.d("ingo", "saved = $url, $name")
            if(url == "" || name == ""){
                // pokreni EnterExerciseUrlActivity da pitaš korisnika za unos, dodaj url i name kao extras u slučaju da je nešto od toga ispunjeno
                val intent = Intent(this, EnterExerciseUrlActivity::class.java)
                intent.putExtra("url", url)
                intent.putExtra("name", name)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
                finish()
                return
            }
        } else {
            url = urlByIntent
            name = nameByIntent
        }
        binding.personsName.text = "Bok, $name."
        if(url != null) fetchExercises(url!!)

        val zadaci_json = intent.extras?.getString("exercises")
        if(zadaci_json != null) {
            val gson = Gson()
            val listType: Type = object : TypeToken<List<Exercise?>?>() {}.type
            try {
                exercises =
                    gson.fromJson(zadaci_json, listType)
                Log.d("ingo", "vjezbe " + exercises.toString())
            }catch (e: JsonParseException){
                Toast.makeText(this, "JsonParseException", Toast.LENGTH_SHORT).show()
            } catch (e: JsonSyntaxException){
                Toast.makeText(this, "JsonSyntaxException", Toast.LENGTH_SHORT).show()
            }
        }

        binding.openExercises.setOnClickListener {
            startZadaciActivity()
        }

        binding.sendStatistics.setOnClickListener {
            val db = AppDatabase.getInstance(this)
            val messageDao: ActionsDao = db.actionsDao()
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    val akcije = messageDao.getAll()
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, akcije.toString())
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                } catch (e: Exception) {
                    Log.e("ingo", "greska sendStatistics")
                }
            }
        }

        actionsAdapter = ActionsAdapter(this, this)
        binding.recyclerView.adapter = actionsAdapter
    }

    fun startZadaciActivity(){
        val intent = Intent(this, ZadataciActivity::class.java)
        intent.putExtra("exercises", Gson().toJson(exercises))
        intent.putExtra("name", name)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    companion object{
        var serviceSharedInstance:MainActivity? = null
        fun getSharedInstance():MainActivity?{
            return serviceSharedInstance;
        }
        fun getExercisesFromJson(json: String): MutableList<Exercise>?{
            val gson = Gson()
            val listType: Type = object : TypeToken<List<Exercise?>?>() {}.type
            try {
                return gson.fromJson(json, listType)
            }catch (e: JsonParseException){
                Log.e("ingo", e.toString())
                return null
            } catch (e: JsonSyntaxException){
                Log.e("ingo", e.toString())
                return null
            }
        }
        fun getExerciseFromJson(json: String): Exercise?{
            val gson = Gson()
            try {
                return gson.fromJson(json, Exercise::class.java)
            }catch (e: JsonParseException){
                Log.e("ingo", e.toString())
                return null
            } catch (e: JsonSyntaxException){
                Log.e("ingo", e.toString())
                return null
            }
        }
    }

    // method to ask user to grant the Overlay permission
    fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            // send user to the device settings
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(myIntent)
        }
    }

    // check for permission again when user grants it from
    // the device settings, and start the service
    override fun onResume() {
        super.onResume()
        serviceSharedInstance = this
        val servis = ForegroundService.getSharedInstance()
        if(servis != null){
            /*binding.serviceOnoff.text = "Zatvori vježbu"
            exercise = servis.exercise
            startZadatakActivity()*/
            startActivity(servis.intentForPending)
            finish()
        } else {
            val db = AppDatabase.getInstance(this)
            val messageDao: ActionsDao = db.actionsDao()
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    var akcije = messageDao.getAll()
                    akcije = akcije.sortedByDescending { it.id }
                    Log.d("ingo", "akcije $akcije")
                    withContext(Dispatchers.Main){
                        if(akcije.isEmpty()){
                            binding.odradeneVjezbeHint.text = "Još nema rješenih vježbi."
                            binding.sendStatistics.visibility = View.GONE
                        }
                        actions = akcije.toMutableList()
                        (binding.recyclerView.adapter as ActionsAdapter).actionsList = akcije
                        (binding.recyclerView.adapter as ActionsAdapter).notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("ingo", "greska sendStatistics")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        serviceSharedInstance = null
    }

    override fun buttonClicked() {
        Log.d("ingo", "button clicked")
    }

    override fun onLongClick(action: Actions) {
        selected_recycler_view_item = actions.indexOf(action)
        val newFragment = DeleteDialog("Želiš li obrisati vježbu?")
        newFragment.show(supportFragmentManager, "deleteAction")
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // izbriši item at ...
        val action = actions[selected_recycler_view_item]
        actions.removeAt(selected_recycler_view_item)
        (binding.recyclerView.adapter as ActionsAdapter).actionsList = actions
        (binding.recyclerView.adapter as ActionsAdapter).exercisesList = exercises
        (binding.recyclerView.adapter as ActionsAdapter).notifyItemRemoved(selected_recycler_view_item)
        val db = AppDatabase.getInstance(this)
        val messageDao: ActionsDao = db.actionsDao()
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val akcije = messageDao.delete(action)
            } catch (e: Exception) {
                Log.e("ingo", "greska onDialogPositiveClick")
            }
        }
    }

    private fun changeSettings(key: String, value: Any){
        Log.d("ingo", "should write $key as $value")
        val sharedPreferences: SharedPreferences = getPreferences(MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if(value::class == Boolean::class) {
            editor.putBoolean(key, value as Boolean)
        } else if(value::class == Float::class) {
            editor.putFloat(key, value as Float)
        } else if(value::class == String::class) {
            editor.putString(key, value as String)
        } else if(value::class == Int::class) {
            editor.putInt(key, value as Int)
        } else if(value::class == Long::class) {
            editor.putLong(key, value as Long)
        }
        editor.apply()
    }

    fun fetchExercises(url: String){
        val client = OkHttpClient()
        val request: Request = Request.Builder()
            .get()
            .url(url)
            .build()

        val test = client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
                Log.e("ingo","Try again later!!! $e")
                //binding.getExercises.isEnabled = true
                Handler(Looper.getMainLooper()).post(Runnable {
                    binding.exercisesStatus.text = "Greška učitavanja zadataka."
                })
            }

            override fun onResponse(call: Call, response: Response) {
                val bodystring = response.body()?.string()
                if(bodystring != null) {
                    exercises = getExercisesFromJson(bodystring)
                }
                bodystring?.let {
                    Log.e("ingo", it)
                }
                Log.e("ingo", exercises.toString())
                Handler(Looper.getMainLooper()).post(Runnable {
                    (binding.recyclerView.adapter as ActionsAdapter).exercisesList = exercises
                    (binding.recyclerView.adapter as ActionsAdapter).notifyDataSetChanged()
                    binding.openExercises.isEnabled = true
                    binding.exercisesStatus.text = "Zadaci učitani."
                })
            }
        })
    }

}