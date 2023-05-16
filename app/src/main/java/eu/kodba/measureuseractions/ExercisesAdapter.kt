package eu.kodba.measureuseractions

import android.content.Context

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface OnExerciseClick{
    fun onClick(exercise: Exercise)
}

class ExercisesAdapter(c: Context, onExerciseClick: OnExerciseClick) : RecyclerView.Adapter<ExercisesAdapter.ViewHolder>() {
    var exercisesList: List<Exercise> = listOf()
    var context:Context
    var onExerciseClick:OnExerciseClick

    init {
        context = c
        this.onExerciseClick = onExerciseClick
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var action_title: TextView
        var action_description: TextView
        override fun onClick(v: View) {
            val pos = adapterPosition
            onExerciseClick.onClick(exercisesList[pos])
        }

        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {

            //Finds the views from our row.xml
            action_title = itemView.findViewById(R.id.action_title) as TextView
            action_description = itemView.findViewById(R.id.action_description) as TextView
            itemView.setOnClickListener(this)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Here we use the information in the list we created to define the views
        //val appIcon:Drawable? = icons.get(appsList[i].packageName)
        viewHolder.action_title.text = exercisesList[i].name
        viewHolder.action_description.text = "Očekivano trajanje: " + exercisesList[i].approxTime.toString() + " min"

    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        return exercisesList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.cardview_action_row, parent, false)
        return ViewHolder(view)
    }

}