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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
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


class ZadataciFragment : Fragment(R.layout.fragment_zadaci), OnExerciseClick {

    private lateinit var binding: ActivityZadaciBinding

    var name: String = ""
    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()
    private var exercisesAdapter: ExercisesAdapter? = null
    var selected_recycler_view_item = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityZadaciBinding.inflate(layoutInflater)

        val view = binding.root

        getExercisesAndActions()
        return view
    }

    fun getExercisesAndActions(){
        val db = AppDatabase.getInstance(requireContext())
        val actionsDao: ActionsDao = db.actionsDao()
        val exerciseDao: ExerciseDao = db.exerciseDao()
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                actions = actionsDao.getAll().toMutableList()
                exercises = exerciseDao.getAll().toMutableList()

                withContext(Dispatchers.Main){
                    if(exercises != null){
                        exercisesAdapter = ExercisesAdapter(requireContext(), this@ZadataciFragment)
                        exercisesAdapter?.exercisesList = exercises as MutableList<Exercise>
                        exercisesAdapter?.actionsList = actions as MutableList<Actions>
                        binding.recyclerView.adapter = exercisesAdapter
                    }
                }
            } catch (e: Exception) {
                Log.e("ingo", "greska getSolvedExercises ${e.toString()}")
            }
        }
    }

    companion object{

        var serviceSharedInstance:ZadataciFragment? = null
        fun getSharedInstance():ZadataciFragment?{
            return serviceSharedInstance;
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onClick(exercise: Exercise) {
        val intent = Intent(requireContext(), ZadatakActivity::class.java)
        intent.putExtra("exercise", Gson().toJson(exercise))
        intent.putExtra("exercises", Gson().toJson(exercises))
        intent.putExtra("name", name)
        //Log.d("ingo", "predao sam ${intent.extras?.getString("exercise")}")
        //if (intent.resolveActivity() != null) {
            startActivity(intent)
        //}
    }

}