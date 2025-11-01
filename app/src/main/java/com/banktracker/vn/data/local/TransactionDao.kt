package com.banktracker.vn.data.local

import androidx.room.*
import com.banktracker.vn.data.model.BankSummary
import com.banktracker.vn.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE bankCode = :bankCode ORDER BY timestamp DESC")
    fun getTransactionsByBank(bankCode: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getTransactionsSince(startTime: Long): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startTime")
    suspend fun getTotalIncomeSince(startTime: Long): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp >= :startTime")
    suspend fun getTransactionCountSince(startTime: Long): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE timestamp < :timestamp")
    suspend fun deleteOldTransactions(timestamp: Long)

    @Query("SELECT * FROM transactions WHERE amount >= :minAmount AND timestamp >= :startTime ORDER BY amount DESC LIMIT 1")
    suspend fun getLargestTransactionSince(startTime: Long, minAmount: Double = 0.0): Transaction?

    @Query("""
        SELECT bankCode, bankName, 
               SUM(amount) as totalIncome,
               COUNT(*) as transactionCount
        FROM transactions 
        WHERE timestamp >= :startTime
        GROUP BY bankCode
        ORDER BY totalIncome DESC
    """)
    suspend fun getBankSummary(startTime: Long): List<BankSummary>
}