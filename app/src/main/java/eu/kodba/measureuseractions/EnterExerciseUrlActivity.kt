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
import eu.kodba.measureuseractions.databinding.ActivityEnterExerciseUrlBinding
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


        binding.getExercises.setOnClickListener {
            binding.getExercises.isEnabled = false
            if(binding.exercisesLink.text.toString() != "" && binding.personsName.text.toString() != "") {
                Log.d("ingo", "fetching exercises")
                fetchExercises(binding.exercisesLink.text.toString())
            } else {
                Log.d("ingo", "not fetching " + binding.exercisesLink.text.toString() + "+" + binding.personsName.text.toString())
            }
        }
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
                binding.getExercises.isEnabled = true
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle this
                try{
                    val gson = Gson()
                    val listType: Type = object : TypeToken<List<Exercise?>?>() {}.type
                    val bodystring = response.body()?.string()
                    val entity: MutableList<Exercise> =
                        gson.fromJson(bodystring, listType)
                    exercises = entity
                    bodystring?.let {
                        Log.e("ingo", it)
                    }
                    Log.e("ingo", entity.toString())
                    changeSettings("url", binding.exercisesLink.text.toString())
                    changeSettings("name", binding.personsName.text.toString())
                    Handler(Looper.getMainLooper()).post(Runnable {
                        startMainActivity()
                    })
                } catch (e: JsonParseException){
                    Toast.makeText(this@EnterExerciseUrlActivity, "JsonParseException", Toast.LENGTH_SHORT).show()
                        binding.getExercises.isEnabled = true
                } catch (e: JsonSyntaxException){
                    Toast.makeText(this@EnterExerciseUrlActivity, "JsonSyntaxException", Toast.LENGTH_SHORT).show()
                        binding.getExercises.isEnabled = true
                } finally {

                }
            }
        })
    }

    fun startMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("exercises", Gson().toJson(exercises))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
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
        val url = getPreferences(MODE_PRIVATE).getString("url", "")
        if(url != null && url != ""){
            binding.getExercises.isEnabled = false
            binding.linearlayout.visibility = View.GONE
            fetchExercises(url)
        }
    }

}