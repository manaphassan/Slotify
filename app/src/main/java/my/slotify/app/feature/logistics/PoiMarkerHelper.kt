package my.slotify.app.feature.logistics

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Helper to generate high-resolution teardrop map pins with category colors
 * and embedded emoji badges for OpenStreetMap (osmdroid) markers.
 */
object PoiMarkerHelper {

    private val markerCache = mutableMapOf<String, Drawable>()

    /**
     * Gets or creates a custom teardrop pin drawable for a given POI item.
     */
    fun getPoiMarker(
        context: Context,
        poi: PoiItem,
        isSelected: Boolean = false
    ): Drawable {
        val cacheKey = "${poi.id}_${poi.iconEmoji}_${poi.category.name}_$isSelected"
        markerCache[cacheKey]?.let { return it }

        val categoryColor = when (poi.category) {
            PoiCategory.RUMAH -> Color.parseColor("#16A34A")   // Vibrant Green
            PoiCategory.SEKOLAH -> Color.parseColor("#0284C7") // Sky Blue
            PoiCategory.TRANSIT -> Color.parseColor("#F59E0B") // Amber Gold
            PoiCategory.PEJABAT -> Color.parseColor("#475569") // Slate Gray
            PoiCategory.ALL -> Color.parseColor("#0284C7")
        }

        val drawable = createPinDrawable(
            context = context,
            emoji = poi.iconEmoji,
            pinColor = categoryColor,
            isSelected = isSelected
        )
        markerCache[cacheKey] = drawable
        return drawable
    }

    /**
     * Creates a high-visibility target pin for dropped/picked points on the map.
     */
    fun getTargetPin(
        context: Context,
        emoji: String = "🎯"
    ): Drawable {
        val cacheKey = "target_pin_$emoji"
        markerCache[cacheKey]?.let { return it }

        val drawable = createPinDrawable(
            context = context,
            emoji = emoji,
            pinColor = Color.parseColor("#EF4444"), // Crimson Red
            isSelected = true
        )
        markerCache[cacheKey] = drawable
        return drawable
    }

    /**
     * Creates a high-visibility animated vehicle marker (Van / Car) with a golden pulsing halo
     */
    fun getVehicleMarker(
        context: Context,
        emoji: String = "🚐"
    ): Drawable {
        val cacheKey = "vehicle_pin_$emoji"
        markerCache[cacheKey]?.let { return it }

        val density = context.resources.displayMetrics.density
        val size = (50 * density).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val r = size * 0.36f

        // 1. Soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy + 3 * density, r + 2 * density, shadowPaint)

        // 2. Amber pulsing halo
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B") // Amber 500
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r + 3 * density, haloPaint)

        // 3. Crisp white outer ring
        val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawCircle(cx, cy, r + 3 * density, whiteRing)

        // 4. Inner disc
        val innerDisc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7") // Warm cream amber-50
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, innerDisc)

        // 5. Vehicle Emoji in center
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = r * 1.35f
        }
        val fm = emojiPaint.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(emoji, cx, textY, emojiPaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        markerCache[cacheKey] = drawable
        return drawable
    }

    private fun createPinDrawable(
        context: Context,
        emoji: String,
        pinColor: Int,
        isSelected: Boolean
    ): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (46 * density).toInt()
        val height = (60 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = width / 2f
        val r = width * 0.40f
        val cy = r + 4 * density
        val bottomY = height - 4 * density

        // 1. Soft ground contact shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            cx - 9 * density, height - 7 * density,
            cx + 9 * density, height - 1 * density,
            shadowPaint
        )

        // 2. Halo glow if selected
        if (isSelected) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8") // Sky-400
                style = Paint.Style.STROKE
                strokeWidth = 3.5f * density
            }
            canvas.drawCircle(cx, cy, r + 3.5f * density, haloPaint)
        }

        // 3. Teardrop pin geometry
        val path = Path()
        val dy = bottomY - cy
        val angleDeg = Math.toDegrees(Math.asin((r / dy).toDouble().coerceIn(-1.0, 1.0))).toFloat()
        val startAngle = 90f + angleDeg
        val sweepAngle = 360f - 2f * angleDeg
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        path.arcTo(oval, startAngle, sweepAngle)
        path.lineTo(cx, bottomY)
        path.close()

        // 4. Fill pin body
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pinColor
        }
        canvas.drawPath(path, fillPaint)

        // 5. White border outline
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            color = Color.WHITE
        }
        canvas.drawPath(path, strokePaint)

        // 6. Inner circular white badge
        val innerR = r * 0.68f
        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(cx, cy, innerR, innerCirclePaint)

        // 7. Render emoji inside badge
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = innerR * 1.35f
        }
        val fm = emojiPaint.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(emoji, cx, textY, emojiPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun clearCache() {
        markerCache.clear()
    }
}
