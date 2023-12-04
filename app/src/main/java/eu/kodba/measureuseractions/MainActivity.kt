package eu.kodba.measureuseractions

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import eu.kodba.measureuseractions.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Type


class MainActivity : AppCompatActivity(), DialogInterface, OnActionClick,
    DeleteDialog.NoticeDialogListener {

    private lateinit var binding: ActivityMainBinding

    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()
    lateinit var zadaciFragment: ZadataciFragment
    lateinit var historyFragment: HistoryFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getExercises()

        binding = ActivityMainBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        checkOverlayPermission();
        Log.d("ingo", "extras ${intent.extras}")

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

        /*binding.openExercises.setOnClickListener {
            startZadaciActivity()
        }

        */

        zadaciFragment = ZadataciFragment()
        historyFragment = HistoryFragment()



        binding.bottomNavigation.setOnItemSelectedListener { item ->
            Log.d("ingo", "clicked")
            when(item.itemId) {
                R.id.item_1 -> {
                    switchFragment(zadaciFragment)
                    Log.d("ingo", "zadaci fragment")
                    true
                }
                R.id.item_2 -> {
                    val servis = ForegroundService.getSharedInstance()
                    if(servis == null){
                        val db = AppDatabase.getInstance(this)
                        val messageDao: ActionsDao = db.actionsDao()
                        lifecycleScope.launch(Dispatchers.Default) {
                            try {
                                var akcije = messageDao.getAll()
                                akcije = akcije.sortedByDescending { it.id }
                                Log.d("ingo", "akcije $akcije")
                                withContext(Dispatchers.Main){
                                    exercises?.let { historyFragment.historyUpdated(akcije, it) }
                                }
                            } catch (e: Exception) {
                                Log.e("ingo", "greska sendStatistics")
                            }
                        }
                    }
                    switchFragment(historyFragment)
                    Log.d("ingo", "history fragment")
                    true
                }
                else -> false
            }
        }
        switchFragment(zadaciFragment)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // izbriši item at ...
        val action = actions[historyFragment.selected_recycler_view_item]
        actions.removeAt(historyFragment.selected_recycler_view_item)
        historyFragment.actionsAdapter.actionsList = actions
        historyFragment.actionsAdapter.exercisesList = exercises
        historyFragment.actionsAdapter.notifyItemRemoved(historyFragment.selected_recycler_view_item)
        val db = AppDatabase.getInstance(this)
        val messageDao: ActionsDao = db.actionsDao()
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                messageDao.delete(action)
            } catch (e: Exception) {
                Log.e("ingo", "greska onDialogPositiveClick $e")
            }
        }
    }

    fun switchFragment(fragment: Fragment){
        supportFragmentManager.commit {
            replace(R.id.fragment_container_view, fragment, "actions")
            setReorderingAllowed(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_restart -> {
                val db = AppDatabase.getInstance(this)
                val actionsDao: ActionsDao = db.actionsDao()
                val exerciseDao: ExerciseDao = db.exerciseDao()
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        actionsDao.deleteAll()
                        exerciseDao.deleteAll()
                        startEnterExerciseUrlActivity()
                    } catch (e: Exception) {
                        Log.e("ingo", "greska sendStatistics")
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        }/* else {
            val db = AppDatabase.getInstance(this)
            val messageDao: ActionsDao = db.actionsDao()
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    var akcije = messageDao.getAll()
                    akcije = akcije.sortedByDescending { it.id }
                    Log.d("ingo", "akcije $akcije")
                    withContext(Dispatchers.Main){
                        exercises?.let { historyFragment.historyUpdated(akcije, it) }
                    }
                } catch (e: Exception) {
                    Log.e("ingo", "greska sendStatistics")
                }
            }
        }*/
    }

    override fun onPause() {
        super.onPause()
        serviceSharedInstance = null
    }

    override fun buttonClicked() {
        Log.d("ingo", "button clicked")
    }

    override fun errorClicked() {
        Log.d("ingo", "errorClicked")
    }

    override fun onLongClick(action: Actions) {

    }

    fun startEnterExerciseUrlActivity(){
        val intent = Intent(this@MainActivity, EnterExerciseUrlActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
        finish()
    }

    fun getExercises(){
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val exerciseDao: ExerciseDao = db.exerciseDao()
            exercises = exerciseDao.getAll().toMutableList()
            if(exercises!!.size == 0){
                withContext(Dispatchers.Main){
                    startEnterExerciseUrlActivity()
                }
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

}