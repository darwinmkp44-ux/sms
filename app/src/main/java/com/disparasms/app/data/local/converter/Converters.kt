package com.disparasms.app.data.local.converter

import androidx.room.TypeConverter
import com.disparasms.app.data.local.entity.CampaignLogStatus
import com.disparasms.app.data.local.entity.CampaignStatus
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromJsonArray(value: String): List<Long> {
        val list = mutableListOf<Long>()
        val jsonArray = JSONArray(value)
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getLong(i))
        }
        return list
    }

    @TypeConverter
    fun toJsonArray(value: List<Long>): String {
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun fromCampaignStatus(value: String): Int = when (value) {
        CampaignStatus.PENDING -> 0
        CampaignStatus.SENDING -> 1
        CampaignStatus.PAUSED -> 2
        CampaignStatus.COMPLETED -> 3
        CampaignStatus.FAILED -> 4
        else -> 0
    }

    @TypeConverter
    fun toCampaignStatus(value: Int): String = when (value) {
        0 -> CampaignStatus.PENDING
        1 -> CampaignStatus.SENDING
        2 -> CampaignStatus.PAUSED
        3 -> CampaignStatus.COMPLETED
        4 -> CampaignStatus.FAILED
        else -> CampaignStatus.PENDING
    }

    @TypeConverter
    fun fromCampaignLogStatus(value: String): Int = when (value) {
        CampaignLogStatus.PENDING -> 0
        CampaignLogStatus.SENT -> 1
        CampaignLogStatus.DELIVERED -> 2
        CampaignLogStatus.FAILED -> 3
        else -> 0
    }

    @TypeConverter
    fun toCampaignLogStatus(value: Int): String = when (value) {
        0 -> CampaignLogStatus.PENDING
        1 -> CampaignLogStatus.SENT
        2 -> CampaignLogStatus.DELIVERED
        3 -> CampaignLogStatus.FAILED
        else -> CampaignLogStatus.PENDING
    }
}
