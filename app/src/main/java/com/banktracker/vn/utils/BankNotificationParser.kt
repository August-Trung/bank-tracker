package com.banktracker.vn.utils

import android.util.Log
import com.banktracker.vn.data.model.BankCode
import com.banktracker.vn.data.model.Transaction
import com.banktracker.vn.data.model.TransactionType

object BankNotificationParser {

    private const val TAG = "BankNotificationParser"

    data class ParsedTransaction(
        val amount: Double,
        val type: TransactionType,
        val content: String,
        val balance: Double?,
        val timestamp: Long
    )

    // ============================
    // 🔹 Regex chung cho tiền vào
    // ============================
    private val COMMON_INCOME_PATTERNS = listOf(
        Regex("""\+\s*([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung|NoiDung|Noidung)[::\s]+(.+?)(?:\s*(?:SD|So du|Sodu)|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""(?:tang|nhan|cong|\+)\s*([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung)[::\s]+(.+?)(?:\s*(?:SD|So du)|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""TK.*?\+\s*([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung)[::\s]+(.+?)(?:\s*(?:SD|So du)|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""bien\s*dong.*?tang.*?([0-9,]+(?:\.[0-9]+)?)\s*VND.*?(?:ND|Noi dung)[::\s]*(.+?)(?:\s*SD|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )

    // ============================
    // 🔹 Regex riêng cho ngân hàng đặc thù
    // ============================
    private val BANK_PATTERNS: Map<BankCode, List<Regex>> = mapOf(

        // 🏦 MSB
        BankCode.MSB to listOf(
            Regex("""TK\s*[\d*]+\s*(ghi có|ghi nợ)\s*([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE),
            Regex("""MSB.*?([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE),
            Regex("""(?:MSB[:\-]?\s*)?TK\s*[\d*]+\s*ghi có\s*([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE)
        ),

        // 💜 Timo
        BankCode.TIMO to listOf(
            Regex("""Timo.*?(nhận|ghi có)\s*([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE),
            Regex("""Bạn.*?(nhận|ghi có)\s*([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE),
            Regex("""(Số\s*dư\s*tài\s*khoản\s*vừa\s*tăng)\s*([0-9.,]+)\s*(VND|₫|đ)?""", RegexOption.IGNORE_CASE)

        ),

        // 💸 MoMo
        BankCode.MOMO to listOf(
            Regex("""(Bạn\s*(đã|vừa)?\s*(nhận|nhận được)|Nhận tiền)\s*([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE),
            Regex("""MoMo.*?([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE),
            Regex("""Nhận tiền.*?(Số tiền|so tien)\s*([0-9.,]+)\s*(đ|₫|vnd)?""", RegexOption.IGNORE_CASE)
        )
    )

    // ============================
    // 🔹 Regex cho SỐ DƯ
    // ============================
    private val BALANCE_PATTERNS = listOf(
        Regex("""(?:SD|So du|Sodu)[::\s]*([0-9,]+(?:\.[0-9]+)?)\s*VND""", RegexOption.IGNORE_CASE),
        Regex("""Balance[::\s]*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    )

    // ============================
    // 🔹 Hàm chính parse thông báo
    // ============================
    fun parseNotification(
        notificationText: String,
        bankCode: BankCode?
    ): ParsedTransaction? {

        Log.d(TAG, "Parsing notification from ${bankCode?.fullName ?: "Unknown"}")
        Log.d(TAG, "Text: $notificationText")

        // Bỏ qua nếu là giao dịch chi
        if (isExpenseNotification(notificationText)) {
            Log.d(TAG, "Skipped: Expense notification")
            return null
        }

        // Thử parse theo pattern riêng từng ngân hàng
        if (bankCode != null) {
            BANK_PATTERNS[bankCode]?.forEach { pattern ->
                val match = pattern.find(notificationText)
                if (match != null) {
                    val groups = match.groupValues
                    val amountText = groups.lastOrNull()?.replace("[^\\d.,]".toRegex(), "") ?: "0"
                    val amount = amountText.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0

                    val type = if (notificationText.contains("ghi có", true) ||
                        notificationText.contains("nhận", true)
                    ) TransactionType.INCOME else TransactionType.EXPENSE

                    Log.d(TAG, "Parsed ${bankCode.name}: $amount VND")

                    return ParsedTransaction(
                        amount = amount,
                        type = type,
                        content = notificationText.take(200),
                        balance = null,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }

        // Thử parse với regex chung
        for (pattern in COMMON_INCOME_PATTERNS) {
            val matchResult = pattern.find(notificationText)
            if (matchResult != null) {
                return parseMatch(matchResult, notificationText)
            }
        }

        Log.w(TAG, "Failed to parse notification for ${bankCode?.fullName ?: "Unknown"}")
        return null
    }

    // ============================
    // 🔹 Kiểm tra có phải giao dịch chi không
    // ============================
    private fun isExpenseNotification(text: String): Boolean {
        val expenseKeywords = listOf(
            "giam", "tru", "chi", "rut", "thanh toan",
            "chuyen di", "chuyen tien", "payment",
            "-", "minus", "debit"
        )

        // Nếu có "+" thì là tiền vào
        if (text.contains("+")) return false

        // Nếu có "tang" hoặc "nhan" thì là tiền vào
        if (text.contains("tang", ignoreCase = true) ||
            text.contains("nhan", ignoreCase = true) ||
            text.contains("cong", ignoreCase = true)
        ) {
            return false
        }

        return expenseKeywords.any { text.contains(it, ignoreCase = true) }
    }

    // ============================
    // 🔹 Hàm parse cho regex chung
    // ============================
    private fun parseMatch(
        match: MatchResult,
        fullText: String
    ): ParsedTransaction? {
        return try {
            val groups = match.groupValues
            val amountStr = groups.getOrNull(1)?.replace(",", "")?.replace(" ", "") ?: return null
            val amount = amountStr.toDoubleOrNull() ?: return null

            val content = groups.getOrNull(2)?.trim()?.take(200) ?: "Không có nội dung"

            var balance: Double? = null
            for (balancePattern in BALANCE_PATTERNS) {
                val balanceMatch = balancePattern.find(fullText)
                if (balanceMatch != null) {
                    balance = balanceMatch.groupValues.getOrNull(1)
                        ?.replace(",", "")
                        ?.toDoubleOrNull()
                    if (balance != null) break
                }
            }

            Log.d(TAG, "Parsed successfully: $amount VND - $content")

            ParsedTransaction(
                amount = amount,
                type = TransactionType.INCOME,
                content = content,
                balance = balance,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
            null
        }
    }

    // ============================
    // 🔹 Tạo Transaction để lưu DB
    // ============================
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
