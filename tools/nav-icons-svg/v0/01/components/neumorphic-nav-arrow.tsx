"use client"

import { cn } from "@/lib/utils"

interface NeumorphicNavArrowProps {
  /** Diameter of the icon in pixels. Everything scales from this value. */
  size?: number
  /** Rotation of the arrow in degrees (0 = pointing up). */
  heading?: number
  className?: string
}

const BASE = "#F0EEE9" // Cloud Dancer
const LIGHT = "#ffffff" // top-left highlight
const DARK = "#cccaca" // bottom-right shadow (outer base)
const INSET_DARK = "#b8b5af" // deeper shadow for the stamped arrow

/**
 * A scalable neumorphic GPS / cursor style navigation arrow icon.
 *
 * Driven by a single `--size` CSS variable so the circle, arrow and every
 * shadow scale proportionally. The outer circle is convex (raised) while the
 * inner arrow is concave (stamped in) using inset shadows. No borders — only
 * soft light/dark shadows derived from the #F0EEE9 base.
 */
export function NeumorphicNavArrow({ size = 200, heading = 0, className }: NeumorphicNavArrowProps) {
  return (
    <div
      role="img"
      aria-label="Navigation arrow"
      className={cn("group relative select-none", className)}
      style={
        {
          "--size": `${size}px`,
          width: "var(--size)",
          height: "var(--size)",
        } as React.CSSProperties
      }
    >
      {/* Outer base — convex: white highlight top-left, soft dark shadow bottom-right */}
      <div
        className="relative h-full w-full rounded-full transition-shadow duration-500 ease-out"
        style={{
          backgroundColor: BASE,
          boxShadow: `
            calc(var(--size) * -0.06) calc(var(--size) * -0.06) calc(var(--size) * 0.12) ${LIGHT},
            calc(var(--size) * 0.06) calc(var(--size) * 0.06) calc(var(--size) * 0.12) ${DARK}
          `,
        }}
      >
        {/* Arrow assembly — rotates smoothly on hover */}
        <div
          className="absolute left-1/2 top-1/2 transition-transform duration-500 ease-out group-hover:[transform:translate(-50%,-50%)_rotate(var(--arrow-hover))]"
          style={
            {
              width: "calc(var(--size) * 0.46)",
              height: "calc(var(--size) * 0.46)",
              transform: `translate(-50%, -50%) rotate(${heading}deg)`,
              "--arrow-hover": `${heading + 14}deg`,
            } as React.CSSProperties
          }
        >
          {/* Concave navigation arrow — inset dark top-left, inset white bottom-right */}
          <div
            className="h-full w-full transition-[box-shadow] duration-500 ease-out"
            style={{
              backgroundColor: BASE,
              // Classic GPS / cursor navigation arrow pointing up
              clipPath: "polygon(50% 0%, 100% 100%, 50% 78%, 0% 100%)",
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
