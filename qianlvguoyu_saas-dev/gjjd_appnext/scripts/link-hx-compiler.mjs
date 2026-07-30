import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { resolveHBuilderX, root } from './hx-uni-env.mjs'

const hx = resolveHBuilderX()
const hxNm = path.join(hx.cliViteRoot, 'node_modules')
const projNm = path.join(root, 'node_modules')

const links = [
  'vite',
  'vue',
  'pinia',
  path.join('@dcloudio', 'vite-plugin-uni'),
  path.join('@dcloudio', 'uni-cli-shared'),
  path.join('@dcloudio', 'uni-components'),
  path.join('@vitejs', 'plugin-vue'),
  path.join('@vue', 'shared'),
  path.join('@vue', 'runtime-core'),
  path.join('@vue', 'runtime-dom'),
  path.join('@vue', 'reactivity'),
  path.join('@vue', 'compiler-core'),
  path.join('@vue', 'compiler-dom'),
  path.join('@vue', 'compiler-sfc'),
  path.join('@vue', 'server-renderer'),
]

function ensureJunction(name) {
  const dest = path.join(projNm, name)
  const target = path.join(hxNm, name)
  if (!fs.existsSync(target)) {
    console.warn(`[skip] missing HX package: ${target}`)
    return
  }
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  if (fs.existsSync(dest)) {
    try {
      fs.rmSync(dest, { recursive: true, force: true })
    } catch {
      spawnSync('cmd', ['/c', 'rmdir', dest], { stdio: 'ignore' })
    }
  }
  const result = spawnSync('cmd', ['/c', 'mklink', '/J', dest, target], {
    encoding: 'utf8',
  })
  if (result.status === 0) {
    console.log(`[ok] ${name}`)
  } else {
    console.error(`[fail] ${name}: ${result.stderr || result.stdout}`)
  }
}

fs.mkdirSync(projNm, { recursive: true })
for (const name of links) ensureJunction(name)
console.log('HBuilderX compiler junctions ready.')
