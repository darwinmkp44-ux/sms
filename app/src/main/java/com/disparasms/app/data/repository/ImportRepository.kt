package com.disparasms.app.data.repository

import android.content.Context
import android.net.Uri
import com.disparasms.app.data.local.entity.ContactEntity
import com.disparasms.app.util.PhoneUtils
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val imported: Int = 0,
    val skipped: Int = 0,
    val duplicates: Int = 0,
    val invalidPhones: Int = 0,
    val totalFound: Int = 0,
    val errors: List<String> = emptyList()
)

data class ImportPreview(
    val columns: List<String>,
    val rows: List<List<String>>,
    val totalRows: Int
)

@Singleton
class ImportRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun previewExcel(uri: Uri): ImportPreview? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                parsePreview(stream, uri.toString())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePreview(stream: InputStream, fileName: String): ImportPreview? {
        return when {
            fileName.endsWith(".csv", true) -> parseCsvPreview(stream)
            fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true) -> parseExcelPreview(stream)
            else -> null
        }
    }

    private fun parseCsvPreview(stream: InputStream): ImportPreview {
        val rows = csvReader().readAll(stream)
        val header = rows.firstOrNull() ?: return ImportPreview(emptyList(), emptyList(), 0)
        val data = rows.drop(1)
        return ImportPreview(
            columns = header,
            rows = data.take(50),
            totalRows = data.size
        )
    }

    private fun parseExcelPreview(stream: InputStream): ImportPreview {
        val workbook = WorkbookFactory.create(stream)
        val sheet = workbook.getSheetAt(0)
        val header = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()

        val rowIterator = sheet.iterator()
        if (rowIterator.hasNext()) {
            val headerRow = rowIterator.next()
            for (cell in headerRow) {
                header.add(cell.toString())
            }
        }

        var count = 0
        while (rowIterator.hasNext() && count < 50) {
            val row = rowIterator.next()
            val cells = mutableListOf<String>()
            for (cell in row) {
                cells.add(cell.toString())
            }
            rows.add(cells)
            count++
        }

        val totalRows = sheet.lastRowNum
        workbook.close()

        return ImportPreview(
            columns = header,
            rows = rows,
            totalRows = totalRows
        )
    }

    fun importFromUri(
        uri: Uri,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean = true
    ): ImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                parseAndImport(stream, uri.toString(), groupId, columnMapping, hasHeader)
            } ?: ImportResult(errors = listOf("Could not open file"))
        } catch (e: Exception) {
            ImportResult(errors = listOf("Error: ${e.message}"))
        }
    }

    private fun parseAndImport(
        stream: InputStream,
        fileName: String,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean
    ): ImportResult {
        return when {
            fileName.endsWith(".csv", true) -> importCsv(stream, groupId, columnMapping, hasHeader)
            fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true) -> importExcel(stream, groupId, columnMapping, hasHeader)
            else -> ImportResult(errors = listOf("Unsupported file format"))
        }
    }

    private fun importCsv(
        stream: InputStream,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean
    ): ImportResult {
        val rows = csvReader().readAll(stream)
        val data = if (hasHeader && rows.isNotEmpty()) rows.drop(1) else rows

        return processRows(data, rows.firstOrNull() ?: emptyList(), columnMapping, groupId)
    }

    private fun importExcel(
        stream: InputStream,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean
    ): ImportResult {
        val workbook = WorkbookFactory.create(stream)
        val sheet = workbook.getSheetAt(0)
        val rows = mutableListOf<List<String>>()
        val header = mutableListOf<String>()

        val rowIterator = sheet.iterator()
        if (hasHeader && rowIterator.hasNext()) {
            val headerRow = rowIterator.next()
            for (cell in headerRow) header.add(cell.toString())
        }

        for (row in rowIterator) {
            val cells = mutableListOf<String>()
            for (cell in row) cells.add(cell.toString())
            rows.add(cells)
        }

        workbook.close()
        return processRows(rows, header, columnMapping, groupId)
    }

    private fun processRows(
        rows: List<List<String>>,
        header: List<String>,
        mapping: Map<String, String>,
        groupId: Long?
    ): ImportResult {
        val phoneColumnIdx = findColumnIndex(header, mapping, "phone")
        val firstNameColumnIdx = findColumnIndex(header, mapping, "first_name")
        val lastNameColumnIdx = findColumnIndex(header, mapping, "last_name")
        val nameColumnIdx = findColumnIndex(header, mapping, "name")

        if (phoneColumnIdx == -1) {
            return ImportResult(errors = listOf("Phone column not found in mapping"))
        }

        var imported = 0
        var skipped = 0
        var invalidPhones = 0
        val seenPhones = mutableSetOf<String>()

        val contacts = rows.mapNotNull { row ->
            if (phoneColumnIdx >= row.size) return@mapNotNull null

            val rawPhone = row[phoneColumnIdx].trim()
            val phone = PhoneUtils.clean(rawPhone)

            if (phone.isEmpty() || !PhoneUtils.isValidMzPhone(phone)) {
                invalidPhones++
                return@mapNotNull null
            }

            if (phone in seenPhones) {
                skipped++
                return@mapNotNull null
            }
            seenPhones.add(phone)

            val firstName = if (firstNameColumnIdx != -1 && firstNameColumnIdx < row.size) {
                row[firstNameColumnIdx].trim()
            } else if (nameColumnIdx != -1 && nameColumnIdx < row.size) {
                row[nameColumnIdx].trim().split(" ").firstOrNull()
            } else null

            val lastName = if (lastNameColumnIdx != -1 && lastNameColumnIdx < row.size) {
                row[lastNameColumnIdx].trim()
            } else if (nameColumnIdx != -1 && nameColumnIdx < row.size) {
                row[nameColumnIdx].trim().split(" ").drop(1).takeLast(1).firstOrNull()
            } else null

            val fullName = when {
                firstName != null && lastName != null -> "$firstName $lastName"
                firstName != null -> firstName
                else -> phone
            }

            ContactEntity(
                groupId = groupId,
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                importedFrom = "excel"
            )
        }

        return ImportResult(
            totalFound = rows.size,
            imported = contacts.size,
            skipped = skipped,
            invalidPhones = invalidPhones
        )
    }

    private fun findColumnIndex(header: List<String>, mapping: Map<String, String>, field: String): Int {
        val columnName = mapping[field] ?: return -1
        return header.indexOfFirst { it.equals(columnName, ignoreCase = true) }
    }
}
