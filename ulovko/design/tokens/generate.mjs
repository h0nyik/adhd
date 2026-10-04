#!/usr/bin/env node
// Generuje design tokeny pro všechny platformy z tokens.json.
//   node design/tokens/generate.mjs          zapíše soubory
//   node design/tokens/generate.mjs --check  jen ověří, že jsou aktuální (pro CI)
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..", "..");
const tokens = JSON.parse(readFileSync(join(here, "tokens.json"), "utf8"));
const HEADER = "Vygenerováno z design/tokens/tokens.json. Needitovat ručně: node design/tokens/generate.mjs";

const hex = (value) => {
  if (!/^#[0-9A-Fa-f]{6}$/.test(value)) throw new Error(`Neplatná barva ${value}`);
  return value.slice(1).toUpperCase();
};
const kebab = (name) => name.replace(/[A-Z]/g, (c) => "-" + c.toLowerCase()).replace(/(\d+)/g, "-$1");
const colorNames = Object.keys(tokens.color);
const hapticEntries = Object.entries(tokens.haptic);

function kotlin() {
  const scheme = (mode) =>
    colorNames.map((n) => `        ${n} = 0xFF${hex(tokens.color[n][mode])},`).join("\n");
  const ints = (obj) => Object.entries(obj).map(([k, v]) => `        const val ${k} = ${v}`).join("\n");
  const pairs = tokens.contrast.map((p) => `        ContrastRequirement("${p.fg}", "${p.bg}", ${p.min.toFixed(1)}),`).join("\n");
  const haptics = hapticEntries
    .map(([key, h]) => `    /** ${h.use}. iOS ${h.ios}, Android ${h.android}. */\n    ${h.kotlin}("${key}", longArrayOf(${h.web.join(", ")})),`)
    .join("\n");
  return `// ${HEADER}
package app.ulovko.design

/** Barvy jako ARGB (0xAARRGGBB), jedna sada pro světlý a jedna pro tmavý režim. */
data class ColorScheme(
${colorNames.map((n) => `    val ${n}: Long,`).join("\n")}
) {
    fun byName(name: String): Long = when (name) {
${colorNames.map((n) => `        "${n}" -> ${n}`).join("\n")}
        else -> error("Neznámá barva $name")
    }
}

data class ContrastRequirement(val foreground: String, val background: String, val minRatio: Double)

object DesignTokens {
    val light = ColorScheme(
${scheme("light")}
    )
    val dark = ColorScheme(
${scheme("dark")}
    )

    /** Dvojice barev, které musí splnit kontrast WCAG (hlídá test). */
    val contrastRequirements = listOf(
${pairs}
    )

    object TypeScale {
${ints(tokens.typeScale)}
    }

    object Space {
${ints(tokens.space)}
    }

    object Radius {
${ints(tokens.radius)}
    }

    object Motion {
${ints(tokens.motion)}
    }
}

/**
 * Sémantická haptika. Platformní vrstva ji přeloží na nativní odezvu;
 * [webPattern] je vzor pro navigator.vibrate (ms).
 */
enum class HapticToken(val key: String, val webPattern: LongArray) {
${haptics}
}
`;
}

function swift() {
  const colors = colorNames
    .map((n) => `    /// ${tokens.color[n].use}\n    public static let ${n} = Color(light: 0x${hex(tokens.color[n].light)}, dark: 0x${hex(tokens.color[n].dark)})`)
    .join("\n");
  const doubles = (obj) => Object.entries(obj).map(([k, v]) => `    public static let ${k}: CGFloat = ${v}`).join("\n");
  const cases = hapticEntries.map(([key, h]) => `    /// ${h.use}\n    case ${h.swift} = "${key}"`).join("\n");
  const mapping = hapticEntries.map(([, h]) => `        case .${h.swift}: return ${h.swiftFeedback}`).join("\n");
  return `// ${HEADER}
import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

public enum UlovkoColor {
${colors}
}

public enum UlovkoSpace {
${doubles(tokens.space)}
}

public enum UlovkoRadius {
${doubles(tokens.radius)}
}

public enum UlovkoTypeScale {
${doubles(tokens.typeScale)}
}

public enum HapticToken: String, CaseIterable {
${cases}

    /// Nativní odezva. Vlastní vzory (úlovek) doplní Core Haptics vrstva.
    @available(iOS 17.0, macOS 14.0, *)
    public var sensoryFeedback: SensoryFeedback {
        switch self {
${mapping}
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
`;
}

function css() {
  const vars = (mode, indent) =>
    colorNames.map((n) => `${indent}--${kebab(n)}: #${hex(tokens.color[n][mode])};`).join("\n");
  const px = (prefix, obj) => Object.entries(obj).map(([k, v]) => `  --${prefix}-${kebab(k)}: ${v}px;`).join("\n");
  return `/* ${HEADER} */
:root {
${vars("light", "  ")}
${px("space", tokens.space)}
${px("radius", tokens.radius)}
${px("text", tokens.typeScale)}
  --motion-fast: ${tokens.motion.fastMs}ms;
  --motion-standard: ${tokens.motion.standardMs}ms;
  --font-display: "${tokens.font.display.family}", ui-rounded, system-ui, sans-serif;
  --font-ui: -apple-system, BlinkMacSystemFont, "Segoe UI Variable Text", "Segoe UI", Roboto, sans-serif;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
${vars("dark", "    ")}
    color-scheme: dark;
  }
}
:root[data-theme="dark"] {
${vars("dark", "  ")}
  color-scheme: dark;
}
`;
}

const outputs = [
  ["shared/design/src/commonMain/kotlin/app/ulovko/design/DesignTokens.kt", kotlin()],
  ["apps/apple/Generated/DesignTokens.swift", swift()],
  ["design/tokens/build/tokens.css", css()],
];

const check = process.argv.includes("--check");
let stale = 0;
for (const [path, content] of outputs) {
  const file = join(root, path);
  if (check) {
    const current = existsSync(file) ? readFileSync(file, "utf8") : null;
    if (current !== content) {
      console.error(`Zastaralé: ${relative(root, file)}`);
      stale++;
    }
  } else {
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, content);
    console.log(`Zapsáno: ${relative(root, file)}`);
  }
}
if (stale) {
  console.error("Spusť: node design/tokens/generate.mjs");
  process.exit(1);
}
