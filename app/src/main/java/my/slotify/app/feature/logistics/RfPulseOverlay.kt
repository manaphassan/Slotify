package my.slotify.app.feature.logistics

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs
import kotlin.math.cos
import androidx.core.graphics.toColorInt

/**
 * High-tech Animated RF Range Pulse Overlay (3.0 km radius).
 * Displays:
 * 1. 3.0 km outer RF boundary (dashed cyan/blue).
 * 2. 1.0 km & 2.0 km concentric distance guide rings.
 * 3. Smooth animated outward expanding radar pulse wave ripples.
 * 4. Center beacon dot with radio ping indicator.
 * 5. Tap-to-coordinate listener for picking coordinates directly on the map.
 */
class RfPulseOverlay(
    var center: GeoPoint? = null,
    var isPulseActive: Boolean = true,
    var maxRadiusMeters: Double = 3000.0,
    var centerLabel: String = "Transmitter RF (3.0 km)",
    var onMapTapped: ((GeoPoint) -> Unit)? = null,
    var onMapLongPressed: ((GeoPoint) -> Unit)? = null
) : Overlay() {

    private var animationPhase = 0f // 0.0f to 1.0f

    // Shaded coverage fill
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#120284C7".toColorInt() // 7% translucent cyan
    }

    // 3km Outer limit boundary
    private val boundaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = "#0284C7".toColorInt()
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(14f, 8f), 0f)
    }

    // 1km & 2km Inner guide rings
    private val guideRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = "#440284C7".toColorInt()
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    // Radar pulse wave rings
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Distance & status text
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#0369A1".toColorInt()
        textSize = 24f
        isFakeBoldText = true
    }

    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = "#0284C7".toColorInt()
    }

    private val centerWhiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.WHITE
    }

    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false
        val proj = mapView.projection ?: return false
        val geo = proj.fromPixels(e.x.toInt(), e.y.toInt())
        if (geo != null) {
            onMapTapped?.invoke(GeoPoint(geo.latitude, geo.longitude))
        }
        return false
    }

    override fun onLongPress(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false
        val proj = mapView.projection ?: return false
        val geo = proj.fromPixels(e.x.toInt(), e.y.toInt())
        if (geo != null) {
            onMapLongPressed?.invoke(GeoPoint(geo.latitude, geo.longitude))
            return true
        }
        return false
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow || canvas == null || mapView == null) return
        val c = center ?: return
        val proj = mapView.projection ?: return

        val centerPx = Point()
        proj.toPixels(c, centerPx)

        // Calculate 3000m radius in screen pixels accurately using latitude scaling
        val metersPerDegLng = 111320.0 * cos(Math.toRadians(c.latitude))
        val deltaLng = maxRadiusMeters / metersPerDegLng
        val edgePx = Point()
        proj.toPixels(GeoPoint(c.latitude, c.longitude + deltaLng), edgePx)
        val maxRadiusPx = abs(edgePx.x - centerPx.x).toFloat()

        if (maxRadiusPx <= 2f) return

        // 1. Coverage area shaded fill & 3km boundary
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), maxRadiusPx, fillPaint)
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), maxRadiusPx, boundaryPaint)

        // 2. Concentric guide rings at 1.0 km (1/3) and 2.0 km (2/3)
        val r1km = maxRadiusPx * (1.0f / 3.0f)
        val r2km = maxRadiusPx * (2.0f / 3.0f)
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), r1km, guideRingPaint)
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), r2km, guideRingPaint)

        // Range labels
        canvas.drawText("1 km", centerPx.x.toFloat() + r1km + 6f, centerPx.y.toFloat() - 4f, textPaint)
        canvas.drawText("2 km", centerPx.x.toFloat() + r2km + 6f, centerPx.y.toFloat() - 4f, textPaint)
        canvas.drawText("3.0 km RF MAX", centerPx.x.toFloat() + maxRadiusPx + 8f, centerPx.y.toFloat() - 4f, textPaint)

        // 3. Smooth outward animated expanding pulse waves
        if (isPulseActive) {
            val waveCount = 3
            for (i in 0 until waveCount) {
                val phase = (animationPhase + i.toFloat() / waveCount) % 1.0f
                val currentRadius = maxRadiusPx * phase
                // Fade alpha from ~170 to 0 as it hits the 3km boundary
                val alpha = ((1.0f - phase) * 170).toInt().coerceIn(0, 255)
                wavePaint.color = android.graphics.Color.argb(alpha, 2, 132, 199) // Sky-600
                wavePaint.strokeWidth = 3f + (1f - phase) * 4f
                canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), currentRadius, wavePaint)
            }

            // Animate phase forward and request next frame (~40fps)
            animationPhase = (animationPhase + 0.014f) % 1.0f
            mapView.postInvalidateDelayed(25)
        }

        // 4. Center beacon dot
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), 12f, centerDotPaint)
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), 12f, centerWhiteRing)
    }
}
