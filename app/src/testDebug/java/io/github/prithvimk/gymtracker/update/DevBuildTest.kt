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

    @Test
    fun `parses version json with a PR number`() {
        assertEquals(DevBuild(30, "abc"), parseDevBuild("""{"versionCode": 30, "commit": "abc", "pr": 9}"""))
    }

    @Test
    fun `PR builds are picked out of the releases list, newest PR first`() {
        val releases =
            """
            [
              {"tag_name": "dev", "name": "Dev build", "prerelease": true, "draft": false},
              {"tag_name": "pr-9", "name": "PR #9: Last time column", "prerelease": true, "draft": false},
              {"tag_name": "pr-12", "name": "PR #12: Updater: PR builds", "prerelease": true, "draft": false},
              {"tag_name": "pr-13", "name": "PR #13: Draft", "prerelease": true, "draft": true},
              {"tag_name": "v1.0.0", "name": "v1.0.0", "prerelease": false, "draft": false},
              {"tag_name": "pr-x", "name": "Not a PR", "prerelease": true, "draft": false}
            ]
            """

        assertEquals(
            listOf(
                PrRelease("pr-12", 12, "Updater: PR builds"),
                PrRelease("pr-9", 9, "Last time column"),
            ),
            parsePrReleases(releases),
        )
    }

    @Test
    fun `a releases list that isn't one gives null`() {
        assertNull(parsePrReleases("""{"message": "API rate limit exceeded"}"""))
        assertEquals(emptyList<PrRelease>(), parsePrReleases("[]"))
    }
}
