package eu.kodba.measureuseractions

import android.app.Notification.Action
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import eu.kodba.measureuseractions.databinding.ActivityZadaciBinding
import eu.kodba.measureuseractions.databinding.ActivityZadatakBinding
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


class ZadataciActivity : AppCompatActivity(), OnExerciseClick {

    private lateinit var binding: ActivityZadaciBinding

    var exercise: Exercise? = null
    var exercises: MutableList<Exercise> = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()
    private var exercisesAdapter: ExercisesAdapter? = null
    var selected_recycler_view_item = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityZadaciBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        val zadaci_json = intent.extras?.getString("exercises")
        if(zadaci_json != null) {
            val gson = Gson()
            val listType: Type = object : TypeToken<List<Exercise?>?>() {}.type
            try {
                exercises =
                    gson.fromJson(zadaci_json, listType)

                exercisesAdapter = ExercisesAdapter(this, this)
                exercisesAdapter?.exercisesList = exercises
                binding.recyclerView.adapter = exercisesAdapter
                //binding.recyclerView.adapter?.notifyDataSetChanged()
                Log.d("ingo", exercises.toString())
            }catch (e: JsonParseException){
                Toast.makeText(this, "JsonParseException", Toast.LENGTH_SHORT).show()
            } catch (e: JsonSyntaxException){
                Toast.makeText(this, "JsonSyntaxException", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object{

        var serviceSharedInstance:ZadataciActivity? = null
        fun getSharedInstance():ZadataciActivity?{
            return serviceSharedInstance;
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onClick(exercise: Exercise) {
        val intent = Intent(this, ZadatakActivity::class.java)
        intent.putExtra("exercise", Gson().toJson(exercise))
        intent.putExtra("exercises", Gson().toJson(exercises))
        Log.d("ingo", "predao sam ${intent.extras?.getString("exercise")}")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

}