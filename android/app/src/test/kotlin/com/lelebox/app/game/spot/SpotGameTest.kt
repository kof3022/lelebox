package com.lelebox.app.game.spot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotGameTest {

    @Test
    fun `difficulty has expected diff count`() {
        assertEquals(5, SpotGame(SpotLevel.EASY).diffs.size)
        assertEquals(7, SpotGame(SpotLevel.HARD).diffs.size)
    }

    @Test
    fun `diffs are within bounds and non-overlapping`() {
        for (level in SpotLevel.entries) {
            val game = SpotGame(level)
            for (d in game.diffs) {
                assertTrue("rect 越界: ${d.rect}", d.rect.left >= 0 && d.rect.top >= 0 && d.rect.right <= 1000 && d.rect.bottom <= 1000)
                assertTrue("描述为空", d.desc.isNotBlank())
            }
            // 两两不重叠
            for (i in game.diffs.indices) {
                for (j in i + 1 until game.diffs.size) {
                    assertFalse("差异重叠: $i-$j", game.diffs[i].rect.overlaps(game.diffs[j].rect))
                }
            }
        }
    }

    @Test
    fun `tapping diff center finds it once`() {
        val game = SpotGame(SpotLevel.EASY)
        val d = game.diffs[0]
        val cx = d.rect.center.x
        val cy = d.rect.center.y
        assertTrue(game.checkTap(cx, cy))
        assertEquals(1, game.found.size)
        // 再点同一处不算
        assertFalse(game.checkTap(cx, cy))
        assertEquals(1, game.found.size)
        assertEquals(1, game.misses)
    }

    @Test
    fun `tapping empty area counts miss`() {
        val game = SpotGame(SpotLevel.EASY)
        assertFalse(game.checkTap(10f, 10f))
        assertEquals(1, game.misses)
    }

    @Test
    fun `finding all diffs ends game`() {
        val game = SpotGame(SpotLevel.EASY)
        for (d in game.diffs) {
            assertFalse(game.over)
            game.checkTap(d.rect.center.x, d.rect.center.y)
        }
        assertTrue(game.over)
        assertEquals(game.diffs.size, game.found.size)
    }

    @Test
    fun `hint returns unfound diff index and -1 when done`() {
        val game = SpotGame(SpotLevel.EASY)
        val h = game.hint()
        assertTrue(h >= 0 && h < game.diffs.size)
        // 找齐后提示 -1
        for (d in game.diffs) game.checkTap(d.rect.center.x, d.rect.center.y)
        assertEquals(-1, game.hint())
    }
}
