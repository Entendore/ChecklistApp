package com.example.checklistapp.memory

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.checklistapp.MainActivity
import com.example.checklistapp.testutil.TestSetup
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ActivityLeakTest {

    @Before fun setUp() =
        TestSetup.isolate(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun mainActivityIsCollectedAfterDestroy() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val ref = AtomicReference<WeakReference<*>?>(null)
        scenario.onActivity { ref.set(WeakReference(it)) }
        scenario.moveToState(Lifecycle.State.DESTROYED)
        scenario.close()
        assertTrue("MainActivity leaked!", waitForCollection(checkNotNull(ref.get())))
    }

    @Test
    fun oldInstanceIsCollectedAfterRecreate() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val ref = AtomicReference<WeakReference<*>?>(null)
        scenario.onActivity { ref.set(WeakReference(it)) }
        scenario.recreate()
        scenario.moveToState(Lifecycle.State.DESTROYED)
        scenario.close()
        assertTrue("Old activity instance leaked across recreation!",
            waitForCollection(checkNotNull(ref.get())))
    }

    private fun waitForCollection(ref: WeakReference<*>): Boolean {
        repeat(20) {
            if (ref.get() == null) return true
            Runtime.getRuntime().gc()
            System.runFinalization()
            try { Thread.sleep(100) } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        return ref.get() == null
    }
}
