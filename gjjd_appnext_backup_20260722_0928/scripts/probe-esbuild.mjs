import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const probe = path.join(root, 'unpackage', '_probe_app_service.js')
const out = path.join(root, 'unpackage', '_probe_out.js')
const code = `
if (typeof Promise !== 'undefined' && !Promise.prototype.finally) {
  Promise.prototype.finally = function(callback) {
    return this
  }
}
`

fs.mkdirSync(path.dirname(probe), { recursive: true })
fs.writeFileSync(probe, code)
const nodeBuf = fs.readFileSync(probe)
console.log('node_read', nodeBuf.length, Buffer.from(nodeBuf.subarray(0, 8)).toString('hex'))

const esbuild = path.join(
  'D:\\Program Files (x86)\\HBuilder X\\plugins\\uniapp-cli-vite\\node_modules\\@esbuild\\win32-ia32\\esbuild.exe'
)
const r = spawnSync(esbuild, [probe, `--outfile=${out}`], { encoding: 'utf8' })
console.log('esbuild_status', r.status)
console.log('esbuild_stderr', r.stderr?.slice(0, 500))
if (fs.existsSync(out)) {
  const b = fs.readFileSync(out)
  console.log('esbuild_out', b.length, Buffer.from(b.subarray(0, 8)).toString('hex'))
}

// Also test with leading 0x88
const bad = path.join(root, 'unpackage', '_probe_bad.js')
fs.writeFileSync(bad, Buffer.concat([Buffer.from([0x88]), Buffer.from(code)]))
const r2 = spawnSync(esbuild, [bad, `--outfile=${path.join(root, 'unpackage', '_probe_bad_out.js')}`], {
  encoding: 'utf8',
})
console.log('bad_status', r2.status)
console.log('bad_stderr', r2.stderr?.slice(0, 400))
