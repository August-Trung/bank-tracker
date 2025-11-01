//package com.banktracker.vn.service
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationManagerCompat
//
///**
// * BroadcastReceiver để restart service khi bị kill
// * và khi device reboot
// */
//class ServiceRestartBroadcastReceiver : BroadcastReceiver() {
//
//    override fun onReceive(context: Context, intent: Intent) {
//        Log.d("BankTracker", "ServiceRestartBroadcastReceiver received: ${intent.action}")
//
//        when (intent.action) {
//            Intent.ACTION_BOOT_COMPLETED,
//            Intent.ACTION_MY_PACKAGE_REPLACED,
//            BankNotificationListenerService.ACTION_RESTART_SERVICE -> {
//                // Kiểm tra xem service đã có quyền Notification Access chưa
//                val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
//                    .contains(context.packageName)
//
//                if (hasPermission) {
//                    Log.d("BankTracker", "Restarting BankNotificationListenerService...")
//                    restartService(context)
//                } else {
//                    Log.w("BankTracker", "No notification permission, skip restart")
//                }
//            }
//        }
//    }
//
//    private fun restartService(context: Context) {
//        try {
//            val serviceIntent = Intent(context, BankNotificationListenerService::class.java)
//            serviceIntent.action = BankNotificationListenerService.ACTION_RESTART_SERVICE
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(serviceIntent)
//            } else {
//                context.startService(serviceIntent)
//            }
//
//            Log.d("BankTracker", "Service restart command sent")
//        } catch (e: Exception) {
//            Log.e("BankTracker", "Failed to restart service", e)
//        }
//    }
//}