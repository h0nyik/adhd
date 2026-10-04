// Vygenerováno z design/tokens/tokens.json. Needitovat ručně: node design/tokens/generate.mjs
package app.ulovko.design

/** Barvy jako ARGB (0xAARRGGBB), jedna sada pro světlý a jedna pro tmavý režim. */
data class ColorScheme(
    val bg: Long,
    val app: Long,
    val surface: Long,
    val surface2: Long,
    val fg: Long,
    val muted: Long,
    val line: Long,
    val brand: Long,
    val brandInk: Long,
    val brandSoft: Long,
    val gold: Long,
    val goldStrong: Long,
    val goldSoft: Long,
    val good: Long,
    val goodSoft: Long,
    val warn: Long,
    val warnSoft: Long,
    val bad: Long,
    val badSoft: Long,
) {
    fun byName(name: String): Long = when (name) {
        "bg" -> bg
        "app" -> app
        "surface" -> surface
        "surface2" -> surface2
        "fg" -> fg
        "muted" -> muted
        "line" -> line
        "brand" -> brand
        "brandInk" -> brandInk
        "brandSoft" -> brandSoft
        "gold" -> gold
        "goldStrong" -> goldStrong
        "goldSoft" -> goldSoft
        "good" -> good
        "goodSoft" -> goodSoft
        "warn" -> warn
        "warnSoft" -> warnSoft
        "bad" -> bad
        "badSoft" -> badSoft
        else -> error("Neznámá barva $name")
    }
}

data class ContrastRequirement(val foreground: String, val background: String, val minRatio: Double)

object DesignTokens {
    val light = ColorScheme(
        bg = 0xFFE9EEEA,
        app = 0xFFF5F7F5,
        surface = 0xFFFFFFFF,
        surface2 = 0xFFE8EEEA,
        fg = 0xFF0F1A15,
        muted = 0xFF55635C,
        line = 0xFFD5DDD8,
        brand = 0xFF12614F,
        brandInk = 0xFFFFFFFF,
        brandSoft = 0xFFD7EBE3,
        gold = 0xFF8A5E0B,
        goldStrong = 0xFFD69E1A,
        goldSoft = 0xFFF6EACD,
        good = 0xFF16764A,
        goodSoft = 0xFFDCF0E5,
        warn = 0xFF965500,
        warnSoft = 0xFFFAEBD4,
        bad = 0xFFB42318,
        badSoft = 0xFFFADFDB,
    )
    val dark = ColorScheme(
        bg = 0xFF070B09,
        app = 0xFF0D1311,
        surface = 0xFF151D1A,
        surface2 = 0xFF1C2723,
        fg = 0xFFE7EEEA,
        muted = 0xFF97A59E,
        line = 0xFF28342F,
        brand = 0xFF45C29B,
        brandInk = 0xFF04241B,
        brandSoft = 0xFF16372D,
        gold = 0xFFEBBB55,
        goldStrong = 0xFFE3A928,
        goldSoft = 0xFF372B10,
        good = 0xFF52C48D,
        goodSoft = 0xFF123324,
        warn = 0xFFE6A44A,
        warnSoft = 0xFF382711,
        bad = 0xFFF2766D,
        badSoft = 0xFF3D1714,
    )

    /** Dvojice barev, které musí splnit kontrast WCAG (hlídá test). */
    val contrastRequirements = listOf(
        ContrastRequirement("fg", "app", 4.5),
        ContrastRequirement("fg", "surface", 4.5),
        ContrastRequirement("muted", "surface", 4.5),
        ContrastRequirement("muted", "app", 4.5),
        ContrastRequirement("brandInk", "brand", 4.5),
        ContrastRequirement("brand", "surface", 4.5),
        ContrastRequirement("good", "surface", 4.5),
        ContrastRequirement("warn", "surface", 4.5),
        ContrastRequirement("warn", "warnSoft", 4.5),
        ContrastRequirement("gold", "goldSoft", 4.5),
        ContrastRequirement("bad", "surface", 4.5),
    )

    object TypeScale {
        const val caption = 12
        const val footnote = 13
        const val body = 15
        const val title = 20
        const val headline = 28
        const val price = 24
        const val priceHero = 40
    }

    object Space {
        const val xxs = 2
        const val xs = 4
        const val s = 8
        const val m = 12
        const val l = 16
        const val xl = 24
        const val xxl = 32
    }

    object Radius {
        const val s = 10
        const val m = 14
        const val l = 22
        const val sheet = 26
    }

    object Motion {
        const val springResponseMs = 380
        const val springDampingPercent = 85
        const val fastMs = 180
        const val standardMs = 320
    }
}

/**
 * Sémantická haptika. Platformní vrstva ji přeloží na nativní odezvu;
 * [webPattern] je vzor pro navigator.vibrate (ms).
 */
enum class HapticToken(val key: String, val webPattern: LongArray) {
    /** segment řazení, zarážka posuvníku, výběr varianty. iOS .selection, Android HapticFeedbackConstants.SEGMENT_TICK. */
    SELECTION("vyber", longArrayOf(8)),
    /** swipe akce zaklapne, tah k obnovení. iOS .impact(weight: .medium), Android HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE. */
    THRESHOLD("prah", longArrayOf(15)),
    /** přidáno do hlídání, recenze odeslána. iOS .success, Android HapticFeedbackConstants.CONFIRM. */
    SUCCESS("uspech", longArrayOf(10, 40, 10)),
    /** akce se nepovedla. iOS .error, Android HapticFeedbackConstants.REJECT. */
    ERROR("chyba", longArrayOf(30, 40, 30)),
    /** cena se před nákupem změnila. iOS .warning, Android VibrationEffect.Composition (vlastní). */
    WARNING("varovani", longArrayOf(20, 60, 20)),
    /** cena klesla pod cíl, připsané úlovky. iOS .success (+ vlastní Core Haptics vzor), Android VibrationEffect.Composition: PRIMITIVE_QUICK_RISE + PRIMITIVE_CLICK. */
    CATCH("ulovek", longArrayOf(8, 30, 8, 30, 24)),
}
