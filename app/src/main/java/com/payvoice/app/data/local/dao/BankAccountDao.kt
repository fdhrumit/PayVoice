package com.payvoice.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.payvoice.app.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY id ASC")
    fun getAllAccountsFlow(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE isEnabled = 1")
    fun getEnabledAccountsFlow(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE isEnabled = 1")
    fun getEnabledAccountsSync(): List<BankAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccountEntity): Long

    @Update
    suspend fun updateAccount(account: BankAccountEntity)

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("SELECT COUNT(*) FROM bank_accounts")
    suspend fun getAccountCount(): Int
}
