package com.assessment.studentselection

import com.data.db.models.entity.Student

sealed class StudentScreenStates {
    object Loading : StudentScreenStates()
    class Error(val t: Throwable) : StudentScreenStates()
    class Success(val studentList: MutableList<Student>) : StudentScreenStates()
}