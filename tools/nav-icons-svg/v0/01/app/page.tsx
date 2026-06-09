import {
  NeuNavDirection,
  NeuNavDua,
  NeuNavPrayer,
  NeuNavSettings,
} from "@/components/neu-nav-icon"

export default function Home() {
  return (
    <main
      className="flex min-h-screen flex-col items-center justify-center gap-12 p-8"
      style={{ backgroundColor: "#F0EEE9" }}
    >
      <div className="flex flex-wrap items-center justify-center gap-12">
        <NeuNavDirection size={128} />
        <NeuNavPrayer size={128} />
        <NeuNavDua size={128} />
        <NeuNavSettings size={128} />
      </div>
    </main>
  )
}
