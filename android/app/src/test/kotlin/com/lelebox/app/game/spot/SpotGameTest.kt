package com.lelebox.app.game.spot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotGameTest {

    private fun allScenes(): List<Pair<SpotLevel, SpotSceneDef>> =
        SpotLevel.entries.flatMap { lv -> lv.scenes.map { lv to it } }

    @Test
    fun `nine levels with expected diff counts`() {
        assertEquals(3, SpotLevel.EASY.scenes.size)
        assertEquals(3, SpotLevel.MEDIUM.scenes.size)
        assertEquals(3, SpotLevel.HARD.scenes.size)
        assertEquals(5, SpotLevel.EASY.diffCount)
        assertEquals(6, SpotLevel.MEDIUM.diffCount)
        assertEquals(7, SpotLevel.HARD.diffCount)
        for ((lv, scene) in allScenes()) {
            assertEquals("${lv.label} ${scene.name} 差异数", lv.diffCount, scene.diffs.size)
        }
    }

    @Test
    fun `all scene diffs are within bounds and non-overlapping`() {
        for ((_, scene) in allScenes()) {
            for (d in scene.diffs) {
                assertTrue("${scene.name} rect 越界: ${d.rect}", d.rect.left >= 0 && d.rect.top >= 0 && d.rect.right <= 1000 && d.rect.bottom <= 1000)
                assertTrue("${scene.name} 描述为空", d.desc.isNotBlank())
            }
            for (i in scene.diffs.indices) {
                for (j in i + 1 until scene.diffs.size) {
                    assertFalse("${scene.name} 差异重叠: $i-$j", scene.diffs[i].rect.overlaps(scene.diffs[j].rect))
                }
            }
        }
    }

    @Test
    fun `tapping diff center finds it once`() {
        for ((_, scene) in allScenes()) {
            val game = SpotGame(scene)
            val d = game.diffs[0]
            assertTrue(game.checkTap(d.rect.center.x, d.rect.center.y))
            assertEquals(1, game.found.size)
            assertFalse(game.checkTap(d.rect.center.x, d.rect.center.y))
            assertEquals(1, game.found.size)
            assertEquals(1, game.misses)
        }
    }

    @Test
    fun `tapping empty area counts miss`() {
        val game = SpotGame(SpotLevel.EASY.scenes[0])
        assertFalse(game.checkTap(10f, 10f))
        assertEquals(1, game.misses)
    }

    @Test
    fun `finding all diffs ends game for every scene`() {
        for ((_, scene) in allScenes()) {
            val game = SpotGame(scene)
            for (d in game.diffs) game.checkTap(d.rect.center.x, d.rect.center.y)
            assertTrue(scene.name + " 应终局", game.over)
            assertEquals(game.diffs.size, game.found.size)
        }
    }

    @Test
    fun `hint returns unfound diff index and -1 when done`() {
        val game = SpotGame(SpotLevel.HARD.scenes[0])
        assertTrue(game.hint() in game.diffs.indices)
        for (d in game.diffs) game.checkTap(d.rect.center.x, d.rect.center.y)
        assertEquals(-1, game.hint())
    }
}
