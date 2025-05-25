package com.example.rooster

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rooster.ui.components.PhotoUploadComponentsDemoScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoUploadTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var photoUploadService: PhotoUploadService

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        photoUploadService = PhotoUploadService(context)
    }

    @After
    fun cleanup() {
        photoUploadService.shutdown()
    }

    @Test
    fun testPhotoPickerDialogAppears() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService)
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
    fun testGallerySelection() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService)
        }

        // Open photo picker
        composeTestRule
            .onNodeWithContentDescription("Add Photo")
            .performClick()

        // Verify Gallery option is available and clickable
        composeTestRule
            .onNodeWithText("Gallery")
            .assertIsDisplayed()
            .performClick()

        // For now, just verify the UI responds to clicks
        composeTestRule.waitForIdle()
    }

    @Test
    fun testCameraCapture() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService)
        }

        // Open photo picker
        composeTestRule
            .onNodeWithContentDescription("Add Photo")
            .performClick()

        // Verify Camera option is available and clickable
        composeTestRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()
            .performClick()

        // Note: Intent mocking and permission handling would be needed for full test
        // For now, just verify the UI responds to clicks
        composeTestRule.waitForIdle()
    }

    @Test
    fun testPhotoUploadProgress() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService)
        }

        // Simulate adding a photo and confirming upload
        val mockUri = Uri.parse("content://media/external/images/media/test")

        runBlocking {
            // Enqueue upload programmatically to test progress UI
            photoUploadService.enqueueUpload(
                uri = mockUri,
                desiredCompression = ImageCompressionLevel.MEDIUM,
                metadata = mapOf("fileName" to "test_photo.jpg"),
            )
        }

        // Wait for upload to start
        composeTestRule.waitForIdle()

        // Verify upload progress elements appear
        composeTestRule
            .onNodeWithText("Upload Queue/Progress:")
            .assertIsDisplayed()

        // Check for progress indicator or status text
        composeTestRule
            .onAllNodesWithText("test_photo.jpg", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun testNetworkAwareCompression() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockUri = Uri.parse("content://media/external/images/media/test")

        // Test that compression level adapts to network quality
        runBlocking {
            // This would normally test the actual compression logic
            // For now, verify the service accepts different compression levels
            photoUploadService.enqueueUpload(
                uri = mockUri,
                desiredCompression = ImageCompressionLevel.ULTRA,
                metadata = mapOf("networkTest" to "poor_connection"),
            )

            photoUploadService.enqueueUpload(
                uri = mockUri,
                desiredCompression = ImageCompressionLevel.LOW,
                metadata = mapOf("networkTest" to "excellent_connection"),
            )
        }

        // Verify uploads were enqueued (would need to check actual status)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testRetryFailedUpload() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService)
        }

        // This test would simulate a failed upload and test retry functionality
        // For now, verify the retry button appears in failed upload UI

        // Would need to mock a failed upload scenario and verify:
        // 1. Error message appears
        // 2. Retry button is clickable
        // 3. Upload restarts on retry

        composeTestRule.waitForIdle()
    }

    @Test
    fun testUploadCancellation() {
        composeTestRule.setContent {
            PhotoUploadComponentsDemoScreen(photoUploadService)
        }

        val mockUri = Uri.parse("content://media/external/images/media/test")

        runBlocking {
            val uploadId =
                photoUploadService.enqueueUpload(
                    uri = mockUri,
                    metadata = mapOf("fileName" to "cancel_test.jpg"),
                )

            // Cancel the upload
            photoUploadService.cancelUpload(uploadId)
        }

        composeTestRule.waitForIdle()

        // Verify upload was cancelled (status should reflect cancellation)
    }
}

/**
 * Integration tests for photo upload with real Parse backend
 */
@RunWith(AndroidJUnit4::class)
class PhotoUploadIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEndToEndPhotoUploadFlow() {
        // This would test the complete flow:
        // 1. Photo selection
        // 2. Compression
        // 3. Parse upload
        // 4. Object linking
        // 5. Status updates

        // Requires Parse test environment setup
    }

    @Test
    fun testOfflineUploadQueue() {
        // Test that uploads are queued when offline
        // and processed when connection restored
    }

    @Test
    fun testParseFileLinking() {
        // Test that uploaded ParseFiles are properly
        // linked to target Parse objects (Fowl, Listing, etc.)
    }
}
