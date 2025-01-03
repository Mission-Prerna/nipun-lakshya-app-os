package com.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.data.db.dao.AssessmentSchoolHistoryDao
import com.data.db.dao.AssessmentStateDao
import com.data.db.dao.AssessmentSubmissionsDao
import com.data.db.dao.MetadataDao
import com.data.db.dao.StudentsAssessmentHistoryDao
import com.data.db.dao.StudentsDao
import com.data.db.models.entity.Actor
import com.data.db.models.entity.AssessmentSchoolHistory
import com.data.db.models.entity.AssessmentState
import com.data.db.models.entity.AssessmentSubmission
import com.data.db.models.entity.AssessmentType
import com.data.db.models.entity.Competency
import com.data.db.models.entity.Designation
import com.data.db.models.entity.ReferenceIds
import com.data.db.models.entity.Student
import com.data.db.models.entity.StudentAssessmentHistory
import com.data.db.models.entity.Subjects
import com.data.db.dao.TeacherPerformanceInsightsDao
import com.data.db.models.TeacherPerformanceInsightsItem

@Database(
    entities = [Student::class, StudentAssessmentHistory::class, Actor::class,
        AssessmentType::class, Competency::class, Subjects::class, Designation::class,
        ReferenceIds::class, AssessmentState::class, AssessmentSubmission::class,
        AssessmentSchoolHistory::class, TeacherPerformanceInsightsItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Convertors::class)
abstract class NLDatabase : RoomDatabase() {

    abstract fun getTeacherPerformanceInsightsDao(): TeacherPerformanceInsightsDao

    abstract fun getStudentsDao(): StudentsDao

    abstract fun getMetadataDao(): MetadataDao

    abstract fun getAssessmentStateDao(): AssessmentStateDao

    abstract fun getAssessmentHistoryDao(): StudentsAssessmentHistoryDao

    abstract fun getAssessmentSubmissionDao(): AssessmentSubmissionsDao

    abstract fun getAssessmentSchoolHistoryDao(): AssessmentSchoolHistoryDao
}
