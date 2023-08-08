package eu.kodba.measureuseractions

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import eu.kodba.measureuseractions.databinding.ActivityEnterExerciseUrlBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Type


class EnterExerciseUrlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterExerciseUrlBinding

    var exercise: Exercise? = null
    var exercises: MutableList<Exercise> = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()
    private var actionsAdapter: ActionsAdapter? = null
    var selected_recycler_view_item = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEnterExerciseUrlBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        val action2: String? = intent?.action
        val data2: Uri? = intent?.data
        Log.d("ingo", "action $action2")
        Log.d("ingo", "data ${data2?.pathSegments}")
        if(data2?.pathSegments != null && data2.pathSegments?.size!! > 0){
            binding.exercisesLink.setText(data2.pathSegments!![0].toString())
        }
        if(data2?.pathSegments != null && data2.pathSegments?.size!! == 2){
            binding.personsName.setText(data2.pathSegments!![1].toString())
        }

        val urlByIntent = intent.extras?.getString("url")
        if(urlByIntent != null && urlByIntent != ""){
            binding.exercisesLink.setText(urlByIntent)
        }
        val nameByIntent = intent.extras?.getString("name")
        if(nameByIntent != null && nameByIntent != ""){
            binding.personsName.setText(nameByIntent)
        }

        binding.getExercises.setOnClickListener {
            if(binding.exercisesLink.text.toString() != "" && binding.personsName.text.toString() != "") {
                startMainActivity()
            } else {
                Log.d("ingo", "not fetching " + binding.exercisesLink.text.toString() + "+" + binding.personsName.text.toString())
                Toast.makeText(this, "Nisu popunjena sva polja.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun startMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        Log.d("ingo", "putting extra ${binding.exercisesLink.text} ${binding.personsName.text}")
        intent.putExtra("url", binding.exercisesLink.text.toString())
        intent.putExtra("name", binding.personsName.text.toString())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    // check for permission again when user grants it from
    // the device settings, and start the service
    override fun onResume() {
        super.onResume()
        val servis = ForegroundService.getSharedInstance()
        if(servis != null){
            startActivity(servis.intentForPending)
            finish()
            return
        }
    }

}