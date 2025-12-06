package com.banktracker.vn.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import kotlinx.coroutines.withContext
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
        const val ACTION_RESTART_SERVICE = "com.banktracker.vn.RESTART_SERVICE"

        @Volatile
        private var isServiceRunning = false

        fun isRunning(): Boolean = isServiceRunning
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BankTracker", "Service onCreate()")
        database = AppDatabase.getDatabase(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initTextToSpeech()
        isServiceRunning = true
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BankTracker", "Service onStartCommand() - Action: ${intent?.action}")
        return START_STICKY
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

        Log.d("BankTracker", "Foreground service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BankTracker", "Service onDestroy()")

        isServiceRunning = false
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        // Schedule restart ONLY IF user opted-in
        if (preferencesManager.isAutoRestartEnabled()) {
            scheduleServiceRestart()
        } else {
            Log.d("BankTracker", "Auto restart disabled by user; not scheduling restart")
        }
    }

    private fun scheduleServiceRestart() {
        val restartIntent = Intent(applicationContext, ServiceRestartBroadcastReceiver::class.java).apply {
            action = ACTION_RESTART_SERVICE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val restartTime = System.currentTimeMillis() + 2000 // 2s

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, restartTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, restartTime, pendingIntent)
            }
            Log.d("BankTracker", "Service restart scheduled in 2s")
        } catch (e: Exception) {
            Log.e("BankTracker", "Failed to schedule restart", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            super.onNotificationPosted(sbn)

            val packageName = sbn.packageName
            val notification = sbn.notification ?: return

            // Get notification text
            val notificationText = getNotificationText(notification) ?: return

            Log.d("BankTracker", "Notification from: $packageName")
            Log.d("BankTracker", "Text: ${notificationText.take(100)}")

            // Kiểm tra có phải app ngân hàng đã biết
            val bankCode = BankCode.fromPackageName(packageName)

            // Nếu là unknown bank và user không cho phép -> ignore
            if (bankCode == null || bankCode == BankCode.UNKNOWN) {
                if (!preferencesManager.isAllowUnknownBanks()) {
                    Log.d("BankTracker", "Ignoring unknown bank notification: $packageName")
                    return
                } else {
                    Log.d("BankTracker", "Processing unknown bank (opt-in) notification: $packageName")
                }
            }

            // Check if this bank is enabled in settings (nếu là bank đã biết)
            if (bankCode != null && bankCode != BankCode.UNKNOWN && !preferencesManager.isBankEnabled(bankCode.code)) {
                Log.d("BankTracker", "Bank ${bankCode.code} is disabled")
                return
            }

            // Parse the notification
            val parsed = BankNotificationParser.parseNotification(notificationText, bankCode) ?: return

            // Check minimum amount threshold
            val minAmount = preferencesManager.getMinimumAmount()
            if (parsed.amount < minAmount) {
                Log.d("BankTracker", "Amount ${parsed.amount} < minimum $minAmount")
                return
            }

            // CHỈ xử lý nếu là TIỀN VÀO
            if (parsed.type != TransactionType.INCOME) {
                return
            }

            val transaction = BankNotificationParser.createTransaction(
                parsed = parsed,
                bankCode = bankCode,
                notificationText = notificationText
            )

            Log.d("BankTracker", "Processing transaction: ${transaction.amount} VND")

            // Save to database and notify UI / play sound/TTS
            serviceScope.launch {
                database.transactionDao().insertTransaction(transaction)

                // Broadcast to update UI
                val intent = Intent(ACTION_TRANSACTION_DETECTED)
                intent.putExtra("transaction_id", transaction.id)
                sendBroadcast(intent)

                // Notification + vibration on background thread
                withContext(Dispatchers.Default) {
                    showCustomNotification(transaction)
                    vibrateIfEnabled()
                }

                // Sound/TTS on main thread
                withContext(Dispatchers.Main) {
                    if (preferencesManager.isSoundEnabled()) {
                        if (preferencesManager.isTTSEnabled() && isTTSReady) {
                            speakWithTTS(transaction)
                        } else {
                            playNotificationSound()
                        }
                    }

                    val largeAmountThreshold = preferencesManager.getLargeAmountThreshold()
                    if (parsed.amount >= largeAmountThreshold) {
                        showLargeTransactionAlert(transaction)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BankService", "Error handling notification: ${e.stackTraceToString()}")
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
            "biến động số dư", "giao dịch thành công", "tiền vào",
            "tiền ra", "thanh toán", "nhận tiền", "+.*VND", "balance"
        )
        return keywords.any { keyword ->
            if (keyword.contains(".*")) {
                Regex(keyword, RegexOption.IGNORE_CASE).find(text) != null
            } else {
                text.contains(keyword, ignoreCase = true)
            }
        }
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
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            if (preferencesManager.isMaxVolumeEnabled()) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
            }

            val amount = transaction.amount
            val type = if (amount > 0) "Đã nhận" else "Đã chi"

            val amountText = formatMoneyToVietnamese(amount)
            val message = "$type $amountText"

            Log.d("BankTracker", "Speaking: $message")

            val utteranceId = "transaction_${transaction.id}"
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (preferencesManager.isMaxVolumeEnabled()) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    }
                }
                override fun onError(utteranceId: String?) {
                    if (preferencesManager.isMaxVolumeEnabled()) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("BankTracker", "TTS Exception", e)
            playNotificationSound()
        }
    }

    private fun formatMoneyToVietnamese(amount: Double): String {
        val absAmount = amount.toLong()
        val billions = absAmount / 1_000_000_000
        val millions = (absAmount % 1_000_000_000) / 1_000_000
        val thousands = (absAmount % 1_000_000) / 1_000
        val hundreds = absAmount % 1_000

        val parts = mutableListOf<String>()
        if (billions > 0) parts.add("${billions} tỷ")
        if (millions > 0) parts.add("${millions} triệu")
        if (thousands > 0) parts.add("${thousands} nghìn")
        if (hundreds > 0) parts.add("${hundreds} đồng")

        return parts.joinToString(" ")
    }


    private fun cleanTransactionContent(content: String): String {
        var cleaned = content
        val patterns = listOf(
            Regex("""Trace\s*\d+""", RegexOption.IGNORE_CASE),
            Regex("""Ma giao dich[:\s]+[A-Z0-9]+""", RegexOption.IGNORE_CASE),
            Regex("""MGD[:\s]+[A-Z0-9]+""", RegexOption.IGNORE_CASE),
            Regex("""FT\d+[A-Z0-9]+""", RegexOption.IGNORE_CASE),
            Regex("""\b[A-Z]{2,}\d{6,}\b"""),
            Regex("""\d{6,}""")
        )
        patterns.forEach { pattern -> cleaned = pattern.replace(cleaned, "") }
        return cleaned.trim().replace(Regex("""\s+"""), " ")
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

    private fun parseAmount(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        return try {
            raw.replace(".", "")   // bỏ dấu ngăn nghìn
                .replace(",", ".") // nếu có dấu thập phân
                .filter { it.isDigit() || it == '.' } // loại bỏ ký tự thừa
                .toDouble()
        } catch (e: Exception) {
            Log.e("BankService", "parseAmount error: ${e.message}")
            0.0
        }
    }
}