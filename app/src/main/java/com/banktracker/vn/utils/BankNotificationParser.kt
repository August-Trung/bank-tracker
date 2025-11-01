package com.banktracker.vn.utils

import android.util.Log
import com.banktracker.vn.data.model.BankCode
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionType

object BankNotificationParser {

    private const val TAG = "BankNotificationParser"

    data class ParsedTransaction(
        val amount: Double,
        val type: TransactionType,  // ← ĐÃ THÊM THUỘC TÍNH NÀY
        val content: String,
        val balance: Double?,
        val timestamp: Long
    )

    // Regex chung cho TIỀN VÀO (áp dụng cho hầu hết ngân hàng)
    private val COMMON_INCOME_PATTERNS = listOf(
        // Pattern 1: +1,000,000VND ... ND: ... SD: ...
        Regex("""\+\s*([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung|NoiDung|Noidung)[::\s]+(.+?)(?:\s*(?:SD|So du|Sodu)|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

        // Pattern 2: tang/nhan/cong ... VND
        Regex("""(?:tang|nhan|cong|\+)\s*([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung)[::\s]+(.+?)(?:\s*(?:SD|So du)|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

        // Pattern 3: TK ... +... VND
        Regex("""TK.*?\+\s*([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung)[::\s]+(.+?)(?:\s*(?:SD|So du)|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),

        // Pattern 4: Bien dong tang (không có ký hiệu +)
        Regex("""bien\s*dong.*?tang.*?([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung)[::\s]*(.+?)(?:\s*SD|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )

    // Regex cho SỐ DƯ (chung cho tất cả)
    private val BALANCE_PATTERNS = listOf(
        Regex("""(?:SD|So du|Sodu)[::\s]*([0-9,]+(?:\.[0-9]+)?)\s*VND""", RegexOption.IGNORE_CASE),
        Regex("""Balance[::\s]*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    )

    fun parseNotification(
        notificationText: String,
        bankCode: BankCode?
    ): ParsedTransaction? {

        Log.d(TAG, "Parsing notification from ${bankCode?.fullName ?: "Unknown"}")
        Log.d(TAG, "Text: $notificationText")

        // Bỏ qua nếu có từ khóa TIỀN RA
        if (isExpenseNotification(notificationText)) {
            Log.d(TAG, "Skipped: Expense notification")
            return null
        }

        // Thử parse với các pattern chung
        for (pattern in COMMON_INCOME_PATTERNS) {
            val matchResult = pattern.find(notificationText)
            if (matchResult != null) {
                return parseMatch(matchResult, notificationText)
            }
        }

        Log.w(TAG, "Failed to parse notification")
        return null
    }

    private fun isExpenseNotification(text: String): Boolean {
        val expenseKeywords = listOf(
            "giam", "tru", "chi", "rut", "thanh toan",
            "chuyen di", "chuyen tien", "payment",
            "-", "minus", "debit"
        )

        // Nếu có "+" thì chắc chắn là tiền vào
        if (text.contains("+")) return false

        // Nếu có "tang" hoặc "nhan" thì là tiền vào
        if (text.contains("tang", ignoreCase = true) ||
            text.contains("nhan", ignoreCase = true) ||
            text.contains("cong", ignoreCase = true)) {
            return false
        }

        return expenseKeywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun parseMatch(
        match: MatchResult,
        fullText: String
    ): ParsedTransaction? {
        try {
            val groups = match.groupValues

            // groups[1] = amount, groups[2] = content
            val amountStr = groups.getOrNull(1)?.replace(",", "")?.replace(" ", "") ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null

            val content = groups.getOrNull(2)?.trim()?.take(200) ?: "Không có nội dung"

            // Parse balance
            var balance: Double? = null
            for (balancePattern in BALANCE_PATTERNS) {
                val balanceMatch = balancePattern.find(fullText)
                if (balanceMatch != null) {
                    balance = balanceMatch.groupValues.getOrNull(1)
                        ?.replace(",", "")
                        ?.toDoubleOrNull()
                    if (balance != null) {
                        break
                    }
                }
            }

            Log.d(TAG, "Parsed successfully: $amount VND - $content")

            return ParsedTransaction(
                amount = amount,
                type = TransactionType.INCOME,
                content = content,
                balance = balance,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            return null
        }
    }

    fun createTransaction(
        parsed: ParsedTransaction,
        bankCode: BankCode?,
        notificationText: String
    ): Transaction {
        return Transaction(
            bankName = bankCode?.fullName ?: "Ngân hàng khác",
            bankCode = bankCode?.code ?: "UNKNOWN",
            amount = parsed.amount,
            transactionType = parsed.type,
            content = parsed.content,
            balance = parsed.balance,
            timestamp = parsed.timestamp,
            notificationText = notificationText
        )
    }
}