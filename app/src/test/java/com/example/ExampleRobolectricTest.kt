package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.mediastore.MediaItemModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("eve", appName)
  }

  @Test
  fun `verify duration formatting`() {
    assertEquals("00:00", MediaItemModel.formatDuration(0L))
    assertEquals("03:25", MediaItemModel.formatDuration(205000L))
    assertEquals("1:15:30", MediaItemModel.formatDuration(4530000L))
  }

  @Test
  fun `verify file size formatting`() {
    assertEquals("0 B", MediaItemModel.formatFileSize(0L))
    assertEquals("1.5 MB", MediaItemModel.formatFileSize(1572864L))
    assertEquals("2.0 GB", MediaItemModel.formatFileSize(2147483648L))
  }
}
