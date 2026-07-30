import fs from 'node:fs'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { applyUniEnv, root } from './hx-uni-env.mjs'

const outputDir = path.join(root, 'unpackage', 'dist', 'dev', 'web')
const hx = applyUniEnv({ platform: 'h5', outputDir })
const nodeExe = fs.existsSync(hx.nodeExe) ? hx.nodeExe : process.execPath

const child = spawn(nodeExe, [hx.uniJs, '-p', 'h5'], {
  cwd: root,
  stdio: 'inherit',
  env: process.env,
})

child.on('exit', (code) => process.exit(code ?? 1))
