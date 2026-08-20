package com.lelebox.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.lelebox.app.R

/**
 * 轻量音效引擎：受「设置→音效开关」（lelebox_settings/sound_enabled，默认开）控制。
 * 音量由手机媒体音量（侧键）调节。
 */
object Sfx {

    @Volatile
    private var pool: SoundPool? = null
    private val loaded = mutableMapOf<Int, Int>()

    private fun ensure(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
        pool = p
        loaded[R.raw.sfx_click] = p.load(context, R.raw.sfx_click, 1)
        loaded[R.raw.sfx_success] = p.load(context, R.raw.sfx_success, 1)
        loaded[R.raw.sfx_fail] = p.load(context, R.raw.sfx_fail, 1)
    }

    /** 播放音效；开关关闭或未初始化时静默 */
    fun play(context: Context?, resId: Int) {
        if (context == null) return
        val prefs = context.getSharedPreferences("lelebox_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sound_enabled", true)) return
        ensure(context)
        val id = loaded[resId] ?: return
        pool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun click(context: Context?) = play(context, R.raw.sfx_click)
    fun success(context: Context?) = play(context, R.raw.sfx_success)
    fun fail(context: Context?) = play(context, R.raw.sfx_fail)

    fun release() {
        pool?.release()
        pool = null
        loaded.clear()
    }
}
