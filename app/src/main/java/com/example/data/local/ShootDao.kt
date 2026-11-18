package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShootDao {
    @Query("SELECT * FROM studio_shoots ORDER BY createdAt DESC")
    fun getAllShoots(): Flow<List<ShootEntity>>

    @Query("SELECT * FROM studio_shoots WHERE id = :id")
    fun getShootById(id: String): Flow<ShootEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoot(shoot: ShootEntity)

    @Query("DELETE FROM studio_shoots WHERE id = :id")
    suspend fun deleteShootById(id: String)
}
