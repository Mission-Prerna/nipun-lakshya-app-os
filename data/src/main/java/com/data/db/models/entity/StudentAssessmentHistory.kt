package com.data.db.models.entity

import androidx.annotation.Keep
import androidx.room.*
import com.google.gson.annotations.SerializedName
import java.util.*

@Keep
@Entity(tableName = "students_assessment_history",
    primaryKeys = ["id", "month","year"],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["id"] // This references the "id" column in the Student table
        )
    ])
data class StudentAssessmentHistory(
    val id: String,
    val status: String,
    @ColumnInfo(name = "last_assessment_date")
    @SerializedName("last_assessment_date")
    val lastAssessmentDate: Long,
    val month: Int,
    val year : Int
)

