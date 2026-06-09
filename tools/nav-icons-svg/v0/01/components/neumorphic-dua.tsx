"use client"

import { NeumorphicStampIcon } from "@/components/neumorphic-stamp-icon"

/** Open book / dua pages (stamped). */
const DUA_CLIP = "polygon(20% 24%, 50% 14%, 80% 24%, 80% 80%, 50% 90%, 20% 80%)"

export function NeumorphicDua({ size = 200 }: { size?: number }) {
  return (
    <NeumorphicStampIcon size={size} clipPath={DUA_CLIP} ariaLabel="Dua" />
  )
}
