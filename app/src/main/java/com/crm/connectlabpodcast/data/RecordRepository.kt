package com.crm.connectlabpodcast.data

import javax.inject.Inject

class RecordRepository (private val recordDao: RecordDao) {

    suspend fun insertRecord(record: Record) = recordDao.insertRecord(record)

    fun fetchRecords() = recordDao.getAllRecords()

    suspend fun getRecordByTitle(title: String) = recordDao.getRecord(title)

}