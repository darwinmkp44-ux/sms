package com.disparasms.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
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
    val errors: List<String> = emptyList(),
    val contacts: List<ContactEntity> = emptyList()
)

data class ImportPreview(
    val columns: List<String>,
    val rows: List<List<String>>,
    val totalRows: Int
)

@Singleton
class ImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository
) {
    companion object {
        private val PHONE_ALIASES = listOf(
            "phone", "telefone", "celular", "movel", "mobile",
            "contacto", "numero", "número",
            "cell", "whatsapp"
        )
        private val NAME_ALIASES = listOf(
            "name", "nome", "fullname", "nomecompleto",
            "full_name", "nomes", "names",
            "nome_completo"
        )
        private val FIRST_NAME_ALIASES = listOf(
            "firstname", "first_name", "primeironome",
            "primeiro_nome", "pnome",
            "givenname", "nome_proprio"
        )
        private val LAST_NAME_ALIASES = listOf(
            "lastname", "last_name", "sobrenome",
            "ultimonome", "ultimo_nome",
            "unome", "surname",
            "apelido"
        )
    }

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

    suspend fun importFromPhoneContacts(
        groupId: Long? = null,
        onProgress: suspend (imported: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )

            val contacts = mutableListOf<ContactEntity>()
            val seenPhones = mutableSetOf<String>()

            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (c.moveToNext()) {
                    val rawPhone = if (phoneIdx >= 0) c.getString(phoneIdx) ?: "" else ""
                    val phone = PhoneUtils.clean(rawPhone)

                    if (phone.isEmpty() || !PhoneUtils.isValidMzPhone(phone)) continue
                    if (phone in seenPhones) continue
                    seenPhones.add(phone)

                    val displayName = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                    val nameParts = displayName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                    val firstName = nameParts.firstOrNull()
                    val lastName = if (nameParts.size > 1) nameParts.last() else null
                    val fullName = displayName.ifBlank { phone }

                    contacts.add(ContactEntity(
                        groupId = groupId,
                        phone = phone,
                        firstName = firstName,
                        lastName = lastName,
                        fullName = fullName,
                        importedFrom = "phone"
                    ))
                }
            }

            if (contacts.isEmpty()) {
                return ImportResult(errors = listOf("Nenhum contacto com número Moçambicano encontrado no telefone."))
            }

            val total = contacts.size
            val existingPhones = contactRepository.getPhonesWithoutGroup()

            val batchSize = 500
            var totalImported = 0
            var totalSkipped = 0

            contacts.chunked(batchSize).forEach { batch ->
                val (imported, skipped) = contactRepository.importContactsWithExisting(batch, existingPhones)
                totalImported += imported
                totalSkipped += skipped
                onProgress(totalImported, total)
            }

            if (groupId != null) groupRepository.refreshContactCount(groupId)

            ImportResult(
                totalFound = total,
                imported = totalImported,
                skipped = totalSkipped,
                contacts = contacts
            )
        } catch (e: SecurityException) {
            ImportResult(errors = listOf("Permissão de leitura de contactos não concedida."))
        } catch (e: Exception) {
            ImportResult(errors = listOf("Erro ao ler contactos: ${e.message}"))
        }
    }

    suspend fun importFromUri(
        uri: Uri,
        groupId: Long?,
        columnMapping: Map<String, String> = emptyMap(),
        hasHeader: Boolean = true,
        onProgress: suspend (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val mimeType = context.contentResolver.getType(uri)
                val fileName = uri.lastPathSegment ?: uri.toString()
                val result = when {
                    mimeType == "text/csv" || mimeType == "text/comma-separated-values" ||
                        fileName.endsWith(".csv", true) ->
                        importCsvStream(stream, groupId, columnMapping, hasHeader, onProgress)
                    mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                        mimeType == "application/vnd.ms-excel" ||
                        fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true) ->
                        importExcelStreaming(stream, groupId, columnMapping, hasHeader)
                    fileName.endsWith(".txt", true) -> importTxtStream(stream, groupId, onProgress)
                    else -> ImportResult(errors = listOf("Formato de ficheiro não suportado"))
                }
                if (result.errors.isEmpty() && result.contacts.isNotEmpty()) {
                    val (imported, skipped) = contactRepository.importContacts(result.contacts)
                    if (groupId != null) groupRepository.refreshContactCount(groupId)
                    result.copy(imported = imported, skipped = result.skipped + skipped)
                } else {
                    result
                }
            } ?: ImportResult(errors = listOf("Não foi possível abrir o ficheiro"))
        } catch (e: Exception) {
            ImportResult(errors = listOf("Erro: ${e.message}"))
        }
    }

    private fun importExcelStreaming(
        stream: InputStream,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean
    ): ImportResult {
        val contacts = mutableListOf<ContactEntity>()
        val seenPhones = mutableSetOf<String>()
        var invalidPhones = 0
        var phoneCol = -1
        var firstNameCol = -1
        var lastNameCol = -1
        var nameCol = -1
        var detected = false

        StreamingExcelReader(stream) { row, rowNum ->
            if (!detected && hasHeader) {
                val mapping = if (columnMapping.isEmpty()) autoDetectColumns(row) else columnMapping
                phoneCol = findColumnIndex(row, mapping, "phone")
                firstNameCol = findColumnIndex(row, mapping, "first_name")
                lastNameCol = findColumnIndex(row, mapping, "last_name")
                nameCol = findColumnIndex(row, mapping, "name")
                detected = true
                return@StreamingExcelReader
            }
            if (hasHeader && !detected) return@StreamingExcelReader
            if (!hasHeader && !detected) {
                phoneCol = 0
                detected = true
            }
            if (phoneCol == -1) return@StreamingExcelReader
            if (phoneCol >= row.size) return@StreamingExcelReader

            val rawPhone = row[phoneCol].trim()
            val phone = PhoneUtils.clean(rawPhone)

            if (phone.isEmpty() || !PhoneUtils.isValidMzPhone(phone)) {
                invalidPhones++
                return@StreamingExcelReader
            }
            if (phone in seenPhones) return@StreamingExcelReader
            seenPhones.add(phone)

            val firstName = when {
                firstNameCol != -1 && firstNameCol < row.size -> row[firstNameCol].trim()
                nameCol != -1 && nameCol < row.size -> row[nameCol].trim().split(" ").firstOrNull()
                else -> null
            }
            val lastName = when {
                lastNameCol != -1 && lastNameCol < row.size -> row[lastNameCol].trim()
                nameCol != -1 && nameCol < row.size -> {
                    val parts = row[nameCol].trim().split(" ")
                    if (parts.size > 1) parts.last() else null
                }
                else -> null
            }
            val fullName = when {
                firstName != null && lastName != null -> "$firstName $lastName"
                firstName != null -> firstName
                else -> phone
            }

            contacts.add(ContactEntity(
                groupId = groupId,
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                importedFrom = "excel"
            ))
        }.read()

        return ImportResult(
            totalFound = contacts.size + invalidPhones,
            imported = 0,
            skipped = seenPhones.size,
            invalidPhones = invalidPhones,
            contacts = contacts
        )
    }

    private suspend fun importCsvStream(
        stream: InputStream,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean,
        onProgress: suspend (processed: Int, total: Int) -> Unit
    ): ImportResult {
        val allRows = csvReader().readAll(stream)
        val header = if (hasHeader && allRows.isNotEmpty()) allRows.first() else emptyList()
        val data = if (hasHeader && allRows.size > 1) allRows.drop(1) else allRows
        val resolvedMapping = if (columnMapping.isEmpty()) autoDetectColumns(header) else columnMapping
        return processRowsFast(data, header, resolvedMapping, groupId, data.size, onProgress)
    }

    private fun importExcel(
        stream: InputStream,
        groupId: Long?,
        columnMapping: Map<String, String>,
        hasHeader: Boolean,
        onProgress: suspend (processed: Int, total: Int) -> Unit
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

    private suspend fun importTxtStream(
        stream: InputStream,
        groupId: Long?,
        onProgress: suspend (processed: Int, total: Int) -> Unit
    ): ImportResult {
        val text = stream.bufferedReader().readText()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var invalidPhones = 0
        val seenPhones = mutableSetOf<String>()

        val contacts = lines.mapNotNull { line ->
            val phone = PhoneUtils.clean(line)
            if (phone.isEmpty() || !PhoneUtils.isValidMzPhone(phone)) {
                invalidPhones++
                return@mapNotNull null
            }
            if (phone in seenPhones) return@mapNotNull null
            seenPhones.add(phone)
            ContactEntity(
                groupId = groupId,
                phone = phone,
                fullName = phone,
                importedFrom = "txt"
            )
        }

        return ImportResult(
            totalFound = lines.size,
            imported = 0,
            skipped = lines.size - contacts.size - invalidPhones,
            invalidPhones = invalidPhones,
            contacts = contacts
        )
    }

    private fun processRowsWithProgress(
        rows: List<List<String>>,
        header: List<String>,
        mapping: Map<String, String>,
        groupId: Long?,
        totalEstimate: Int,
        onProgress: suspend (processed: Int, total: Int) -> Unit
    ): ImportResult {
        val resolvedMapping = if (mapping.isEmpty()) autoDetectColumns(header) else mapping

        val phoneColumnIdx = findColumnIndex(header, resolvedMapping, "phone")
        val firstNameColumnIdx = findColumnIndex(header, resolvedMapping, "first_name")
        val lastNameColumnIdx = findColumnIndex(header, resolvedMapping, "last_name")
        val nameColumnIdx = findColumnIndex(header, resolvedMapping, "name")

        if (phoneColumnIdx == -1) {
            return ImportResult(errors = listOf(
                "Coluna de telefone não encontrada. " +
                "Verifique se o ficheiro tem uma coluna com nome 'Telefone', 'Celular', 'Phone' etc."
            ))
        }

        var invalidPhones = 0
        val seenPhones = mutableSetOf<String>()
        val contacts = mutableListOf<ContactEntity>()

        rows.forEachIndexed { index, row ->
            if (phoneColumnIdx >= row.size) return@forEachIndexed

            val rawPhone = row[phoneColumnIdx].trim()
            val phone = PhoneUtils.clean(rawPhone)

            if (phone.isEmpty() || !PhoneUtils.isValidMzPhone(phone)) {
                invalidPhones++
                return@forEachIndexed
            }

            if (phone in seenPhones) return@forEachIndexed
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

            contacts.add(ContactEntity(
                groupId = groupId,
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                importedFrom = "excel"
            ))
        }

        return ImportResult(
            totalFound = rows.size,
            imported = 0,
            skipped = rows.size - contacts.size - invalidPhones,
            invalidPhones = invalidPhones,
            contacts = contacts
        )
    }

    private suspend fun processRowsFast(
        rows: List<List<String>>,
        header: List<String>,
        mapping: Map<String, String>,
        groupId: Long?,
        totalEstimate: Int,
        onProgress: suspend (processed: Int, total: Int) -> Unit
    ): ImportResult {
        val phoneColumnIdx = findColumnIndex(header, mapping, "phone")
        val firstNameColumnIdx = findColumnIndex(header, mapping, "first_name")
        val lastNameColumnIdx = findColumnIndex(header, mapping, "last_name")
        val nameColumnIdx = findColumnIndex(header, mapping, "name")

        if (phoneColumnIdx == -1) {
            return ImportResult(errors = listOf(
                "Coluna de telefone não encontrada. " +
                "Verifique se o ficheiro tem uma coluna com nome 'Telefone', 'Celular', 'Phone' etc."
            ))
        }

        var invalidPhones = 0
        val seenPhones = mutableSetOf<String>()
        val contacts = ArrayList<ContactEntity>(totalEstimate)

        for (row in rows) {
            if (phoneColumnIdx >= row.size) continue

            val rawPhone = row[phoneColumnIdx].trim()
            val phone = PhoneUtils.clean(rawPhone)

            if (phone.isEmpty() || !PhoneUtils.isValidMzPhone(phone)) {
                invalidPhones++
                continue
            }
            if (phone in seenPhones) continue
            seenPhones.add(phone)

            val firstName = when {
                firstNameColumnIdx != -1 && firstNameColumnIdx < row.size -> row[firstNameColumnIdx].trim()
                nameColumnIdx != -1 && nameColumnIdx < row.size -> row[nameColumnIdx].trim().split(" ").firstOrNull()
                else -> null
            }
            val lastName = when {
                lastNameColumnIdx != -1 && lastNameColumnIdx < row.size -> row[lastNameColumnIdx].trim()
                nameColumnIdx != -1 && nameColumnIdx < row.size -> {
                    val parts = row[nameColumnIdx].trim().split(" ")
                    if (parts.size > 1) parts.last() else null
                }
                else -> null
            }
            val fullName = when {
                firstName != null && lastName != null -> "$firstName $lastName"
                firstName != null -> firstName
                else -> phone
            }

            contacts.add(ContactEntity(
                groupId = groupId,
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                importedFrom = "excel"
            ))
        }

        return ImportResult(
            totalFound = rows.size,
            imported = 0,
            skipped = rows.size - contacts.size - invalidPhones,
            invalidPhones = invalidPhones,
            contacts = contacts
        )
    }

    private fun processRows(
        rows: List<List<String>>,
        header: List<String>,
        mapping: Map<String, String>,
        groupId: Long?
    ): ImportResult {
        val resolvedMapping = if (mapping.isEmpty()) autoDetectColumns(header) else mapping

        val phoneColumnIdx = findColumnIndex(header, resolvedMapping, "phone")
        val firstNameColumnIdx = findColumnIndex(header, resolvedMapping, "first_name")
        val lastNameColumnIdx = findColumnIndex(header, resolvedMapping, "last_name")
        val nameColumnIdx = findColumnIndex(header, resolvedMapping, "name")

        if (phoneColumnIdx == -1) {
            return ImportResult(errors = listOf(
                "Coluna de telefone não encontrada. " +
                "Verifique se o ficheiro tem uma coluna com nome 'Telefone', 'Celular', 'Phone' etc."
            ))
        }

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

            if (phone in seenPhones) return@mapNotNull null
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
            imported = 0,
            skipped = rows.size - contacts.size - invalidPhones,
            invalidPhones = invalidPhones,
            contacts = contacts
        )
    }

    private fun autoDetectColumns(header: List<String>): Map<String, String> {
        val mapping = mutableMapOf<String, String>()

        for (col in header) {
            val colLower = col.trim().lowercase().replace(" ", "_").replace("-", "_")

            when {
                PHONE_ALIASES.any { it == colLower || colLower.contains(it) } -> {
                    mapping["phone"] = col
                }
                LAST_NAME_ALIASES.any { it == colLower || colLower.contains(it) } -> {
                    mapping["last_name"] = col
                }
                FIRST_NAME_ALIASES.any { it == colLower || colLower.contains(it) } -> {
                    if ("name" !in mapping) {
                        mapping["name"] = col
                    } else {
                        mapping["first_name"] = col
                    }
                }
                NAME_ALIASES.any { it == colLower || colLower.contains(it) } -> {
                    if ("name" !in mapping && "first_name" !in mapping) {
                        mapping["name"] = col
                    }
                }
            }
        }

        return mapping
    }

    private fun findColumnIndex(header: List<String>, mapping: Map<String, String>, field: String): Int {
        val columnName = mapping[field] ?: return -1
        return header.indexOfFirst { it.equals(columnName, ignoreCase = true) }
    }
}
