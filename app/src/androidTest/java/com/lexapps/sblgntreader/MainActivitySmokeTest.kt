package com.lexapps.sblgntreader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @Test
    fun packageName_matchesExpected() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.lexapps.sblgntreader", context.packageName)

        // Reference main source so Android Studio keeps the module dependency.
        MainActivity::class.qualifiedName
    }
}

