package com.data.db.dao

import androidx.room.*
import com.data.db.models.entity.StudentAssessmentHistory
import com.data.db.models.helper.StudentWithAssessmentHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentsAssessmentHistoryDao {

    @Transaction
    @Query(
        "SELECT students.id, students.name, students.grade, students.roll_no, students.is_place_holder_student, " +
                "students_assessment_history.status, students_assessment_history.last_assessment_date, students_assessment_history.month " +
                "FROM students " +
                "LEFT JOIN students_assessment_history ON students.id = students_assessment_history.id " + // Use the "id" column as the foreign key
                "WHERE students.grade = :grade " +
                "AND is_place_holder_student = 0 " +
                "AND students_assessment_history.month = :month " +
                "AND students_assessment_history.year = :year ORDER BY students.name ASC"
    )
    fun getStudentsByGradeMonthYear(grade: Int, month: Int, year: Int): Flow<MutableList<StudentWithAssessmentHistory>>

    @Query("SELECT * from students_assessment_history where month = :month and year = :year")
    suspend fun getAllHistories(month: Int, year: Int): List<StudentAssessmentHistory>

    @Query("SELECT students.id, students.name, students.grade, students.roll_no, students.is_place_holder_student, " +
            "students_assessment_history.status, students_assessment_history.last_assessment_date, students_assessment_history.month " +
            "FROM students " +
            "LEFT JOIN students_assessment_history ON students.id = students_assessment_history.id " + // Use the "id" column as the foreign key
            "WHERE students.id = :studentId " +
            "AND is_place_holder_student = 0 " +
            "AND students_assessment_history.month = :month " +
            "AND students_assessment_history.year = :year")
    suspend fun getHistoryByAssessmentInfo(studentId :String, month : Int, year : Int) : StudentWithAssessmentHistory

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(studentAssessmentHistoryList: List<StudentAssessmentHistory>)
    //timestamp check with column value in this table

}
