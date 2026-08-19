package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM vtiger_account_cache ORDER BY accountName ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM vtiger_account_cache WHERE accountName LIKE '%' || :query || '%' ORDER BY accountName ASC")
    fun searchClients(query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Query("DELETE FROM vtiger_account_cache")
    suspend fun clearClients()
}
