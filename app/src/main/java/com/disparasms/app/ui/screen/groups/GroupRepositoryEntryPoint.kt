package com.disparasms.app.ui.screen.groups

import com.disparasms.app.data.repository.GroupRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GroupRepositoryEntryPoint {
    fun groupRepository(): GroupRepository
}
