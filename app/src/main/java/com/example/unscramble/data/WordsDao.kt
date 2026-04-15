package com.example.unscramble.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordsDao {

    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<WordsModel>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(words: List<WordsModel>)
}