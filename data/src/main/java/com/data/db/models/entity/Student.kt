package com.data.db.models.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.math.BigInteger

@Keep
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val name: String,
    val grade: Int,
    @ColumnInfo(name = "roll_no")
    @SerializedName("roll_no")
    val rollNo: Long,
    @ColumnInfo(name = "is_place_holder_student")
    var isPlaceHolderStudent: Boolean = false
)

