package com.disparasms.app.util

object PhoneUtils {

    private val MZ_PREFIXES = listOf("82", "83", "84", "85", "86", "87")

    private val VALID_MZ_REGEX = Regex("^\\+?258?[8][2-7]\\d{6}$")
    private val CLEAN_REGEX = Regex("[^\\d+]")

    fun clean(phone: String): String {
        val cleaned = phone.replace(CLEAN_REGEX, "")
        return when {
            cleaned.startsWith("+258") -> cleaned
            cleaned.startsWith("258") -> "+$cleaned"
            cleaned.startsWith("0") && cleaned.length == 9 -> "+258${cleaned.removePrefix("0")}"
            cleaned.length == 9 -> "+258$cleaned"
            cleaned.length == 12 && cleaned.startsWith("258") -> "+$cleaned"
            else -> cleaned
        }
    }

    fun isValidMzPhone(phone: String): Boolean {
        val cleaned = clean(phone)
        return VALID_MZ_REGEX.matches(cleaned)
    }

    fun formatForDisplay(phone: String): String {
        val cleaned = clean(phone)
        return when {
            cleaned.startsWith("+258") -> "84 ${cleaned.substring(4, 7)} ${cleaned.substring(7)}"
            else -> phone
        }
    }

    fun getCarrier(phone: String): String? {
        val cleaned = clean(phone)
        val prefix = cleaned.substringAfter("+258").take(2)
        return when (prefix) {
            "82", "83" -> "TMcel"
            "84", "85" -> "Vodacom"
            "86", "87" -> "Movitel"
            else -> null
        }
    }
}
