package eu.kodba.measureuseractions

import androidx.room.*

@Dao
interface ActionsDao {
    @Query("SELECT * FROM Actions")
    fun getAll(): List<Actions>

    @Query("SELECT * FROM Actions WHERE id = :id")
    fun findById(id: Int): List<Actions>

    @Insert
    fun insertAll(vararg polja: Actions): List<Long>

    @Update
    fun update(polje: Actions)

    @Query("SELECT * FROM Actions WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): List<Actions>

    @Delete
    fun delete(polje: Actions)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM Exercise")
    fun getAll(): List<Exercise>

    @Query("SELECT * FROM Exercise WHERE id = :id")
    fun findById(id: Int): List<Exercise>

    @Insert
    fun insertAll(vararg polja: Exercise): List<Long>

    @Update
    fun update(polje: Exercise)

    @Query("SELECT * FROM Exercise WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): List<Exercise>

    @Delete
    fun delete(polje: Exercise)
}