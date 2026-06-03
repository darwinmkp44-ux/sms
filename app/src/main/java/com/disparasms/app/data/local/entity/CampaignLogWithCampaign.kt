package com.disparasms.app.data.local.entity

data class CampaignLogWithCampaign(
    val logId: Long,
    val campaignId: Long,
    val campaignName: String,
    val phone: String,
    val firstName: String?,
    val message: String,
    val status: String,
    val errorMessage: String?,
    val sentAt: Long?,
    val deliveredAt: Long?
)
