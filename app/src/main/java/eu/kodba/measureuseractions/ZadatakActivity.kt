package eu.kodba.measureuseractions

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import eu.kodba.measureuseractions.databinding.ActivityZadatakBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZadatakActivity : AppCompatActivity(), DeleteDialog.NoticeDialogListener, OnActionClick {

    private lateinit var binding: ActivityZadatakBinding

    var name: String? = null
    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()

    private var actionsAdapter: ActionsAdapter? = null
    var selected_recycler_view_item = -1
    override fun onBackPressed() {
        if(ForegroundService.getSharedInstance() == null){
            super.onBackPressed()
        } else {
            this.moveTaskToBack(true)
        }
    }

    fun getSolvedExercises(){
        val db = AppDatabase.getInstance(this)
        val messageDao: ActionsDao = db.actionsDao()
        val exercisesDao: ExerciseDao = db.exerciseDao()
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                actions = messageDao.getAll().filter{it.exercise == exercise!!.id }.toMutableList()
                exercises = exercisesDao.getAll().toMutableList()

                val rjesenosti = "Rješenosti zadatka: " + exercise!!.apps.map{app -> "$app: " + actions.filter { it.application == app }.size + "/${exercise!!.repetitions}"}

                withContext(Dispatchers.Main){
                    binding.rjesenostZadatka.text = rjesenosti.toString()
                    (binding.recyclerView.adapter as ActionsAdapter).exercisesList = exercises
                    (binding.recyclerView.adapter as ActionsAdapter).actionsList = actions
                    (binding.recyclerView.adapter as ActionsAdapter).notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("ingo", "greska getSolvedExercises ${e.toString()}")
            }
        }
    }

    fun goToMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }
/*
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        ForegroundService.getSharedInstance()?.stopSelf()
        finish()
    }*/

    override fun onResume() {
        super.onResume()
        if(ForegroundService.getSharedInstance() != null) {
            binding.serviceOnoff.text = "Prekini zadatak"
        }
        getSolvedExercises()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityZadatakBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        name = intent.extras?.getString("name")
        exercise = intent.extras?.getString("exercise")
            ?.let { MainActivity.getExerciseFromJson(it) }
        exercises = intent.extras?.getString("exercises")
            ?.let { MainActivity.getExercisesFromJson(it) }

        //Log.d("ingo", "dobio sam $exercise i $exercises")
        if(exercise == null){
            Toast.makeText(this, "Vježba nije dostupna.", Toast.LENGTH_SHORT).show()
            goToMainActivity()
            finish()
            return
        }

        getSolvedExercises()

        if(exercise!!.apps.isEmpty()){
            binding.aplikacijaContainer.visibility = View.GONE
        }

        supportActionBar?.title = "Zadatak ${exercise!!.id}"

        binding.upute.text = exercise!!.instructions
        binding.zadatak.text = "${exercise!!.name}"
        val encodedHtml = Base64.encodeToString(exercise!!.instructions.toByteArray(), Base64.NO_PADDING)

        binding.webview.loadData(encodedHtml, "text/html", "base64")

        val items = exercise!!.apps
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        binding.appsMenu.setAdapter(adapter)

        binding.serviceOnoff.setOnClickListener {
            /*val intent = Intent(this, MyBubbleService::class.java)

            ContextCompat.startForegroundService(this, intent)*/

            if(ForegroundService.getSharedInstance() == null){
                if(exercise!!.apps.isNotEmpty() && (binding.appsMenu.text == null || binding.appsMenu.text.toString() == "")){
                    Toast.makeText(this, "Odaberi aplikaciju", Toast.LENGTH_SHORT).show()
                } else {
                    startService()
                    binding.serviceOnoff.text = "Prekini zadatak"
                }
            } else {
                ForegroundService.getSharedInstance()?.stopSelf()
                binding.serviceOnoff.text = "Započni zadatak"
            }
        }
        actionsAdapter = ActionsAdapter(this, this)
        binding.recyclerView.adapter = actionsAdapter
    }

    companion object{
        var serviceSharedInstance:ZadatakActivity? = null
        fun getSharedInstance():ZadatakActivity?{
            return serviceSharedInstance;
        }
    }

    fun startService() {
        Log.d("ingo", "application ${binding.appsMenu.text}")
        // check if the user has already granted
        // the Draw over other apps permission
        if (Settings.canDrawOverlays(this)) {
            // start the service based on the android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, ForegroundService::class.java)
                    .putExtra("app", binding.appsMenu.text.toString())
                    .putExtra("name", name)
                    .putExtra("exercise", Gson().toJson(exercise))
                    .putExtra("exercises", Gson().toJson(exercises))
                )
            } else {
                startService(Intent(this, ForegroundService::class.java)
                    .putExtra("app", binding.appsMenu.text.toString())
                    .putExtra("name", name)
                    .putExtra("exercise", Gson().toJson(exercise))
                    .putExtra("exercises", Gson().toJson(exercises))
                )
            }
        }
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
                messageDao.delete(action)
            } catch (e: Exception) {
                Log.e("ingo", "greska onDialogPositiveClick $e")
            }
        }
    }

}