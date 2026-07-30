import path from 'node:path'
import { spawn } from 'node:child_process'
import fs from 'node:fs'
import { applyUniEnv, root } from './hx-uni-env.mjs'

const outputDir = path.join(root, 'dist')
const hx = applyUniEnv({ platform: 'h5', outputDir })
process.env.NODE_ENV = 'production'
const nodeExe = fs.existsSync(hx.nodeExe) ? hx.nodeExe : process.execPath

const child = spawn(nodeExe, [hx.uniJs, 'build', '-p', 'h5'], {
  cwd: root,
  stdio: 'inherit',
  env: process.env,
})

child.on('exit', (code) => process.exit(code ?? 1))
