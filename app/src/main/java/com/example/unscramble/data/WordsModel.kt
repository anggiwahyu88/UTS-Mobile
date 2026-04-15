package com.example.unscramble.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity("words")
data class WordsModel(

    @PrimaryKey(true)
    @field:SerializedName("index")
    val index: Int = 0,

    @field:SerializedName("word")
    val word: String? = null,

    )
