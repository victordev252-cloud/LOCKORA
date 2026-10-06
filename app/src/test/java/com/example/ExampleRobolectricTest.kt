package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    assertEquals("Lockora", appName)
  }

  @Test
  fun `verify security manager hashing and verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sec = SecurityManager.get(context)
    sec.clear()
    assertFalse(sec.isCredentialSet())

    sec.setCredential("1234".toCharArray(), "pin")
    assertTrue(sec.isCredentialSet())
    assertEquals("pin", sec.credentialMode())
    assertTrue(sec.verify("1234".toCharArray()))
    assertFalse(sec.verify("0000".toCharArray()))
  }
}
