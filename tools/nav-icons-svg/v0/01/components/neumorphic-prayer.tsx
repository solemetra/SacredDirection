"use client"

import { NeumorphicStampIcon } from "@/components/neumorphic-stamp-icon"

/** Mosque dome + minaret silhouette (stamped). */
const PRAYER_CLIP =
  "polygon(50% 8%, 78% 38%, 78% 72%, 62% 72%, 62% 88%, 38% 88%, 38% 72%, 22% 72%, 22% 38%)"

export function NeumorphicPrayer({ size = 200 }: { size?: number }) {
  return (
    <NeumorphicStampIcon
      size={size}
      clipPath={PRAYER_CLIP}
      ariaLabel="Prayer times"
    />
  )
}
