package com.banktracker.vn.data.repository

import com.banktracker.vn.data.local.TransactionDao
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionSummary
import com.banktracker.vn.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate.time, endDate.time)
    }

    fun getTransactionsByBank(bankCode: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByBank(bankCode)
    }

    suspend fun getTodaySummary(): TransactionSummary {
        val startOfDay = getStartOfDay(Date())
        return getSummarySince(startOfDay.time)
    }

    suspend fun getWeeklySummary(): TransactionSummary {
        val startOfWeek = getStartOfWeek(Date())
        return getSummarySince(startOfWeek.time)
    }

    suspend fun getMonthlySummary(): TransactionSummary {
        val startOfMonth = getStartOfMonth(Date())
        return getSummarySince(startOfMonth.time)
    }

    private suspend fun getSummarySince(timestamp: Long): TransactionSummary {
        val totalIncome = transactionDao.getTotalIncomeSince(timestamp) ?: 0.0
        val count = transactionDao.getTransactionCountSince(timestamp)
        val largest = transactionDao.getLargestTransactionSince(timestamp)?.amount ?: 0.0

        return TransactionSummary(
            totalIncome = totalIncome,
            totalExpense = 0.0,  // Luôn = 0 vì không track tiền ra
            transactionCount = count,
            largestTransaction = largest
        )
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun deleteOldTransactions(daysAgo: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        transactionDao.deleteOldTransactions(calendar.timeInMillis)
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun exportTransactions(startDate: Date, endDate: Date): List<Transaction> {
        return transactionDao.getTransactionsSince(startDate.time)
            .filter { it.timestamp <= endDate.time }
    }

    // Helper functions
    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getStartOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getStartOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}