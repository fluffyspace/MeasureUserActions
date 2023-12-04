package eu.kodba.measureuseractions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import eu.kodba.measureuseractions.databinding.ActivityEnterExerciseUrlBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class EnterExerciseUrlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterExerciseUrlBinding

    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = null
    var actions: MutableList<Actions> = mutableListOf()

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

        val urlByIntent = intent.extras?.getString("url")
        if(urlByIntent != null && urlByIntent != ""){
            binding.exercisesLink.setText(urlByIntent)
        }

        binding.getExercises.setOnClickListener {
            if(binding.exercisesLink.text.toString() != "") {
                fetchExercises(binding.exercisesLink.text.toString())
            } else {
                Log.d("ingo", "not fetching " + binding.exercisesLink.text.toString())
                Toast.makeText(this, getString(R.string.nisu_popunjena_sva_polja), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun startMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        Log.d("ingo", "putting extra ${binding.exercisesLink.text}")
        intent.putExtra("url", binding.exercisesLink.text.toString())
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
                    Toast.makeText(this@EnterExerciseUrlActivity, getString(R.string.greska_ucitavanja), Toast.LENGTH_SHORT).show()
                })
            }

            override fun onResponse(call: Call, response: Response) {
                val bodystring = response.body()?.string()
                if(bodystring != null) {
                    exercises = MainActivity.getExercisesFromJson(bodystring)

                    val db = AppDatabase.getInstance(this@EnterExerciseUrlActivity)
                    val exerciseDao: ExerciseDao = db.exerciseDao()
                    for(tmp in exercises!!) {
                        exerciseDao.insertAll(tmp)
                    }
                    startMainActivity()
                }
                bodystring?.let {
                    Log.e("ingo", it)
                }
                Log.e("ingo", exercises.toString())
                Handler(Looper.getMainLooper()).post(Runnable {
                    Toast.makeText(this@EnterExerciseUrlActivity, getString(R.string.zadaci_ucitani), Toast.LENGTH_SHORT).show()
                })
            }
        })
    }

}