import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const hxCandidates = [
  process.env.HBUILDERX_PATH,
  'D:\\Program Files (x86)\\HBuilder X',
  'C:\\Program Files (x86)\\HBuilder X',
  'D:\\HBuilderX',
  'C:\\HBuilderX',
].filter(Boolean)

export function resolveHBuilderX() {
  for (const candidate of hxCandidates) {
    const uniJs = path.join(
      candidate,
      'plugins',
      'uniapp-cli-vite',
      'node_modules',
      '@dcloudio',
      'vite-plugin-uni',
      'bin',
      'uni.js'
    )
    if (fs.existsSync(uniJs)) {
      return {
        hxRoot: candidate,
        nodeExe: path.join(candidate, 'plugins', 'node', 'node.exe'),
        uniJs,
        cliViteRoot: path.join(candidate, 'plugins', 'uniapp-cli-vite'),
      }
    }
  }
  throw new Error(
    '未找到 HBuilderX uni 编译器。请设置环境变量 HBUILDERX_PATH，或确认已安装 HBuilderX。'
  )
}

export function applyUniEnv({ platform, outputDir }) {
  const hx = resolveHBuilderX()
  process.env.UNI_INPUT_DIR = root
  process.env.UNI_OUTPUT_DIR = outputDir
  process.env.UNI_PLATFORM = platform
  process.env.UNI_CLI_CONTEXT = hx.cliViteRoot
  process.env.UNI_HBUILDERX_PLUGINS = path.join(hx.hxRoot, 'plugins')
  process.env.VITE_ROOT_DIR = root
  if (!process.env.NODE_ENV) {
    process.env.NODE_ENV = platform === 'h5' && process.argv.includes('build')
      ? 'production'
      : 'development'
  }
  return hx
}

export { root }
