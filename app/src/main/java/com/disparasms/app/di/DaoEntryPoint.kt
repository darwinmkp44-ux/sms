package com.disparasms.app.di

import com.disparasms.app.data.local.dao.ContactDao
import com.disparasms.app.data.repository.ContactRepository
import com.disparasms.app.data.repository.ImportRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DaoEntryPoint {
    fun contactRepository(): ContactRepository
    fun importRepository(): ImportRepository
}
