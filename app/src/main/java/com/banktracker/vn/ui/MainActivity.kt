package com.banktracker.vn.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.banktracker.vn.R
import com.banktracker.vn.databinding.ActivityMainBinding
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionType
import com.banktracker.vn.service.BankNotificationListenerService
import com.banktracker.vn.ui.adapter.TransactionAdapter
import com.banktracker.vn.ui.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private val transactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BankNotificationListenerService.ACTION_TRANSACTION_DETECTED) {
                viewModel.loadSummaries()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        checkNotificationPermission()

        // Register broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                transactionReceiver,
                IntentFilter(BankNotificationListenerService.ACTION_TRANSACTION_DETECTED),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                transactionReceiver,
                IntentFilter(BankNotificationListenerService.ACTION_TRANSACTION_DETECTED)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(transactionReceiver)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetails(transaction)
            },
            onItemLongClick = { transaction ->
                showDeleteDialog(transaction)
            }
        )

        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupObservers() {
        // Observe all transactions
        viewModel.allTransactions.observe(this) { transactions ->
            transactionAdapter.submitList(transactions)
            binding.layoutEmptyState.visibility = if (transactions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Observe today summary (default)
        viewModel.todaySummary.observe(this) { summary ->
            updateSummaryUI(summary)
        }
    }

    private fun setupListeners() {
        // Tab layout
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.todaySummary.observe(this@MainActivity) { updateSummaryUI(it) }
                    1 -> viewModel.weeklySummary.observe(this@MainActivity) { updateSummaryUI(it) }
                    2 -> viewModel.monthlySummary.observe(this@MainActivity) { updateSummaryUI(it) }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Service enable button
        binding.btnEnableService.setOnClickListener {
            openNotificationSettings()
        }

        // FAB
        binding.fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateSummaryUI(summary: com.banktracker.vn.data.model.TransactionSummary) {
        binding.tvTotalIncome.text = "${formatMoney(summary.totalIncome)} VND"
        binding.tvTransactionCount.text = "${summary.transactionCount} giao dịch"
    }

    private fun checkNotificationPermission() {
        val isEnabled = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (isEnabled) {
            binding.cardServiceStatus.visibility = View.GONE
        } else {
            binding.cardServiceStatus.visibility = View.VISIBLE
            binding.tvServiceStatus.text = "⚠️ Chưa cấp quyền đọc thông báo"
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun showTransactionDetails(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Chi tiết giao dịch")
            .setMessage("""
                Ngân hàng: ${transaction.bankName}
                Số tiền: ${formatMoney(transaction.amount)} VND
                Nội dung: ${transaction.content}
                ${if (transaction.balance != null) "Số dư: ${formatMoney(transaction.balance!!)} VND" else ""}
                Thời gian: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))}
            """.trimIndent())
            .setPositiveButton("Đóng", null)
            .setNegativeButton("Xóa") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .show()
    }

    private fun showDeleteDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc muốn xóa giao dịch này?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                showExportDialog()
                true
            }
            R.id.action_delete_all -> {
                showDeleteAllDialog()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExportDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Export dữ liệu")
            .setMessage("Tính năng export sẽ xuất toàn bộ giao dịch ra file Excel")
            .setPositiveButton("Export") { _, _ ->
                // TODO: Implement export functionality
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa tất cả")
            .setMessage("Bạn có chắc muốn xóa toàn bộ lịch sử giao dịch?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteAllTransactions()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun formatMoney(amount: Double): String {
        return String.format("%,.0f", amount)
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
        viewModel.loadSummaries()

//        checkAndRestartService()
    }

//    private fun checkAndRestartService() {
//        val isEnabled = NotificationManagerCompat.getEnabledListenerPackages(this)
//            .contains(packageName)
//
//        if (isEnabled && !BankNotificationListenerService.isRunning()) {
//            try {
//                val serviceIntent = Intent(this, BankNotificationListenerService::class.java)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    startForegroundService(serviceIntent)
//                } else {
//                    startService(serviceIntent)
//                }
//            } catch (e: Exception) {
//            }
//        }
//    }
}