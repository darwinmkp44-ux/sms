# Session Context

## Project
SMS Disparo em Massa — Android app for bulk SMS campaigns.

## Build Command
```
export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.10-ms && export ANDROID_HOME=$HOME/Android && ./gradlew assembleDebug
```

## Lint/Check
Build succeeds with `assembleDebug`; no separate lint or typecheck commands.

## Recent Fixes

### 1. Pause/Resume Race Condition (SmsQueueManager.kt)
- Added `pauseUpdateJob` (a `Job?`) that stores the coroutine launched by `pauseSending()` to update DB status to PAUSED.
- `stopSending()` now cancels `pauseUpdateJob`, preventing the DB-update coroutine from racing with resume's own `updateStatus(SENDING)`.

### 2. Count Resets on Resume (SmsQueueManager.kt:startSending)
- `startSending()` now reads existing `sentCount`, `deliveredCount`, `failedCount` from `campaignRepository.getById(campaignId)` (via `runBlocking(Dispatchers.IO)` at the start of the method).
- Counter variables `sent`, `delivered`, `failed` initialise from these DB values instead of zero.
- `_progress.value` includes these initial values so UI shows accumulated counts.

### 3. Excel Import Speed (ImportRepository.kt)
- **Replaced** `importExcelStreaming()` with `importExcelAsCsv()`:
  - Parses .xlsx via `StreamingExcelReader` (SAX-based, low memory).
  - Writes all rows as CSV to a `ByteArrayOutputStream`.
  - Creates a `ByteArrayInputStream` from the CSV bytes.
  - Calls `importCsvStream()` — the existing fast CSV import path (`processRowsFast`).
  - Avoids the slower `processRowsWithProgress` / inline contact-building in the old streaming Excel path.
- Added `escapeCell()` helper for proper CSV quoting.

### 4. DB Insert Batching (ContactRepository.kt)
- `importContactsWithExisting()` now chunks the filtered contacts list into groups of 500 before calling `contactDao.insertAll()`, calling `insertAll` multiple times instead of once.
- Avoids potential SQLite variable limit issues and improves transaction performance.

### 5. Phone Import ORDER BY Removed
- `importFromPhoneContacts()` no longer sorts the ContentResolver query, eliminating slow device-side sort on large contact lists.

### 6. Pre-fetched Existing Phones
- `getPhonesWithoutGroup()` called once at the start of `importFromPhoneContacts()` and passed to `importContactsWithExisting()` for every batch, avoiding repeated DB queries.

### 7. Batch Size Increased
- Contact batch size in `importFromPhoneContacts()` raised from 100 to 500.

## Key Files
- `app/src/main/java/com/disparasms/app/sms/SmsQueueManager.kt` — pause/resume race & counter fixes
- `app/src/main/java/com/disparasms/app/data/repository/ImportRepository.kt` — Excel→CSV, phone import efficiency
- `app/src/main/java/com/disparasms/app/data/repository/ContactRepository.kt` — chunked DB insert
- `app/src/main/java/com/disparasms/app/data/repository/StreamingExcelReader.kt` — SAX-based streaming Excel reader
- `app/src/main/java/com/disparasms/app/data/repository/ContactDao.kt` — `getPhonesWithoutGroup()`, `insertAll()`
