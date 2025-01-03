package com.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.db.models.entity.Actor
import com.data.db.models.entity.AssessmentType
import com.data.db.models.entity.Competency
import com.data.db.models.entity.Designation
import com.data.db.models.entity.ReferenceIds
import com.data.db.models.entity.Subjects
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataDao {

    //actors
    @Query("SELECT * FROM actors")
    fun getActors(): Flow<List<Actor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActors(actors: List<Actor>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateActors(actor: Actor)

    //assessment types
    @Query("SELECT * FROM assessment_types")
    fun getAssessmentTypes(): Flow<List<AssessmentType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentTypes(assessmentTypes: List<AssessmentType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAssessmentTypes(assessmentType: AssessmentType)

    //subjects
    @Query("SELECT * FROM subjects")
    fun getSubjects(): Flow<List<Subjects>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subjects>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSubjects(subject: Subjects)

    //designations
    @Query("SELECT * FROM designations")
    fun getDesignations(): Flow<List<Designation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesignations(designations: List<Designation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDesignations(designation: Designation)

    //competency
    @Query("SELECT * FROM competencies")
    fun getCompetency(): Flow<List<Competency>>

    @Query("SELECT count(*) FROM competencies")
    fun getCompetencyCount(): Long

    @Query("SELECT * FROM competencies where grade = :grade and learning_outcome like '%Nipun Lakshya%'")
    fun getCompetencyIdsList(grade : Int): List<Competency>

    @Query("SELECT DISTINCT * FROM ref_ids WHERE competency_id IN (:competencyList)")
    fun getRefIdsFromCompetencyIds(competencyList: List<Int>) : List<ReferenceIds>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetency(competencies: List<Competency>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCompetency(competency: Competency)

    //workflow reference ids
    @Query("SELECT * FROM ref_ids")
    fun getReferenceIds(): Flow<List<ReferenceIds>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferenceIds(referenceIds: List<ReferenceIds>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReferenceIds(referenceIds: ReferenceIds)

}