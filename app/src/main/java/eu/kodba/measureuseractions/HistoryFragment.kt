package eu.kodba.measureuseractions

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import eu.kodba.measureuseractions.databinding.ActivityMainBinding
import eu.kodba.measureuseractions.databinding.FragmentHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HistoryFragment : Fragment(), OnActionClick {


    private var binding: FragmentHistoryBinding? = null
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var actionsAdapter: ActionsAdapter
    var selected_recycler_view_item = -1
    var actions: MutableList<Actions> = mutableListOf()
    var exercises: MutableList<Exercise> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)

        actionsAdapter = ActionsAdapter(requireContext(), this)
        binding!!.recyclerView.adapter = actionsAdapter

        binding!!.sendStatistics.setOnClickListener {
            val db = AppDatabase.getInstance(requireContext())
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

        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    fun historyUpdated(akcije: List<Actions>, vjezbe: MutableList<Exercise>){
        Log.d("ingo", "historyUpdated")
        exercises = vjezbe
        actions = akcije.toMutableList()
        if(akcije.isEmpty()){
            Log.d("ingo", "akcije.isEmpty()")
            binding?.odradeneVjezbeHint?.text = getString(R.string.jos_nema_rjesenih_vjezbi)
            binding?.sendStatistics?.visibility = View.GONE
        } else {
            Log.d("ingo", "!akcije.isEmpty()")
            binding?.odradeneVjezbeHint?.text = getString(R.string.odradene_vjezbe_drzi_za_brisanje)
            binding?.sendStatistics?.visibility = View.VISIBLE
        }

        Log.d("ingo", binding?.recyclerView.toString())
        actionsAdapter.exercisesList = exercises
        actionsAdapter.actionsList = akcije
        actionsAdapter.notifyDataSetChanged()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HistoryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onLongClick(action: Actions) {
        selected_recycler_view_item = actions.indexOf(action)
        val newFragment = DeleteDialog("Želiš li obrisati vježbu?")
        newFragment.show(parentFragmentManager, "deleteAction")
    }

}