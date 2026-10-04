# Android

**Kotlin + Jetpack Compose**, Material 3 Expressive.

## Jak navázat

1. Modul `apps/android` s Android Gradle Pluginem (vyžaduje Android SDK; v cloudovém
   prostředí zatím není).
2. V `shared/core` přidat `androidTarget()` vedle `jvm()`. Do té doby lze použít JVM
   artefakt jádra.
3. Tokeny: `app.ulovko.design.DesignTokens` (ARGB barvy, rozestupy) a `HapticToken`.
   Mapování haptiky na `HapticFeedbackConstants` / `VibrationEffect.Composition` je
   popsané v `design/tokens/tokens.json` a v `docs/05-ux-ui.md`.
4. Integrace: cíl sdílení, zkratky aplikace, widget hlídaných cen, akce v notifikacích,
   prediktivní gesto zpět.
