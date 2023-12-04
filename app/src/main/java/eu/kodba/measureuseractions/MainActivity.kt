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
import androidx.lifecycle.lifecycleScope
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
    private var actionsAdapter: ActionsAdapter? = null
    var selected_recycler_view_item = -1

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
                        putExtra(Intent.EXTRA_TEXT, Gson().toJson(akcije))
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

    fun startZadaciActivity(){
        val intent = Intent(this, ZadataciActivity::class.java)
        intent.putExtra("exercises", Gson().toJson(exercises))
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
                        } else {
                            binding.odradeneVjezbeHint.text = "Odrađene vježbe: (drži za brisanje)"
                            binding.sendStatistics.visibility = View.VISIBLE
                        }
                        actions = akcije.toMutableList()
                        (binding.recyclerView.adapter as ActionsAdapter).exercisesList = exercises
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

    override fun errorClicked() {
        Log.d("ingo", "errorClicked")
    }

    override fun onLongClick(action: Actions) {
        selected_recycler_view_item = actions.indexOf(action)
        val newFragment = DeleteDialog("Želiš li obrisati vježbu?")
        newFragment.show(supportFragmentManager, "deleteAction")
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
                messageDao.delete(action)
            } catch (e: Exception) {
                Log.e("ingo", "greska onDialogPositiveClick $e")
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