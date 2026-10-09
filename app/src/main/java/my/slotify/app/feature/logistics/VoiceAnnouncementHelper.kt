package my.slotify.app.feature.logistics

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Voice announcement & audio chime helper for live geofence and transit alerts.
 * Uses Android TextToSpeech in Bahasa Melayu (ms-MY) with fallback to default locale,
 * accompanied by a gentle tone notification chime.
 */
class VoiceAnnouncementHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        } catch (e: Exception) {
            Log.e("VoiceAnnouncement", "Failed to init ToneGenerator / TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ttsEngine = tts ?: return
            val msLocale = Locale("ms", "MY")
            val result = ttsEngine.setLanguage(msLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceAnnouncement", "Malay locale not supported by device TTS, fallback to default")
                ttsEngine.setLanguage(Locale.getDefault())
            }

            ttsEngine.setPitch(1.05f)
            ttsEngine.setSpeechRate(0.95f)
            isInitialized = true

            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
            })
        } else {
            Log.e("VoiceAnnouncement", "TTS initialization failed with status $status")
            isInitialized = false
        }
    }

    /**
     * Plays a brief soft double chime tone and announces the alert in Bahasa Melayu.
     */
    fun speak(text: String, withChime: Boolean = true) {
        if (withChime) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                Log.w("VoiceAnnouncement", "Tone play failed: ${e.message}")
            }
        }

        if (isInitialized && tts != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "slotify_alert_${System.currentTimeMillis()}")
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                }
            } catch (e: Exception) {
                Log.e("VoiceAnnouncement", "Error speaking utterance: ${e.message}")
            }
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w("VoiceAnnouncement", "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            toneGenerator?.release()
            toneGenerator = null
            isInitialized = false
        } catch (e: Exception) {
            Log.w("VoiceAnnouncement", "Error shutting down: ${e.message}")
        }
    }
}
