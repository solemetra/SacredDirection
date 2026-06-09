"use client"

import { cn } from "@/lib/utils"

interface NeumorphicCompassProps {
  /** Diameter of the compass in pixels. Everything scales from this value. */
  size?: number
  /** Heading the needle points to, in degrees (0 = North). Defaults to 45 (North-East). */
  heading?: number
  className?: string
}

/**
 * A scalable monochrome neumorphic compass icon.
 *
 * Built on a single `--size` CSS variable so every shadow, marker and the
 * needle scale proportionally. Colors are a strict light-gray palette derived
 * from the #E0E5EC base — no borders, only soft light/dark shadows.
 */
export function NeumorphicCompass({ size = 200, heading = 45, className }: NeumorphicCompassProps) {
  return (
    <div
      role="img"
      aria-label="Compass"
      className={cn("group relative select-none", className)}
      style={
        {
          "--size": `${size}px`,
          width: "var(--size)",
          height: "var(--size)",
        } as React.CSSProperties
      }
    >
      {/* Outer dial — convex: white highlight top-left, dark shadow bottom-right */}
      <div
        className="relative h-full w-full rounded-full"
        style={{
          backgroundColor: "#E0E5EC",
          boxShadow: `
            calc(var(--size) * -0.06) calc(var(--size) * -0.06) calc(var(--size) * 0.12) #ffffff,
            calc(var(--size) * 0.06) calc(var(--size) * 0.06) calc(var(--size) * 0.12) #a3b1c6
          `,
        }}
      >
        {/* Inner recessed face — subtle pressed-in plate that holds the markers */}
        <div
          className="absolute rounded-full"
          style={{
            inset: "calc(var(--size) * 0.08)",
            backgroundColor: "#E0E5EC",
            boxShadow: `
              inset calc(var(--size) * 0.035) calc(var(--size) * 0.035) calc(var(--size) * 0.07) #a3b1c6,
              inset calc(var(--size) * -0.035) calc(var(--size) * -0.035) calc(var(--size) * 0.07) #ffffff
            `,
          }}
        >
          {/* Engraved directional markers */}
          <Marker label="N" className="left-1/2 -translate-x-1/2" style={{ top: "calc(var(--size) * 0.05)" }} />
          <Marker label="S" className="left-1/2 -translate-x-1/2" style={{ bottom: "calc(var(--size) * 0.05)" }} />
          <Marker label="E" className="top-1/2 -translate-y-1/2" style={{ right: "calc(var(--size) * 0.07)" }} />
          <Marker label="W" className="top-1/2 -translate-y-1/2" style={{ left: "calc(var(--size) * 0.07)" }} />

          {/* Needle assembly — rotates smoothly toward heading, nudges on hover */}
          <div
            className="absolute left-1/2 top-1/2 transition-transform duration-700 ease-out group-hover:[transform:translate(-50%,-50%)_rotate(var(--needle-hover))]"
            style={
              {
                width: "calc(var(--size) * 0.16)",
                height: "calc(var(--size) * 0.52)",
                transform: `translate(-50%, -50%) rotate(${heading}deg)`,
                "--needle-hover": `${heading + 12}deg`,
              } as React.CSSProperties
            }
          >
            {/* North half — raised / convex */}
            <div
              className="absolute left-0 top-0 h-1/2 w-full"
              style={{
                backgroundColor: "#E0E5EC",
                clipPath: "polygon(50% 0%, 100% 100%, 0% 100%)",
                boxShadow: `
                  inset calc(var(--size) * -0.012) calc(var(--size) * -0.012) calc(var(--size) * 0.02) #a3b1c6,
                  inset calc(var(--size) * 0.012) calc(var(--size) * 0.012) calc(var(--size) * 0.02) #ffffff
                `,
              }}
            />
            {/* South half — pressed / concave */}
            <div
              className="absolute bottom-0 left-0 h-1/2 w-full"
              style={{
                backgroundColor: "#E0E5EC",
                clipPath: "polygon(0% 0%, 100% 0%, 50% 100%)",
                boxShadow: `
                  inset calc(var(--size) * 0.012) calc(var(--size) * 0.012) calc(var(--size) * 0.02) #a3b1c6,
                  inset calc(var(--size) * -0.012) calc(var(--size) * -0.012) calc(var(--size) * 0.02) #ffffff
                `,
              }}
            />
          </div>

          {/* Raised pivot dot — dead center */}
          <div
            className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full"
            style={{
              width: "calc(var(--size) * 0.09)",
              height: "calc(var(--size) * 0.09)",
              backgroundColor: "#E0E5EC",
              boxShadow: `
                calc(var(--size) * -0.012) calc(var(--size) * -0.012) calc(var(--size) * 0.02) #ffffff,
                calc(var(--size) * 0.012) calc(var(--size) * 0.012) calc(var(--size) * 0.02) #a3b1c6
              `,
            }}
          />
        </div>
      </div>
    </div>
  )
}

function Marker({
  label,
  className,
  style,
}: {
  label: string
  className?: string
  style?: React.CSSProperties
}) {
  return (
    <span
      className={cn("absolute font-semibold leading-none", className)}
      style={{
        fontSize: "calc(var(--size) * 0.1)",
        color: "#c5ccd6",
        // Engraved effect: light below, dark above
        textShadow: "0.5px 0.5px 0.5px #ffffff, -0.5px -0.5px 0.5px #a3b1c6",
        ...style,
      }}
    >
      {label}
    </span>
  )
}
