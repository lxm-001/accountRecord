package com.mian.accountrecord.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mian.accountrecord.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE openId = :openId LIMIT 1")
    suspend fun getByOpenId(openId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users WHERE openId = :openId")
    suspend fun hasLoginHistory(openId: String): Int
}
