import { NeuNavDirection, NeuNavDua, NeuNavPrayer, NeuNavSettings } from "@/components/neu-nav-icon"

const BG = "#F0EEE9"
const EXPORT_SIZE = 128

const icons = [
  { id: "nav_direction", Component: NeuNavDirection },
  { id: "nav_prayer", Component: NeuNavPrayer },
  { id: "nav_dua", Component: NeuNavDua },
  { id: "nav_settings", Component: NeuNavSettings },
] as const

export default function ExportPage() {
  return (
    <main
      id="export-root"
      style={{
        backgroundColor: BG,
        padding: 24,
        display: "flex",
        flexWrap: "wrap",
        gap: 24,
      }}
    >
      {icons.map(({ id, Component }) => (
        <div
          key={id}
          id={id}
          data-export={id}
          style={{
            width: EXPORT_SIZE,
            height: EXPORT_SIZE,
            backgroundColor: BG,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            overflow: "hidden",
          }}
        >
          <Component size={EXPORT_SIZE} />
        </div>
      ))}
    </main>
  )
}
