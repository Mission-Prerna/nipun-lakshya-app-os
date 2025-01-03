package com.data.repository.impl

import com.data.db.dao.AssessmentSubmissionsDao
import com.data.db.dao.StudentsAssessmentHistoryDao
import com.data.db.dao.StudentsDao
import com.data.db.models.entity.Student
import com.data.db.models.StudentAssessmentHistoryCompleteInfo
import com.data.db.models.entity.StudentAssessmentHistory
import com.data.db.models.helper.StudentWithAssessmentHistory
import com.data.network.Result
import com.data.network.StudentsService
import com.data.repository.StudentsRepository
import com.samagra.commons.AppPreferences.getUserAuth
import com.samagra.commons.constants.Constants
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StudentsRepositoryImpl @Inject constructor(
    private val service: StudentsService,
    private val studentsDao: StudentsDao,
    private val studentsAssessmentHistoryDao: StudentsAssessmentHistoryDao,
    private val submissionsDao: AssessmentSubmissionsDao
) : StudentsRepository() {

    override fun getStudents(grade: Int): Flow<MutableList<Student>> {
        return studentsDao.getStudents(grade)
    }

    override fun getGradesList(): Flow<List<Int>> {
        return studentsDao.getGradesList()
    }

    override suspend fun fetchStudents(udise: Long): Result<Unit> {
        try {
            val response = service.getStudents(udise.toString(), Constants.BEARER_ + getUserAuth())
            if (response.isSuccessful) {
                var students = response.body()
                val dummyStudents = getDummyStudents()
                if (students == null) {
                    students = mutableListOf()
                }
                students.addAll(dummyStudents)
                studentsDao.insert(students)
                return Result.Success(Unit)
            }
            return Result.Error(Exception(response.errorBody()?.string() ?: response.message()))
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    override suspend fun addDummyStudents() {
        studentsDao.insert(getDummyStudents())
    }

    override suspend fun fetchStudentsAssessmentHistory(
        udise: Long,
        grade: String,
        month: Int,
        year: Int
    ): Result<StudentAssessmentHistoryCompleteInfo?> {
        try {
            val response = service.getStudentAssessmentHistoryInfo(
                udise.toString(),
                Constants.BEARER_ + getUserAuth(),
                "hi",
                grade,
                month,
                year
            )
            if (response.isSuccessful) {
                val studentsAssessmentHistoryInfo = response.body()
                var allStudents = mutableListOf<StudentAssessmentHistory>()
                studentsAssessmentHistoryInfo?.forEach { entry ->
                    val modifiedStudents = entry.students.map { originalStudent ->
                        StudentAssessmentHistory(
                            id = originalStudent.id,
                            status = originalStudent.status,
                            lastAssessmentDate = originalStudent.lastAssessmentDate,
                            month = month,
                            year = year
                        )
                    }
                    allStudents.addAll(modifiedStudents)
                }

                allStudents = modifyEntriesWihOfflineData(allStudents, month, year)
                studentsAssessmentHistoryDao.insert(allStudents)
                return Result.Success(studentsAssessmentHistoryInfo?.get(0))
            }
            return Result.Error(Exception(response.errorBody()?.string() ?: response.message()))
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    private suspend fun modifyEntriesWihOfflineData(
        modifiedStudents: MutableList<StudentAssessmentHistory>,
        month: Int,
        year: Int
    ): MutableList<StudentAssessmentHistory> {
        val localEntriesMap = studentsAssessmentHistoryDao.getAllHistories(month, year)
            .associateBy { it.id }
        // Modify the entries based on offline data & If there is no local entry, use the server entry
        return modifiedStudents.map { serverEntry ->
            localEntriesMap[serverEntry.id]?.let { localEntry ->
                if (localEntry.lastAssessmentDate > serverEntry.lastAssessmentDate) {
                    localEntry
                } else {
                    serverEntry
                }
            } ?: serverEntry
        }.toMutableList()
    }

    override fun getStudentsAssessmentHistory(
        grade: Int,
        month: Int,
        year: Int
    ): Flow<MutableList<StudentWithAssessmentHistory>> {
        return studentsAssessmentHistoryDao.getStudentsByGradeMonthYear(grade, month, year)
    }

    private fun getDummyStudents(): MutableList<Student> {
        val dummyStudents = mutableListOf<Student>()
        val range = -3..-1
        for (i in range) {
            val virtualId = i.toString()
            dummyStudents.add(
                Student(
                    id = virtualId,
                    name = "",
                    i * -1,
                    rollNo = i.toLong(),
                    isPlaceHolderStudent = true
                )
            )
        }
        return dummyStudents
    }


}