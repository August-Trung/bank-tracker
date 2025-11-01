package com.banktracker.vn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.banktracker.vn.R
import com.banktracker.vn.data.local.AppDatabase
import com.banktracker.vn.data.model.BankCode
import com.banktracker.vn.data.model.TransactionType
import com.banktracker.vn.utils.BankNotificationParser
import com.banktracker.vn.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: PreferencesManager
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioManager: AudioManager
    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady = false

    companion object {
        const val CHANNEL_ID = "bank_tracker_channel"
        const val ACTION_TRANSACTION_DETECTED = "com.banktracker.vn.TRANSACTION_DETECTED"

        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initTextToSpeech()  // Khởi tạo TTS ngay từ đầu
        isServiceRunning = true
        createNotificationChannel()
        startForeground()
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale("vi", "VN"))
                isTTSReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

                if (isTTSReady) {
                    textToSpeech?.setSpeechRate(1.0f)
                    textToSpeech?.setPitch(1.0f)
                    Log.d("BankTracker", "Text-to-Speech initialized successfully")
                } else {
                    Log.e("BankTracker", "Vietnamese language not supported")
                }
            }
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bank Tracker đang chạy")
            .setContentText("Đang theo dõi thông báo ngân hàng...")
            .setSmallIcon(R.drawable.ic_money_income)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        val notification = sbn.notification ?: return

        // Get notification text
        val notificationText = getNotificationText(notification) ?: return

        // Kiểm tra có phải thông báo ngân hàng không
        val bankCode = BankCode.fromPackageName(packageName)

        // Nếu không phải app ngân hàng đã biết, kiểm tra nội dung
        if (bankCode == null) {
            if (!containsBalanceChangeKeywords(notificationText)) {
                return
            }
            Log.d("BankTracker", "Detected unknown bank notification: $packageName")
        } else if (bankCode == BankCode.UNKNOWN) {
            if (!containsBalanceChangeKeywords(notificationText)) {
                return
            }
        }

        // Check if this bank is enabled in settings (nếu là bank đã biết)
        if (bankCode != null && bankCode != BankCode.UNKNOWN && !preferencesManager.isBankEnabled(bankCode.code)) {
            return
        }

        // Parse the notification
        val parsed = BankNotificationParser.parseNotification(notificationText, bankCode) ?: return

        // Check minimum amount threshold
        val minAmount = preferencesManager.getMinimumAmount()
        if (parsed.amount < minAmount) {
            return
        }

        // Create transaction
        val transaction = BankNotificationParser.createTransaction(
            parsed = parsed,
            bankCode = bankCode,
            notificationText = notificationText
        )

        // CHỈ xử lý nếu là TIỀN VÀO
        if (parsed.type != TransactionType.INCOME) {
            return
        }

        // Save to database
        serviceScope.launch {
            database.transactionDao().insertTransaction(transaction)

            // Broadcast to update UI
            val intent = Intent(ACTION_TRANSACTION_DETECTED)
            intent.putExtra("transaction_id", transaction.id)
            sendBroadcast(intent)

            // Show notification and play sound/TTS
            launch(Dispatchers.Main) {
                showCustomNotification(transaction)

                // KIỂM TRA TTS TRƯỚC KHI PHÁT ÂM THANH
                if (preferencesManager.isSoundEnabled()) {
                    if (preferencesManager.isTTSEnabled() && isTTSReady) {
                        // Đọc bằng giọng nói
                        speakWithTTS(transaction)
                    } else {
                        // Phát âm thanh thông báo
                        playNotificationSound()
                    }
                }

                vibrateIfEnabled()

                // Check for large transaction alert
                val largeAmountThreshold = preferencesManager.getLargeAmountThreshold()
                if (parsed.amount >= largeAmountThreshold) {
                    showLargeTransactionAlert(transaction)
                }
            }
        }
    }

    private fun getNotificationText(notification: Notification): String? {
        val extras = notification.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        return when {
            bigText.isNotEmpty() -> "$title $bigText"
            text.isNotEmpty() -> "$title $text"
            else -> null
        }
    }

    private fun containsBalanceChangeKeywords(text: String): Boolean {
        val keywords = listOf(
            "biến động số dư",
            "giao dịch thành công",
            "tiền vào",
            "tiền ra",
            "thanh toán",
            "nhận tiền"
        )
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun showCustomNotification(transaction: com.banktracker.vn.data.model.Transaction) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_money_income)
            .setContentTitle("💰 Nhận tiền ${transaction.bankName}")
            .setContentText("${formatMoney(transaction.amount)} VND")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Số tiền: ${formatMoney(transaction.amount)} VND\nNội dung: ${transaction.content}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(transaction.id.toInt(), notification)
    }

    private fun showLargeTransactionAlert(transaction: com.banktracker.vn.data.model.Transaction) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("🔥 Giao dịch lớn!")
            .setContentText("${formatMoney(transaction.amount)} VND từ ${transaction.bankName}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Giao dịch lớn vừa được phát hiện!\n\nSố tiền: ${formatMoney(transaction.amount)} VND\nNgân hàng: ${transaction.bankName}\nNội dung: ${transaction.content}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun speakWithTTS(transaction: com.banktracker.vn.data.model.Transaction) {
        try {
            // Tăng volume lên max nếu enable
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            if (preferencesManager.isMaxVolumeEnabled()) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            }

            // Lọc bỏ mã giao dịch từ content
            val cleanContent = cleanTransactionContent(transaction.content)

            // Tạo câu đọc
            val amountText = formatMoneyToVietnamese(transaction.amount)
            val message = if (cleanContent.isNotEmpty()) {
                "Bạn vừa nhận được $amountText đồng từ ${transaction.bankName}. Nội dung: $cleanContent"
            } else {
                "Bạn vừa nhận được $amountText đồng từ ${transaction.bankName}"
            }

            Log.d("BankTracker", "Speaking: $message")

            // Đọc
            val utteranceId = "transaction_${transaction.id}"
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            // Restore volume sau khi đọc xong
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("BankTracker", "TTS started")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d("BankTracker", "TTS completed")
                    if (preferencesManager.isMaxVolumeEnabled()) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e("BankTracker", "TTS error")
                    if (preferencesManager.isMaxVolumeEnabled()) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("BankTracker", "TTS Exception", e)
            // Fallback to sound nếu TTS lỗi
            playNotificationSound()
        }
    }

    private fun formatMoneyToVietnamese(amount: Double): String {
        val billions = (amount / 1_000_000_000).toInt()
        val millions = ((amount % 1_000_000_000) / 1_000_000).toInt()
        val thousands = ((amount % 1_000_000) / 1_000).toInt()

        val parts = mutableListOf<String>()

        if (billions > 0) {
            parts.add("$billions tỷ")
        }
        if (millions > 0) {
            parts.add("$millions triệu")
        }
        if (thousands > 0) {
            parts.add("$thousands nghìn")
        }

        return if (parts.isEmpty()) {
            "${amount.toInt()}"
        } else {
            parts.joinToString(" ")
        }
    }

    private fun cleanTransactionContent(content: String): String {
        // Loại bỏ mã giao dịch (Trace, Ma giao dich, MGD, etc.)
        var cleaned = content

        // Regex patterns để loại bỏ mã giao dịch
        val patterns = listOf(
            Regex("""Trace\s*\d+""", RegexOption.IGNORE_CASE),
            Regex("""Ma giao dich[:\s]+[A-Z0-9]+""", RegexOption.IGNORE_CASE),
            Regex("""MGD[:\s]+[A-Z0-9]+""", RegexOption.IGNORE_CASE),
            Regex("""FT\d+[A-Z0-9]+""", RegexOption.IGNORE_CASE),
            Regex("""\b[A-Z]{2,}\d{6,}\b"""),  // Các mã dạng ABC123456
            Regex("""\d{6,}""")  // Các số dài (mã giao dịch)
        )

        patterns.forEach { pattern ->
            cleaned = pattern.replace(cleaned, "")
        }

        // Loại bỏ khoảng trắng thừa
        cleaned = cleaned.trim().replace(Regex("""\s+"""), " ")

        return cleaned
    }

    private fun playNotificationSound() {
        try {
            mediaPlayer?.release()

            val soundUri = preferencesManager.getCustomSoundUri()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer.create(this, soundUri)

            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            if (preferencesManager.isMaxVolumeEnabled()) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            }

            mediaPlayer?.setOnCompletionListener {
                if (preferencesManager.isMaxVolumeEnabled()) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                }
                it.release()
            }

            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibrateIfEnabled() {
        if (!preferencesManager.isVibrationEnabled()) return

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bank Transaction Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for bank transactions"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatMoney(amount: Double): String {
        return String.format("%,.0f", amount)
    }
}