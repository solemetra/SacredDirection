"use client"

import { cn } from "@/lib/utils"

const BASE = "#F6F4EF"
const LIGHT = "#ffffff"
const DARK = "#cccaca"
const INSET_DARK = "#b8b5af"

export interface NeumorphicStampIconProps {
  size?: number
  clipPath: string
  ariaLabel: string
  className?: string
}

/** Convex disc + concave stamped glyph (nav-arrow pattern, milk surface). */
export function NeumorphicStampIcon({
  size = 200,
  clipPath,
  ariaLabel,
  className,
}: NeumorphicStampIconProps) {
  return (
    <div
      role="img"
      aria-label={ariaLabel}
      className={cn("relative select-none", className)}
      style={
        {
          "--size": `${size}px`,
          width: "var(--size)",
          height: "var(--size)",
        } as React.CSSProperties
      }
    >
      <div
        className="relative h-full w-full rounded-full"
        style={{
          backgroundColor: BASE,
          boxShadow: `
            calc(var(--size) * -0.06) calc(var(--size) * -0.06) calc(var(--size) * 0.12) ${LIGHT},
            calc(var(--size) * 0.06) calc(var(--size) * 0.06) calc(var(--size) * 0.12) ${DARK}
          `,
        }}
      >
        <div
          className="absolute left-1/2 top-1/2"
          style={{
            width: "calc(var(--size) * 0.5)",
            height: "calc(var(--size) * 0.5)",
            transform: "translate(-50%, -50%)",
          }}
        >
          <div
            className="h-full w-full"
            style={{
              backgroundColor: BASE,
              clipPath,
              boxShadow: `
                inset calc(var(--size) * 0.045) calc(var(--size) * 0.045) calc(var(--size) * 0.06) ${INSET_DARK},
                inset calc(var(--size) * -0.04) calc(var(--size) * -0.04) calc(var(--size) * 0.055) ${LIGHT}
              `,
            }}
          />
        </div>
      </div>
    </div>
  )
}
