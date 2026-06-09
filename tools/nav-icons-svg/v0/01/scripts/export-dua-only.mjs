import { mkdir } from "node:fs/promises"
import path from "node:path"
import { fileURLToPath } from "node:url"
import { chromium } from "playwright"
import { spawn } from "node:child_process"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const projectRoot = path.resolve(__dirname, "..")
const androidRes = path.resolve(projectRoot, "../../../../app/src/main/res")

const SIZES = [
  { folder: "drawable-mdpi", scale: 1 },
  { folder: "drawable-hdpi", scale: 1.5 },
  { folder: "drawable-xhdpi", scale: 2 },
  { folder: "drawable-xxhdpi", scale: 3 },
  { folder: "drawable-xxxhdpi", scale: 4 },
]
const BASE_PX = 128

function waitForServer(url, timeoutMs = 120000) {
  const start = Date.now()
  return new Promise((resolve, reject) => {
    const tick = async () => {
      try {
        const res = await fetch(url)
        if (res.ok) return resolve()
      } catch {
        /* retry */
      }
      if (Date.now() - start > timeoutMs) {
        reject(new Error(`Server not ready: ${url}`))
        return
      }
      setTimeout(tick, 500)
    }
    tick()
  })
}

async function main() {
  const dev = spawn("npm", ["run", "dev", "--", "--port", "3456"], {
    cwd: projectRoot,
    shell: true,
    stdio: "ignore",
  })

  try {
    await waitForServer("http://localhost:3456/export")
    const browser = await chromium.launch()
    const page = await browser.newPage({
      viewport: { width: 800, height: 400 },
      deviceScaleFactor: 1,
    })

    for (const { folder, scale } of SIZES) {
      const px = Math.round(BASE_PX * (scale / 3))
      await page.setViewportSize({ width: 800, height: 400 })
      await page.goto("http://localhost:3456/export", { waitUntil: "networkidle" })
      await page.addStyleTag({
        content: `
          #nav_dua { width: ${px}px !important; height: ${px}px !important; }
          #nav_dua svg { width: ${px}px !important; height: ${px}px !important; }
        `,
      })
      await page.waitForTimeout(300)
      const outDir = path.join(androidRes, folder)
      await mkdir(outDir, { recursive: true })
      await page.locator("#nav_dua").screenshot({
        path: path.join(outDir, "nav_dua.png"),
      })
    }

    await browser.close()
    console.log("Exported nav_dua to", androidRes)
  } finally {
    dev.kill("SIGTERM")
  }
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
