package eu.kodba.measureuseractions

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Exercise(
    @PrimaryKey(autoGenerate = true) var id:Int,
    var name:String,
    var approxTime:Long = 0,
    var instructions: String = "",
    var additionalInfo: String = "",
    var repetitions: Int = 0,
)
