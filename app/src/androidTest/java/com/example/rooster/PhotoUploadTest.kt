package com.example.rooster

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rooster.ui.components.PhotoUploadComponentsDemoScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PhotoUploadTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var photoUploadService: PhotoUploadService
    private lateinit var networkQualityManager: NetworkQualityManager
    private lateinit var testDatabase: PhotoUploadDatabase
    private lateinit var testDao: PhotoUploadDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create in-memory database for testing
        testDatabase = Room.inMemoryDatabaseBuilder(
            context,
            PhotoUploadDatabase::class.java
        ).allowMainThreadQueries().build()
        
        testDao = testDatabase.photoUploadDao()
        networkQualityManager = NetworkQualityManager(context)
        photoUploadService = PhotoUploadService(context, networkQualityManager)
    }

    @After
    fun cleanup() {
        photoUploadService.shutdown()
        testDatabase.close()
    }

    // Core UI Tests
    
    @Test
    fun testPhotoPickerDialogAppears() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService, networkQualityManager)
        }

        // Click "Add Photo" button to trigger picker dialog
        composeTestRule
            .onNodeWithContentDescription("Add Photo")
            .performClick()

        // Verify photo picker dialog appears
        composeTestRule
            .onNodeWithText("Choose Photo Source")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Gallery")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()
    }

    @Test
    fun testUploadProgressDisplay() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService, networkQualityManager)
        }

        val mockUri = Uri.parse("content://media/external/images/media/test")

        runBlocking {
            // Create a properly structured upload request
            val uploadRequest = SerializablePhotoUploadRequest(
                uri = mockUri,
                fileName = "test_photo.jpg",
                targetParseObjectId = "TEST_FOWL_ID",
                targetClassName = "Fowl",
                targetField = "primaryImage"
            )
            photoUploadService.enqueueUpload(uploadRequest)
        }

        composeTestRule.waitForIdle()

        // Verify upload queue summary appears
        composeTestRule
            .onNodeWithText("Upload Queue")
            .assertIsDisplayed()
    }

    @Test
    fun testQueueManagementButtons() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService, networkQualityManager)
        }

        // Test Process Queue button
        composeTestRule
            .onNodeWithText("Process Queue")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Test Clear Completed button
        composeTestRule
            .onNodeWithText("Clear Completed")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
    }

    // Room DAO Tests - Core functionality

    @Test
    fun testRoomDaoBasicOperations() = runTest {
        val testEntity = PhotoUploadEntity(
            id = "test_123",
            uriString = "content://test/image.jpg",
            fileName = "test_image.jpg",
            targetObjectId = "fowl_123",
            targetClassName = "Fowl",
            targetField = "primaryImage",
            statusName = UploadStatus.PENDING.name
        )

        // Test insert
        testDao.insert(testEntity)
        
        // Test retrieve by ID
        val retrieved = testDao.getById("test_123")
        assertNotNull(retrieved)
        assertEquals("test_image.jpg", retrieved!!.fileName)
        assertEquals(UploadStatus.PENDING.name, retrieved.statusName)

        // Test update status
        testDao.updateStatus("test_123", UploadStatus.UPLOADING.name)
        val updated = testDao.getById("test_123")
        assertEquals(UploadStatus.UPLOADING.name, updated!!.statusName)

        // Test delete
        testDao.delete(testEntity)
        val deleted = testDao.getById("test_123")
        assertNull(deleted)
    }

    @Test
    fun testRoomDaoRetryLogic() = runTest {
        val testEntity = PhotoUploadEntity(
            id = "retry_test_123",
            uriString = "content://test/retry.jpg",
            fileName = "retry_test.jpg",
            targetObjectId = "fowl_retry",
            targetClassName = "Fowl",
            targetField = "primaryImage",
            statusName = UploadStatus.FAILED.name,
            retryCount = 1
        )

        testDao.insert(testEntity)

        // Test increment retry count
        testDao.incrementRetryCount("retry_test_123")
        val updated = testDao.getById("retry_test_123")
        assertEquals(2, updated!!.retryCount)
        assertEquals(UploadStatus.RETRYING.name, updated.statusName)

        // Test retriable requests
        val retriableRequests = testDao.getRetriableRequests(maxRetries = 3)
        assertTrue(retriableRequests.any { it.id == "retry_test_123" })
    }

    @Test
    fun testRoomDaoBatchOperations() = runTest {
        // Create multiple test entities
        val entities = (1..5).map { i ->
            PhotoUploadEntity(
                id = "batch_test_$i",
                uriString = "content://test/batch_$i.jpg",
                fileName = "batch_test_$i.jpg",
                targetObjectId = "fowl_$i",
                targetClassName = "Fowl",
                targetField = "primaryImage",
                statusName = UploadStatus.PENDING.name
            )
        }

        // Insert all entities
        entities.forEach { testDao.insert(it) }

        // Test getting pending batch
        val pendingBatch = testDao.getPendingBatch(batchSize = 3)
        assertEquals(3, pendingBatch.size)

        // Test fresh pending requests
        val freshPending = testDao.getFreshPendingRequests(limit = 2)
        assertEquals(2, freshPending.size)
        assertTrue(freshPending.all { it.retryCount == 0 })
    }

    @Test
    fun testRoomDaoStatusCounts() = runTest {
        // Insert entities with different statuses
        val statuses = listOf(
            UploadStatus.PENDING,
            UploadStatus.UPLOADING,
            UploadStatus.COMPLETED,
            UploadStatus.FAILED,
            UploadStatus.PENDING
        )

        statuses.forEachIndexed { index, status ->
            val entity = PhotoUploadEntity(
                id = "status_test_$index",
                uriString = "content://test/status_$index.jpg",
                fileName = "status_test_$index.jpg",
                statusName = status.name
            )
            testDao.insert(entity)
        }

        // Test status counts
        val pendingCount = testDao.getCountByStatus(UploadStatus.PENDING.name)
        assertEquals(2, pendingCount)

        val uploadingCount = testDao.getCountByStatus(UploadStatus.UPLOADING.name)
        assertEquals(1, uploadingCount)

        val activeCount = testDao.getActiveUploadCount()
        assertEquals(1, activeCount)
    }

    @Test
    fun testRoomDaoProgressUpdates() = runTest {
        val testEntity = PhotoUploadEntity(
            id = "progress_test",
            uriString = "content://test/progress.jpg",
            fileName = "progress_test.jpg",
            statusName = UploadStatus.UPLOADING.name,
            progress = 0
        )

        testDao.insert(testEntity)

        // Test progress update
        testDao.updateProgress("progress_test", 50)
        val updated = testDao.getById("progress_test")
        assertEquals(50, updated!!.progress)

        // Test parse file URL update
        testDao.updateWithParseFileUrl("progress_test", "https://parse.com/file.jpg")
        val completed = testDao.getById("progress_test")
        assertEquals("https://parse.com/file.jpg", completed!!.parseFileUrl)
        assertEquals(UploadStatus.COMPLETED.name, completed.statusName)
    }

    @Test
    fun testRoomDaoClearOperations() = runTest {
        // Insert entities with different statuses for cleanup testing
        val entities = listOf(
            PhotoUploadEntity(
                id = "clear_1", 
                uriString = "content://test/clear_1.jpg",
                fileName = "clear_1.jpg", 
                statusName = UploadStatus.COMPLETED.name
            ),
            PhotoUploadEntity(
                id = "clear_2", 
                uriString = "content://test/clear_2.jpg",
                fileName = "clear_2.jpg", 
                statusName = UploadStatus.FAILED.name
            ),
            PhotoUploadEntity(
                id = "clear_3", 
                uriString = "content://test/clear_3.jpg",
                fileName = "clear_3.jpg", 
                statusName = UploadStatus.CANCELLED.name
            ),
            PhotoUploadEntity(
                id = "clear_4", 
                uriString = "content://test/clear_4.jpg",
                fileName = "clear_4.jpg", 
                statusName = UploadStatus.PENDING.name
            )
        )

        entities.forEach { testDao.insert(it) }

        // Test clear completed
        testDao.clearCompleted()
        
        // Only pending should remain
        val remaining = testDao.getPendingRequests()
        assertEquals(1, remaining.size)
        assertEquals("clear_4", remaining.first().id)
    }

    // Basic Network Quality Test

    @Test
    fun testNetworkQualityDetection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkManager = NetworkQualityManager(context)
        
        // Test that network quality can be detected
        val quality = networkManager.getCurrentNetworkQuality()
        assertNotNull(quality)
        
        // Quality should be one of the enum values
        assertTrue(quality in NetworkQualityLevel.values())
    }

    // Error Handling

    @Test
    fun testInvalidUriHandling() = runTest {
        val invalidEntity = PhotoUploadEntity(
            id = "invalid_test",
            uriString = "invalid://uri",
            fileName = "invalid.jpg",
            statusName = UploadStatus.PENDING.name
        )

        testDao.insert(invalidEntity)
        
        // Service should handle invalid URIs gracefully
        val retrieved = testDao.getById("invalid_test")
        assertNotNull(retrieved)
        assertEquals("invalid://uri", retrieved!!.uriString)
    }

    @Test
    fun testServiceInitialization() {
        // Test that service initializes properly
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkManager = NetworkQualityManager(context)
        val service = PhotoUploadService(context, networkManager)
        
        assertNotNull(service)
        service.shutdown()
    }
}

/**
 * Simple integration tests
 */
@RunWith(AndroidJUnit4::class)
class PhotoUploadIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var photoUploadService: PhotoUploadService
    private lateinit var networkQualityManager: NetworkQualityManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        networkQualityManager = NetworkQualityManager(context)
        photoUploadService = PhotoUploadService(context, networkQualityManager)
    }

    @After
    fun cleanup() {
        photoUploadService.shutdown()
    }

    @Test
    fun testBasicUploadFlow() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService, networkQualityManager)
        }

        // Simulate photo selection and upload
        val mockUri = Uri.parse("content://media/external/images/media/test")
        
        runBlocking {
            val uploadRequest = SerializablePhotoUploadRequest(
                uri = mockUri,
                fileName = "integration_test.jpg",
                targetParseObjectId = "TEST_FOWL_ID",
                targetClassName = "Fowl",
                targetField = "primaryImage"
            )
            photoUploadService.enqueueUpload(uploadRequest)
        }

        composeTestRule.waitForIdle()
        
        // Verify upload UI appears
        composeTestRule
            .onNodeWithText("Upload Queue")
            .assertIsDisplayed()
    }

    @Test
    fun testOfflineUploadQueue() {
        val mockUri = Uri.parse("content://media/external/images/media/offline_test")
        
        runBlocking {
            val uploadRequest = SerializablePhotoUploadRequest(
                uri = mockUri,
                fileName = "offline_test.jpg",
                targetParseObjectId = "OFFLINE_FOWL_ID",
                targetClassName = "Fowl",
                targetField = "primaryImage"
            )
            photoUploadService.enqueueUpload(uploadRequest)
        }
        
        // Verify upload is queued
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService, networkQualityManager)
        }
        
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithText("Upload Queue")
            .assertIsDisplayed()
    }
}
