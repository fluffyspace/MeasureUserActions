package eu.kodba.measureuseractions

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Actions(
    @PrimaryKey(autoGenerate = true) var id:Int = 0,
    var exercise:Int,
    var timeTook:Long = 0,
    var additionalInfo1: String = "",
    var additionalInfo2: String = "",
    var additionalInfo3: String = "",
    var standardOrAlternative: Boolean = false,
    var personName: String = "",
    var uploaded: Boolean = false,
    var timestamp: Long = 0,
)
