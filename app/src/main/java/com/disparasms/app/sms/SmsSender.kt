package com.disparasms.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import java.lang.reflect.Method

data class SmsResult(
    val success: Boolean,
    val messageId: Long? = null,
    val error: String? = null
)

class SmsSender(private val context: Context) {

    private val smsManager: SmsManager by lazy { SmsManager.getDefault() }

    fun getAvailableSimSlots(): List<SimInfo> {
        val sims = mutableListOf<SimInfo>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                    subscriptionInfoList?.forEachIndexed { index, info ->
                        sims.add(SimInfo(
                            slotIndex = index,
                            subscriptionId = info.subscriptionId,
                            carrierName = info.carrierName?.toString() ?: "SIM ${index + 1}",
                            displayName = info.displayName?.toString() ?: "SIM ${index + 1}"
                        ))
                    }
                }
            }
            if (sims.isEmpty()) {
                sims.add(SimInfo(0, 0, "Operadora A", "SIM 1"))
                sims.add(SimInfo(1, 0, "Operadora B", "SIM 2"))
            }
        } catch (e: Exception) {
            sims.add(SimInfo(0, 0, "Operadora A", "SIM 1"))
            sims.add(SimInfo(1, 0, "Operadora B", "SIM 2"))
        }
        return sims
    }

    fun sendSms(phone: String, message: String, simSlot: Int = 0): SmsResult {
        return try {
            val manager = if (simSlot > 0) {
                getSmsManagerForSlot(simSlot)
            } else {
                smsManager
            }

            val parts = manager.divideMessage(message)
            val sentIntents = ArrayList<android.app.PendingIntent>()

            val sentIntent = android.app.PendingIntent.getBroadcast(
                context,
                phone.hashCode(),
                android.content.Intent("SMS_SENT_ACTION").apply {
                    putExtra("phone", phone)
                },
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            sentIntents.add(sentIntent)

            if (parts.size > 1) {
                manager.sendMultipartTextMessage(phone, null, parts, sentIntents, null)
            } else {
                manager.sendTextMessage(phone, null, message, sentIntent, null)
            }

            SmsResult(success = true, messageId = System.currentTimeMillis())
        } catch (e: Exception) {
            SmsResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    private fun getSmsManagerForSlot(slotIndex: Int): SmsManager {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                    val targetSub = subscriptionInfoList?.getOrNull(slotIndex)
                    if (targetSub != null) {
                        return SmsManager.getSmsManagerForSubscriptionId(targetSub.subscriptionId)
                    }
                }
            }

            val getServiceMethod: Method = SmsManager::class.java.getMethod("getSmsManagerForSubscriptionId", Int::class.java)
            return getServiceMethod.invoke(null, slotIndex) as SmsManager
        } catch (e: Exception) {
            return smsManager
        }
    }

    fun getMessageParts(message: String): Int {
        return smsManager.divideMessage(message).size
    }

    fun getMessageCount(message: String): Int {
        val parts = smsManager.divideMessage(message)
        return parts.size
    }
}

data class SimInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String
)
