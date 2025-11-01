package com.banktracker.vn.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.banktracker.vn.R
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionType
import com.banktracker.vn.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding,
        private val onItemClick: (Transaction) -> Unit,
        private val onItemLongClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(transaction: Transaction) {
            binding.apply {
                // Bank name
                tvBankName.text = transaction.bankName

                // Amount - Luôn màu xanh vì chỉ có tiền vào
                val amountText = formatMoney(transaction.amount)
                tvAmount.text = "+$amountText VND"

                val color = ContextCompat.getColor(root.context, R.color.income_green)
                tvAmount.setTextColor(color)

                // Icon - Luôn là income
                ivTransactionIcon.setImageResource(R.drawable.ic_money_income)
                ivTransactionIcon.setColorFilter(color)

                // Content
                tvContent.text = transaction.content

                // Date
                tvDate.text = dateFormat.format(Date(transaction.timestamp))

                // Balance if available
                if (transaction.balance != null) {
                    tvBalance.text = "Số dư: ${formatMoney(transaction.balance)} VND"
                    tvBalance.visibility = android.view.View.VISIBLE
                } else {
                    tvBalance.visibility = android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener { onItemClick(transaction) }
                root.setOnLongClickListener {
                    onItemLongClick(transaction)
                    true
                }
            }
        }

        private fun formatMoney(amount: Double): String {
            return String.format("%,.0f", amount)
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}