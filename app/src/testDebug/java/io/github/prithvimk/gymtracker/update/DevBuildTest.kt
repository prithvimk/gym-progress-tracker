package io.github.prithvimk.gymtracker.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevBuildTest {
    @Test
    fun `parses version json as CI writes it`() {
        val build = parseDevBuild("""{"versionCode": 42, "commit": "0096633dd31335175812064afca1a6070f493d9c"}""")
        assertEquals(DevBuild(42, "0096633dd31335175812064afca1a6070f493d9c"), build)
        assertEquals("0096633", build?.shortCommit)
    }

    @Test
    fun `ignores fields it does not know`() {
        assertEquals(DevBuild(7, "abc"), parseDevBuild("""{"versionCode": 7, "commit": "abc", "extra": true}"""))
    }

    @Test
    fun `returns null for anything else`() {
        assertNull(parseDevBuild("<html>Not Found</html>"))
        assertNull(parseDevBuild("""{"commit": "abc"}"""))
        assertNull(parseDevBuild(""))
    }

    @Test
    fun `only a higher version code counts as newer`() {
        val build = DevBuild(42, "abc")
        assertTrue(build.isNewerThan(41))
        assertFalse(build.isNewerThan(42))
        assertFalse(build.isNewerThan(43))
    }
}
