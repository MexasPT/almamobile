package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.Opportunity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDao {
    @Query("SELECT * FROM opportunities ORDER BY timestamp DESC")
    fun getAllOpportunities(): Flow<List<Opportunity>>

    @Query("SELECT * FROM opportunities WHERE timestamp >= :dayStartTimestamp ORDER BY timestamp DESC")
    fun getTodayOpportunities(dayStartTimestamp: Long): Flow<List<Opportunity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunity(opportunity: Opportunity): Long

    @Query("DELETE FROM opportunities WHERE id = :id")
    suspend fun deleteOpportunity(id: Long)
}
