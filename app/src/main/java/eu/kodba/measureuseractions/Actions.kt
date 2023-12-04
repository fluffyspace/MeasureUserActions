package eu.kodba.measureuseractions

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Actions(
    @PrimaryKey(autoGenerate = true) var id:Int = 0,
    var exercise:Int,
    var timeTook:Long = 0,
    var application: String = "",
    var timestamp: Long = 0,
    var error: Boolean = false,
)
