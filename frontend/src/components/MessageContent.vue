<script setup lang="ts">
import { computed } from 'vue'

type InlineSegmentType = 'text' | 'code' | 'math'
type MessageBlockType = 'paragraph' | 'code' | 'table' | 'math'

interface InlineSegment {
  type: InlineSegmentType
  text: string
}

interface ParagraphBlock {
  type: 'paragraph'
  segments: InlineSegment[]
}

interface CodeBlock {
  type: 'code'
  language: string
  code: string
}

interface TableBlock {
  type: 'table'
  headers: string[]
  rows: string[][]
}

interface MathBlock {
  type: 'math'
  formula: string
}

type MessageBlock = ParagraphBlock | CodeBlock | TableBlock | MathBlock

interface Props {
  content: string
}

const props = defineProps<Props>()

const SUPERSCRIPT_MAP: Record<string, string> = {
  '0': '⁰',
  '1': '¹',
  '2': '²',
  '3': '³',
  '4': '⁴',
  '5': '⁵',
  '6': '⁶',
  '7': '⁷',
  '8': '⁸',
  '9': '⁹',
  '+': '⁺',
  '-': '⁻',
  '=': '⁼',
  '(': '⁽',
  ')': '⁾',
  n: 'ⁿ',
  x: 'ˣ'
}

const SUBSCRIPT_MAP: Record<string, string> = {
  '0': '₀',
  '1': '₁',
  '2': '₂',
  '3': '₃',
  '4': '₄',
  '5': '₅',
  '6': '₆',
  '7': '₇',
  '8': '₈',
  '9': '₉',
  '+': '₊',
  '-': '₋',
  '=': '₌',
  '(': '₍',
  ')': '₎',
  a: 'ₐ',
  e: 'ₑ',
  h: 'ₕ',
  i: 'ᵢ',
  j: 'ⱼ',
  k: 'ₖ',
  l: 'ₗ',
  m: 'ₘ',
  n: 'ₙ',
  o: 'ₒ',
  p: 'ₚ',
  r: 'ᵣ',
  s: 'ₛ',
  t: 'ₜ',
  u: 'ᵤ',
  v: 'ᵥ',
  x: 'ₓ'
}

function isCodeFence(line: string): boolean {
  return line.trim().startsWith('```')
}

function isMathFence(line: string): boolean {
  return line.trim().startsWith('$$')
}

function isTableSeparator(line: string): boolean {
  const normalizedLine = line.trim().replace(/^\|/, '').replace(/\|$/, '')
  const cells = normalizedLine.split('|').map((cell) => cell.trim())
  return cells.length > 1 && cells.every((cell) => /^:?-{3,}:?$/.test(cell))
}

function isTableStart(lines: string[], index: number): boolean {
  const currentLine = lines[index]
  const nextLine = lines[index + 1]
  return Boolean(currentLine?.includes('|') && nextLine && isTableSeparator(nextLine))
}

function parseTableRow(line: string): string[] {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => cell.trim())
}

function toScriptText(value: string, map: Record<string, string>): string {
  return value
    .split('')
    .map((character) => map[character] ?? character)
    .join('')
}

function normalizeLatexGroups(formula: string): string {
  let normalized = formula

  for (let index = 0; index < 4; index += 1) {
    normalized = normalized
      .replace(/\\frac\s*\{([^{}]*)\}\s*\{([^{}]*)\}/g, (_, numerator: string, denominator: string) => {
        return `(${formatMath(numerator)})/(${formatMath(denominator)})`
      })
      .replace(/\\sqrt\s*\{([^{}]*)\}/g, (_, value: string) => `√(${formatMath(value)})`)
      .replace(/\^\{([^{}]*)\}/g, (_, value: string) => toScriptText(formatMath(value), SUPERSCRIPT_MAP))
      .replace(/_\{([^{}]*)\}/g, (_, value: string) => toScriptText(formatMath(value), SUBSCRIPT_MAP))
  }

  return normalized
}

function formatMath(formula: string): string {
  return normalizeLatexGroups(formula)
    .replace(/\\text\s*\{([^{}]*)\}/g, '$1')
    .replace(/\\left|\\right/g, '')
    .replace(/\\quad|\\qquad/g, '  ')
    .replace(/\\,/g, ' ')
    .replace(/\\cdot/g, '·')
    .replace(/\\neq/g, '≠')
    .replace(/\\leq/g, '≤')
    .replace(/\\geq/g, '≥')
    .replace(/\\pm/g, '±')
    .replace(/\\infty/g, '∞')
    .replace(/\\int/g, '∫')
    .replace(/\\ln/g, 'ln')
    .replace(/\\sin/g, 'sin')
    .replace(/\\cos/g, 'cos')
    .replace(/\\tan/g, 'tan')
    .replace(/\\sec/g, 'sec')
    .replace(/\\csc/g, 'csc')
    .replace(/\\cot/g, 'cot')
    .replace(/\\arctan/g, 'arctan')
    .replace(/\\arcsin/g, 'arcsin')
    .replace(/\\arccos/g, 'arccos')
    .replace(/\^([A-Za-z0-9+\-=()])/g, (_, value: string) => toScriptText(value, SUPERSCRIPT_MAP))
    .replace(/_([A-Za-z0-9+\-=()])/g, (_, value: string) => toScriptText(value, SUBSCRIPT_MAP))
    .replace(/[{}]/g, '')
    .replace(/\\/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function parseInlineSegments(text: string): InlineSegment[] {
  const segments: InlineSegment[] = []
  const pattern = /(`[^`]+`|\\\([^)]*\\\)|\$[^$\n]+\$)/g
  let cursor = 0
  let match: RegExpExecArray | null = pattern.exec(text)

  while (match) {
    if (match.index > cursor) {
      segments.push({ type: 'text', text: text.slice(cursor, match.index) })
    }

    const token = match[0]
    if (token.startsWith('`')) {
      segments.push({ type: 'code', text: token.slice(1, -1) })
    } else if (token.startsWith('\\(')) {
      segments.push({ type: 'math', text: formatMath(token.slice(2, -2)) })
    } else {
      segments.push({ type: 'math', text: formatMath(token.slice(1, -1)) })
    }

    cursor = match.index + token.length
    match = pattern.exec(text)
  }

  if (cursor < text.length) {
    segments.push({ type: 'text', text: text.slice(cursor) })
  }

  return segments.length > 0 ? segments : [{ type: 'text', text }]
}

function parseMessage(content: string): MessageBlock[] {
  const lines = content.split(/\r?\n/)
  const blocks: MessageBlock[] = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]

    if (!line.trim()) {
      index += 1
      continue
    }

    if (isCodeFence(line)) {
      const language = line.trim().slice(3).trim()
      const codeLines: string[] = []
      index += 1

      while (index < lines.length && !isCodeFence(lines[index])) {
        codeLines.push(lines[index])
        index += 1
      }

      if (index < lines.length) {
        index += 1
      }

      blocks.push({ type: 'code', language, code: codeLines.join('\n') })
      continue
    }

    if (isMathFence(line)) {
      const firstFormulaPart = line.trim().slice(2)
      const mathLines: string[] = []

      if (firstFormulaPart.endsWith('$$') && firstFormulaPart.length > 2) {
        blocks.push({ type: 'math', formula: formatMath(firstFormulaPart.slice(0, -2).trim()) })
        index += 1
        continue
      }

      if (firstFormulaPart) {
        mathLines.push(firstFormulaPart)
      }
      index += 1

      while (index < lines.length && !lines[index].trim().endsWith('$$')) {
        mathLines.push(lines[index])
        index += 1
      }

      if (index < lines.length) {
        mathLines.push(lines[index].trim().slice(0, -2))
        index += 1
      }

      blocks.push({ type: 'math', formula: formatMath(mathLines.join('\n').trim()) })
      continue
    }

    if (isTableStart(lines, index)) {
      const headers = parseTableRow(lines[index])
      const rows: string[][] = []
      index += 2

      while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
        rows.push(parseTableRow(lines[index]))
        index += 1
      }

      blocks.push({ type: 'table', headers, rows })
      continue
    }

    const paragraphLines: string[] = []
    while (
      index < lines.length &&
      lines[index].trim() &&
      !isCodeFence(lines[index]) &&
      !isMathFence(lines[index]) &&
      !isTableStart(lines, index)
    ) {
      paragraphLines.push(lines[index])
      index += 1
    }

    blocks.push({ type: 'paragraph', segments: parseInlineSegments(paragraphLines.join('\n')) })
  }

  return blocks
}

const blocks = computed(() => {
  return parseMessage(props.content)
})
</script>

<template>
  <div class="message-content">
    <template v-for="(block, blockIndex) in blocks" :key="blockIndex">
      <p v-if="block.type === 'paragraph'" class="content-paragraph">
        <template v-for="(segment, segmentIndex) in block.segments" :key="segmentIndex">
          <code v-if="segment.type === 'code'" class="inline-code">{{ segment.text }}</code>
          <span v-else-if="segment.type === 'math'" class="inline-math">{{ segment.text }}</span>
          <span v-else>{{ segment.text }}</span>
        </template>
      </p>

      <figure v-else-if="block.type === 'code'" class="code-block-wrap">
        <figcaption v-if="block.language" class="code-language">{{ block.language }}</figcaption>
        <pre class="code-block"><code>{{ block.code }}</code></pre>
      </figure>

      <div v-else-if="block.type === 'table'" class="table-wrap">
        <table class="message-table">
          <thead>
            <tr>
              <th v-for="(header, headerIndex) in block.headers" :key="headerIndex">{{ header }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="math-block">{{ block.formula }}</div>
    </template>
  </div>
</template>
