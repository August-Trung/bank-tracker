package com.banktracker.vn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.banktracker.vn.data.local.AppDatabase
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionSummary
import com.banktracker.vn.data.model.TransactionType
import com.banktracker.vn.data.repository.TransactionRepository
import com.banktracker.vn.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val preferencesManager: PreferencesManager

    val allTransactions: LiveData<List<Transaction>>

    private val _todaySummary = MutableLiveData<TransactionSummary>()
    val todaySummary: LiveData<TransactionSummary> = _todaySummary

    private val _weeklySummary = MutableLiveData<TransactionSummary>()
    val weeklySummary: LiveData<TransactionSummary> = _weeklySummary

    private val _monthlySummary = MutableLiveData<TransactionSummary>()
    val monthlySummary: LiveData<TransactionSummary> = _monthlySummary

    private val _filteredTransactions = MutableLiveData<List<Transaction>>()
    val filteredTransactions: LiveData<List<Transaction>> = _filteredTransactions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
        preferencesManager = PreferencesManager(application)
        allTransactions = repository.getAllTransactions().asLiveData()

        loadSummaries()
    }

    fun loadSummaries() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _todaySummary.value = repository.getTodaySummary()
                _weeklySummary.value = repository.getWeeklySummary()
                _monthlySummary.value = repository.getMonthlySummary()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByBank(bankCode: String?) {
        if (bankCode == null) {
            allTransactions.value?.let { _filteredTransactions.value = it }
        } else {
            viewModelScope.launch {
                repository.getTransactionsByBank(bankCode).collect {
                    _filteredTransactions.value = it
                }
            }
        }
    }

    fun filterByDateRange(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            repository.getTransactionsByDateRange(startDate, endDate).collect {
                _filteredTransactions.value = it
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            loadSummaries()
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            loadSummaries()
        }
    }

    fun deleteOldTransactions(daysAgo: Int) {
        viewModelScope.launch {
            repository.deleteOldTransactions(daysAgo)
            loadSummaries()
        }
    }

    suspend fun exportTransactions(startDate: Date, endDate: Date): List<Transaction> {
        return repository.exportTransactions(startDate, endDate)
    }

    fun getPreferencesManager(): PreferencesManager = preferencesManager
}