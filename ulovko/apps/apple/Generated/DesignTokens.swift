// Vygenerováno z design/tokens/tokens.json. Needitovat ručně: node design/tokens/generate.mjs
import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

public enum UlovkoColor {
    /// pozadí za oknem aplikace
    public static let bg = Color(light: 0xE9EEEA, dark: 0x070B09)
    /// pozadí obrazovky
    public static let app = Color(light: 0xF5F7F5, dark: 0x0D1311)
    /// karty, řádky nabídek, listy
    public static let surface = Color(light: 0xFFFFFF, dark: 0x151D1A)
    /// segmenty, miniatury, sekundární plochy
    public static let surface2 = Color(light: 0xE8EEEA, dark: 0x1C2723)
    /// hlavní text, ceny
    public static let fg = Color(light: 0x0F1A15, dark: 0xE7EEEA)
    /// doplňkový text
    public static let muted = Color(light: 0x55635C, dark: 0x97A59E)
    /// oddělovače, obrysy
    public static let line = Color(light: 0xD5DDD8, dark: 0x28342F)
    /// hlavní akce, výběr
    public static let brand = Color(light: 0x12614F, dark: 0x45C29B)
    /// text na brand ploše
    public static let brandInk = Color(light: 0xFFFFFF, dark: 0x04241B)
    /// zvýraznění výběru, upozornění
    public static let brandSoft = Color(light: 0xD7EBE3, dark: 0x16372D)
    /// úlovky (text)
    public static let gold = Color(light: 0x8A5E0B, dark: 0xEBBB55)
    /// úlovky (grafika, hvězdy)
    public static let goldStrong = Color(light: 0xD69E1A, dark: 0xE3A928)
    /// pozadí odměn
    public static let goldSoft = Color(light: 0xF6EACD, dark: 0x372B10)
    /// ověřená cena, vysoká důvěra
    public static let good = Color(light: 0x16764A, dark: 0x52C48D)
    /// štítek ověřeno
    public static let goodSoft = Color(light: 0xDCF0E5, dark: 0x123324)
    /// odhad ceny, střední důvěra
    public static let warn = Color(light: 0x965500, dark: 0xE6A44A)
    /// pozadí varování
    public static let warnSoft = Color(light: 0xFAEBD4, dark: 0x382711)
    /// chyba, minusy v recenzích
    public static let bad = Color(light: 0xB42318, dark: 0xF2766D)
    /// pozadí chyby
    public static let badSoft = Color(light: 0xFADFDB, dark: 0x3D1714)
}

public enum UlovkoSpace {
    public static let xxs: CGFloat = 2
    public static let xs: CGFloat = 4
    public static let s: CGFloat = 8
    public static let m: CGFloat = 12
    public static let l: CGFloat = 16
    public static let xl: CGFloat = 24
    public static let xxl: CGFloat = 32
}

public enum UlovkoRadius {
    public static let s: CGFloat = 10
    public static let m: CGFloat = 14
    public static let l: CGFloat = 22
    public static let sheet: CGFloat = 26
}

public enum UlovkoTypeScale {
    public static let caption: CGFloat = 12
    public static let footnote: CGFloat = 13
    public static let body: CGFloat = 15
    public static let title: CGFloat = 20
    public static let headline: CGFloat = 28
    public static let price: CGFloat = 24
    public static let priceHero: CGFloat = 40
}

public enum HapticToken: String, CaseIterable {
    /// segment řazení, zarážka posuvníku, výběr varianty
    case selection = "vyber"
    /// swipe akce zaklapne, tah k obnovení
    case threshold = "prah"
    /// přidáno do hlídání, recenze odeslána
    case success = "uspech"
    /// akce se nepovedla
    case error = "chyba"
    /// cena se před nákupem změnila
    case warning = "varovani"
    /// cena klesla pod cíl, připsané úlovky
    case catchMoment = "ulovek"

    /// Nativní odezva. Vlastní vzory (úlovek) doplní Core Haptics vrstva.
    @available(iOS 17.0, macOS 14.0, *)
    public var sensoryFeedback: SensoryFeedback {
        switch self {
        case .selection: return .selection
        case .threshold: return .impact(weight: .medium)
        case .success: return .success
        case .error: return .error
        case .warning: return .warning
        case .catchMoment: return .success
        }
    }
}

extension Color {
    /// Barva, která se sama přepne podle světlého a tmavého režimu.
    init(light: UInt32, dark: UInt32) {
        #if canImport(UIKit)
        self.init(uiColor: UIColor { traits in
            UIColor(rgb: traits.userInterfaceStyle == .dark ? dark : light)
        })
        #elseif canImport(AppKit)
        self.init(nsColor: NSColor(name: nil) { appearance in
            let isDark = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
            return NSColor(rgb: isDark ? dark : light)
        })
        #else
        self.init(red: Double((light >> 16) & 0xFF) / 255, green: Double((light >> 8) & 0xFF) / 255, blue: Double(light & 0xFF) / 255)
        #endif
    }
}

#if canImport(UIKit)
private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(red: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255, blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}
#elseif canImport(AppKit)
private extension NSColor {
    convenience init(rgb: UInt32) {
        self.init(srgbRed: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255, blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
    }
}
#endif
