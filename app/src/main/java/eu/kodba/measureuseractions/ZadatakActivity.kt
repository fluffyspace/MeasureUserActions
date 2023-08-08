package eu.kodba.measureuseractions

import android.app.Notification.Action
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
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

enum class UsingStandardOrAlternative{
    NONE, STANDARD, ALTERNATIVE
}
class ZadatakActivity : AppCompatActivity(), DeleteDialog.NoticeDialogListener {

    private lateinit var binding: ActivityZadatakBinding

    var name: String? = null
    var exercise: Exercise? = null
    var exercises: MutableList<Exercise>? = mutableListOf()
    var actions: MutableList<Actions> = mutableListOf()
    private var actionsAdapter: ActionsAdapter? = null
    var selected_recycler_view_item = -1
    var using_standard_or_alternative = UsingStandardOrAlternative.NONE

    override fun onBackPressed() {
        if(ForegroundService.getSharedInstance() == null){
            super.onBackPressed()
        } else {
            this.moveTaskToBack(true)
        }
    }

    fun goToMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        ForegroundService.getSharedInstance()?.stopSelf()
        finish()
    }

    override fun onResume() {
        super.onResume()
        if(ForegroundService.getSharedInstance() != null) {
            binding.serviceOnoff.text = "Prekini zadatak"
            binding.serviceOnoff.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityZadatakBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        binding.serviceOnoff.isEnabled = false

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
        binding.upute.text = exercise!!.instructions
        binding.zadatak.text = "Zadatak: ${exercise!!.name}"
        val encodedHtml = Base64.encodeToString(exercise!!.instructions.toByteArray(), Base64.NO_PADDING)

        binding.webview.loadData(encodedHtml, "text/html", "base64")

        binding.toggleButton.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
            // Respond to button selection
            Log.d("ingo", toggleButton.toString() + " " + checkedId + " " + isChecked)
            if(binding.button1.id == checkedId && isChecked){
                Log.d("ingo", "prvi označen")
                using_standard_or_alternative = UsingStandardOrAlternative.STANDARD
            } else if(binding.button2.id == checkedId && isChecked){
                Log.d("ingo", "drugi označen")
                using_standard_or_alternative = UsingStandardOrAlternative.ALTERNATIVE
            }
            //binding.instructionToOpenWhat.text = StringBuilder("3. Otvori " + (if(switched) "alternativnu" else "standardnu") + " aplikaciju.")
            ForegroundService.getSharedInstance()?.switch = using_standard_or_alternative == UsingStandardOrAlternative.STANDARD
            binding.serviceOnoff.isEnabled = true
        }

        //binding.instructionToOpenWhat.text = StringBuilder("3. Otvori " + (if(switched) "alternativnu" else "standardnu") + " aplikaciju.")
        binding.serviceOnoff.setOnClickListener {
            if(ForegroundService.getSharedInstance() == null){
                startService()
                binding.serviceOnoff.text = "Prekini zadatak"
            } else {
                ForegroundService.getSharedInstance()?.stopSelf()
                binding.serviceOnoff.text = "Započni zadatak"
            }
            /*val newFragment = DeleteDialog("Stvarno želiš prekinuti vježbu?")
            newFragment.show(supportFragmentManager, "exitAction")*/

        }

    }

    companion object{
        var serviceSharedInstance:ZadatakActivity? = null
        fun getSharedInstance():ZadatakActivity?{
            return serviceSharedInstance;
        }
    }

    override fun onPause() {
        super.onPause()
    }

    fun startService() {
        // check if the user has already granted
        // the Draw over other apps permission
        if (Settings.canDrawOverlays(this)) {
            // start the service based on the android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, ForegroundService::class.java)
                    .putExtra("switch", using_standard_or_alternative == UsingStandardOrAlternative.ALTERNATIVE)
                    .putExtra("name", name)
                    .putExtra("exercise", Gson().toJson(exercise))
                    .putExtra("exercises", Gson().toJson(exercises))
                )
            } else {
                startService(Intent(this, ForegroundService::class.java)
                    .putExtra("switch", using_standard_or_alternative == UsingStandardOrAlternative.ALTERNATIVE)
                    .putExtra("name", name)
                    .putExtra("exercise", Gson().toJson(exercise))
                    .putExtra("exercises", Gson().toJson(exercises))
                )
            }
        }
    }

}