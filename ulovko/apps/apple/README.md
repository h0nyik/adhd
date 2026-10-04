# Apple: iOS, iPadOS, macOS

**SwiftUI**, jeden kód pro iPhone, iPad a Mac. Je to i hostitel Safari rozšíření.

## Co už je

- `Generated/DesignTokens.swift`: barvy (samy se přepínají podle světlého a tmavého
  režimu), rozestupy, rádiusy, typografická škála a `HapticToken` s mapováním na
  `SensoryFeedback`. Generuje se z `design/tokens/tokens.json`, ručně se needituje.

## Jak navázat

1. Na Macu: `./gradlew :shared:core:assembleUlovkoCoreXCFramework`
   → `shared/core/build/XCFrameworks/release/UlovkoCore.xcframework`
   (modely, cenový engine a řazení stejné jako na serveru).
2. Založit Xcode projekt `Ulovko` (iOS 17+, macOS 14+), přidat XCFramework
   a `Generated/DesignTokens.swift`.
3. První obrazovky podle prototypu (`prototype/index.html`): hledání s našeptávačem,
   karta věci, list s rozpadem ceny. Haptiku volat výhradně přes `HapticToken`.
4. Cíle navíc: Share Extension, Safari Web Extension (sdílí kód s `apps/extension`),
   App Intents, widget hlídaných cen.

Systémové komponenty dostanou Liquid Glass samy. Vlastní chrome nekreslit.
