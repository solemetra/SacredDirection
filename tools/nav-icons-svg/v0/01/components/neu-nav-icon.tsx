"use client"

const BASE = "#F0EEE9"
const SHADOW_LIGHT = "#ffffff"
const SHADOW_DARK = "#D1CFCB"

export interface NeuNavIconProps {
  size?: number
  /** Unique per icon on the same page (SVG filter id). */
  filterId: string
  ariaLabel: string
  glyphPath?: string
  /** Raster glyph (milk silhouette on transparent) — e.g. dua hands. */
  glyphImage?: string
  glyphImageBox?: { x: number; y: number; w: number; h: number }
}

/**
 * Nav glyph: convex milk disc + concave stamped shape (SVG feFilter).
 * Based on user NavArrow design — exported as raster for Android ImageView.
 */
export function NeuNavIcon({
  size = 180,
  filterId,
  glyphPath,
  glyphImage,
  glyphImageBox = { x: 28, y: 28, w: 124, h: 124 },
  ariaLabel,
}: NeuNavIconProps) {
  const shadowScale = size / 180
  const dropLight = `${-5 * shadowScale}px ${-5 * shadowScale}px ${10 * shadowScale}px ${SHADOW_LIGHT}`
  const dropDark = `${5 * shadowScale}px ${5 * shadowScale}px ${13 * shadowScale}px ${SHADOW_DARK}`

  return (
    <div
      role="img"
      aria-label={ariaLabel}
      style={{
        display: "inline-block",
        borderRadius: "50%",
        filter: `drop-shadow(${dropLight}) drop-shadow(${dropDark})`,
      }}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 180 180"
        xmlns="http://www.w3.org/2000/svg"
        style={{ display: "block" }}
      >
        <defs>
          <filter
            id={filterId}
            x="-10%"
            y="-10%"
            width="120%"
            height="120%"
            colorInterpolationFilters="sRGB"
          >
            <feOffset in="SourceAlpha" dx="3" dy="3" result="dk-shifted" />
            <feGaussianBlur in="dk-shifted" stdDeviation="3" result="dk-blurred" />
            <feComposite in="dk-blurred" in2="SourceAlpha" operator="out" result="dk-inward" />
            <feComposite in="dk-inward" in2="SourceAlpha" operator="in" result="dk-masked" />
            <feFlood floodColor="#9a9894" floodOpacity="0.78" result="dk-color" />
            <feComposite in="dk-color" in2="dk-masked" operator="in" result="dark-shadow" />

            <feOffset in="SourceAlpha" dx="-3" dy="-3" result="lt-shifted" />
            <feGaussianBlur in="lt-shifted" stdDeviation="2.5" result="lt-blurred" />
            <feComposite in="lt-blurred" in2="SourceAlpha" operator="out" result="lt-inward" />
            <feComposite in="lt-inward" in2="SourceAlpha" operator="in" result="lt-masked" />
            <feFlood floodColor="#ffffff" floodOpacity="0.82" result="lt-color" />
            <feComposite in="lt-color" in2="lt-masked" operator="in" result="light-highlight" />

            <feMorphology in="SourceAlpha" operator="erode" radius="4" result="eroded" />
            <feComposite in="SourceAlpha" in2="eroded" operator="out" result="edge-band" />
            <feGaussianBlur in="edge-band" stdDeviation="3.5" result="edge-blurred" />
            <feComposite in="edge-blurred" in2="SourceAlpha" operator="in" result="edge-masked" />
            <feFlood floodColor="#8e8c88" floodOpacity="0.38" result="edge-color" />
            <feComposite in="edge-color" in2="edge-masked" operator="in" result="ambient-depth" />

            <feFlood floodColor="#D8D6D1" result="flat-base" />
            <feComposite in="flat-base" in2="SourceAlpha" operator="in" result="base-fill" />

            <feMerge>
              <feMergeNode in="base-fill" />
              <feMergeNode in="ambient-depth" />
              <feMergeNode in="dark-shadow" />
              <feMergeNode in="light-highlight" />
            </feMerge>
          </filter>
        </defs>

        <circle cx="90" cy="90" r="80" fill={BASE} />
        {glyphImage ? (
          <image
            href={glyphImage}
            x={glyphImageBox.x}
            y={glyphImageBox.y}
            width={glyphImageBox.w}
            height={glyphImageBox.h}
            preserveAspectRatio="xMidYMid meet"
            filter={`url(#${filterId})`}
          />
        ) : (
          <path d={glyphPath} fill={BASE} filter={`url(#${filterId})`} />
        )}
      </svg>
    </div>
  )
}

/** GPS / qibla direction arrow */
export function NeuNavDirection({ size = 180 }: { size?: number }) {
  return (
    <NeuNavIcon
      size={size}
      filterId="concave-direction"
      ariaLabel="Direction"
      glyphPath="M 90 38 L 130 126 L 90 108 L 50 126 Z"
    />
  )
}

/** Mosque dome silhouette */
export function NeuNavPrayer({ size = 180 }: { size?: number }) {
  return (
    <NeuNavIcon
      size={size}
      filterId="concave-prayer"
      ariaLabel="Prayer times"
      glyphPath="M 90 40 L 122 72 L 122 118 L 104 118 L 104 136 L 76 136 L 76 118 L 58 118 L 58 72 Z"
    />
  )
}

/**
 * Hands + tasbih — raster glyph on milk disc (no concave filter; filter flattens detail).
 */
export function NeuNavDua({ size = 180 }: { size?: number }) {
  const shadowScale = size / 180
  const dropLight = `${-5 * shadowScale}px ${-5 * shadowScale}px ${10 * shadowScale}px ${SHADOW_LIGHT}`
  const dropDark = `${5 * shadowScale}px ${5 * shadowScale}px ${13 * shadowScale}px ${SHADOW_DARK}`

  return (
    <div
      role="img"
      aria-label="Dua"
      style={{
        display: "inline-block",
        borderRadius: "50%",
        filter: `drop-shadow(${dropLight}) drop-shadow(${dropDark})`,
      }}
    >
      <svg
        width={size}
        height={size}
        viewBox="0 0 180 180"
        xmlns="http://www.w3.org/2000/svg"
        style={{ display: "block" }}
      >
        <circle cx="90" cy="90" r="80" fill={BASE} />
        <image
          href="/nav-dua-glyph.png"
          x={18}
          y={22}
          width={144}
          height={144}
          preserveAspectRatio="xMidYMid meet"
        />
      </svg>
    </div>
  )
}

/** Gear cog */
export function NeuNavSettings({ size = 180 }: { size?: number }) {
  return (
    <NeuNavIcon
      size={size}
      filterId="concave-settings"
      ariaLabel="Settings"
      glyphPath="M 90 50 L 100 54 L 110 52 L 114 62 L 124 66 L 124 76 L 132 82 L 128 92 L 132 102 L 124 108 L 124 118 L 114 122 L 110 132 L 100 130 L 90 134 L 80 130 L 70 132 L 66 122 L 56 118 L 56 108 L 48 102 L 52 92 L 48 82 L 56 76 L 56 66 L 66 62 L 70 52 L 80 54 Z"
    />
  )
}
