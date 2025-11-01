package com.banktracker.vn.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.banktracker.vn.databinding.ActivityTransactionDetailBinding
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    companion object {
        const val EXTRA_TRANSACTION = "extra_transaction"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadTransactionData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Chi tiết giao dịch"
        }
    }

    private fun loadTransactionData() {
        val transaction = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TRANSACTION, Transaction::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TRANSACTION)
        }

        transaction?.let { displayTransaction(it) } ?: finish()
    }

    private fun displayTransaction(transaction: Transaction) {
        binding.apply {
            tvBankName.text = transaction.bankName
            val amountText = formatMoney(transaction.amount)
            tvAmount.text = "$amountText VND"
            tvAmount.setTextColor(getColor(com.banktracker.vn.R.color.income_green))

            // Loại giao dịch (cố định)
            tvTransactionType.text = "Tiền vào"
            tvTransactionType.setTextColor(getColor(com.banktracker.vn.R.color.income_green))

            tvContent.text = transaction.content
            tvDateTime.text = dateFormat.format(Date(transaction.timestamp))
            tvBalance.text = transaction.balance?.let { "${formatMoney(it)} VND" } ?: "Không có thông tin"
            tvNotificationText.text = transaction.notificationText
        }
    }


    private fun formatMoney(amount: Double): String {
        return String.format("%,.0f", amount)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}