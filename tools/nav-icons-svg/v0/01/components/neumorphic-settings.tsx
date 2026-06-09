"use client"

import { NeumorphicStampIcon } from "@/components/neumorphic-stamp-icon"

/** 8-tooth gear (stamped). */
const SETTINGS_CLIP =
  "polygon(50% 0%, 61% 7%, 75% 4%, 79% 18%, 93% 25%, 89% 39%, 100% 50%, 89% 61%, 93% 75%, 79% 82%, 75% 96%, 61% 93%, 50% 100%, 39% 93%, 25% 96%, 21% 82%, 7% 75%, 11% 61%, 0% 50%, 11% 39%, 7% 25%, 21% 18%, 25% 4%, 39% 7%)"

export function NeumorphicSettings({ size = 200 }: { size?: number }) {
  return (
    <NeumorphicStampIcon
      size={size}
      clipPath={SETTINGS_CLIP}
      ariaLabel="Settings"
    />
  )
}
