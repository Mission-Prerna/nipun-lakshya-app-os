package com.data.repository

import com.data.db.models.entity.Student
import com.data.db.models.StudentAssessmentHistoryCompleteInfo
import com.data.db.models.helper.StudentWithAssessmentHistory
import kotlinx.coroutines.flow.Flow
import com.data.network.Result
import com.samagra.commons.basemvvm.BaseRepository

abstract class StudentsRepository : BaseRepository() {

    abstract fun getStudents(grade : Int): Flow<MutableList<Student>>
    abstract fun getGradesList(): Flow<List<Int>>
    abstract suspend fun fetchStudents(udise : Long) : Result<Unit>
    abstract suspend fun fetchStudentsAssessmentHistory(udise: Long, grade: String, month: Int, year: Int) : Result<StudentAssessmentHistoryCompleteInfo?>
    abstract fun getStudentsAssessmentHistory(grade : Int, month: Int, year: Int) : Flow<MutableList<StudentWithAssessmentHistory>>
    abstract suspend fun addDummyStudents()

}