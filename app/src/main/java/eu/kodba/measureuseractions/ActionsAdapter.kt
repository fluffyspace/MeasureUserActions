package eu.kodba.measureuseractions

import android.content.Context
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

interface OnActionClick{
    fun onLongClick(action: Actions)
}

class ActionsAdapter(c: Context, onActionClick: OnActionClick) : RecyclerView.Adapter<ActionsAdapter.ViewHolder>() {
    var actionsList: List<Actions> = listOf()
    var exercisesList: List<Exercise>? = listOf()
    var context:Context
    var onActionClick:OnActionClick
    var idOtvorenogMenija:Int = -1


    init {
        context = c
        this.onActionClick = onActionClick
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnLongClickListener {
        var action_title: TextView
        var action_description: TextView
        override fun onLongClick(v: View): Boolean {
            val pos = adapterPosition
            onActionClick.onLongClick(actionsList[pos])
            val context = v.context
            /*val launchIntent: Intent? =
                context.packageManager.getLaunchIntentForPackage(appsList[pos].packageName)
            if(launchIntent != null) {
                EventBus.getDefault().post(MessageEvent(appsList[pos].label, pos, appsList[pos].packageName, appsList[pos].color, app=appsList[pos]))
            } else {
                Log.d("ingo", "No launch intent")
            }
            //context.startActivity(launchIntent)*/
            return true
        }


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {

            //Finds the views from our row.xml
            action_title = itemView.findViewById(R.id.action_title) as TextView
            action_description = itemView.findViewById(R.id.action_description) as TextView
            itemView.setOnLongClickListener(this)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Here we use the information in the list we created to define the views
        //val appIcon:Drawable? = icons.get(appsList[i].packageName)
        val exercise = exercisesList?.find{it.id == actionsList[i].exercise}
        val exerciseName = exercise?.name ?: "Zadatak ne postoji"
        Log.d("ingo", "broj zadataka " + exercisesList?.size)
        viewHolder.action_title.text = StringBuilder(exerciseName)
        viewHolder.action_description.text = StringBuilder(getDateString(actionsList[i].timestamp) + " (" + (if(actionsList[i].standardOrAlternative) "alt." else "std.") + ", ~" + (actionsList[i].timeTook/1000).toString() + " s)")

    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        return actionsList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.history_row, parent, false)
        return ViewHolder(view)
    }

    private val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.ENGLISH)

    private fun getDateString(time: Long) : String = simpleDateFormat.format(time)

    private fun getDateString(time: Int) : String = simpleDateFormat.format(time)

}