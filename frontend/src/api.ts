import { getAppConfig } from './config/app-config'
import { RESULT_CODE } from './constants'
import type {
  ChatEvent,
  ChatHistoryMessageVO,
  ChatHistoryPageVO,
  ChatRequest,
  ErrorKind,
  KnowledgeBaseUploadTaskVO,
  RenameSessionRequest,
  Result,
  SessionVO
} from './types'

export class ApiError extends Error {
  readonly code?: number
  readonly data: unknown
  readonly kind: ErrorKind

  constructor(message: string, kind: ErrorKind, code?: number, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
    this.kind = kind
  }
}

export function getErrorKind(error: unknown): ErrorKind | undefined {
  return error instanceof ApiError ? error.kind : undefined
}

function getBehaviorConfig() {
  return getAppConfig().behavior
}

function getUiText() {
  return getAppConfig().labels.ui
}

function isResultPayload(value: unknown): value is Result<unknown> {
  if (!value || typeof value !== 'object') {
    return false
  }

  const candidate = value as Record<string, unknown>
  return 'code' in candidate && 'message' in candidate
}

async function readJsonSafely(response: Response): Promise<unknown | null> {
  try {
    return await response.clone().json()
  } catch {
    return null
  }
}

async function readTextSafely(response: Response): Promise<string> {
  try {
    return await response.text()
  } catch {
    return ''
  }
}

function formatPlainErrorMessage(text: string, fallbackMessage: string): string {
  const trimmedText = text.trim()
  if (!trimmedText) {
    return fallbackMessage
  }

  const messageMatch = trimmedText.match(/message\s*=\s*([^,\r\n}]+)/)
  if (messageMatch?.[1]) {
    return messageMatch[1].trim()
  }

  return trimmedText
}

export function normalizeApiErrorMessage(message: string): string {
  return formatPlainErrorMessage(message, message)
}

async function fetchSafely(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(input, init)
  } catch {
    throw new ApiError(getUiText().networkError, 'system')
  }
}

function isSuccessCode(code: number): boolean {
  return code === RESULT_CODE.success || code === RESULT_CODE.noContent
}

function buildHttpErrorMessage(response: Response, fallbackPrefix: string): string {
  return `${fallbackPrefix}${response.status}`
}

function resolveErrorKind(code?: number): ErrorKind {
  if (code === RESULT_CODE.paramError || code === RESULT_CODE.notFound) {
    return 'validation'
  }

  if (code === RESULT_CODE.documentParseError) {
    return 'document'
  }

  return 'system'
}

function formatResultErrorMessage(result: Result<unknown>, fallbackMessage: string): string {
  const message = result.message || fallbackMessage || getUiText().unknownError
  const data = result.data

  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return message
  }

  const details = Object.entries(data as Record<string, unknown>)
    .map(([field, value]) => `${field}: ${String(value)}`)
    .join(', ')

  return details ? `${message}: ${details}` : message
}

function buildResultError(result: Result<unknown>, fallbackMessage: string): ApiError {
  return new ApiError(
    formatResultErrorMessage(result, fallbackMessage),
    resolveErrorKind(result.code),
    result.code,
    result.data
  )
}

async function buildResponseError(response: Response, fallbackPrefix: string): Promise<ApiError> {
  const jsonPayload = await readJsonSafely(response)
  const fallbackMessage = buildHttpErrorMessage(response, fallbackPrefix)

  if (isResultPayload(jsonPayload)) {
    return buildResultError(jsonPayload, fallbackMessage)
  }

  const textPayload = await readTextSafely(response)
  return new ApiError(
    formatPlainErrorMessage(textPayload, fallbackMessage),
    resolveErrorKind(response.status),
    response.status
  )
}

async function parseResult<T>(response: Response): Promise<T> {
  const jsonPayload = await readJsonSafely(response)

  if (isResultPayload(jsonPayload)) {
    if (isSuccessCode(jsonPayload.code)) {
      return (jsonPayload.data ?? null) as T
    }

    throw buildResultError(jsonPayload, buildHttpErrorMessage(response, getUiText().requestFailedPrefix))
  }

  if (!response.ok) {
    const fallbackMessage = buildHttpErrorMessage(response, getUiText().requestFailedPrefix)
    const textPayload = await readTextSafely(response)
    throw new ApiError(formatPlainErrorMessage(textPayload, fallbackMessage), resolveErrorKind(response.status), response.status)
  }

  throw new ApiError(getUiText().invalidResponse, 'system')
}

export async function createSession(): Promise<SessionVO> {
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/create`, {
    method: 'POST'
  })
  return parseResult<SessionVO>(response)
}

export async function listSessions(): Promise<SessionVO[]> {
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/list`)
  const data = await parseResult<SessionVO[]>(response)
  return data ?? []
}

export async function getSession(sessionId: string): Promise<SessionVO | null> {
  const params = new URLSearchParams({ sessionId })
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/get?${params.toString()}`)
  const data = await parseResult<SessionVO | null>(response)
  return data ?? null
}

export async function listSessionHistory(sessionId: string, beforeIndex?: number | null): Promise<ChatHistoryPageVO> {
  const params = new URLSearchParams({ sessionId })
  if (beforeIndex !== undefined && beforeIndex !== null) {
    params.set('beforeIndex', String(beforeIndex))
  }

  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/history?${params.toString()}`)
  const data = await parseResult<ChatHistoryPageVO | ChatHistoryMessageVO[]>(response)
  if (Array.isArray(data)) {
    return {
      records: data,
      nextCursor: null,
      hasMore: false,
      total: data.length
    }
  }

  return data ?? {
    records: [],
    nextCursor: null,
    hasMore: false,
    total: 0
  }
}

export async function rollbackLastRound(sessionId: string): Promise<ChatHistoryPageVO> {
  const params = new URLSearchParams({ sessionId })
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/rollback?${params.toString()}`, {
    method: 'POST'
  })
  const data = await parseResult<ChatHistoryPageVO>(response)
  return data ?? {
    records: [],
    nextCursor: null,
    hasMore: false,
    total: 0
  }
}

export async function deleteSession(sessionId: string): Promise<boolean> {
  const params = new URLSearchParams({ sessionId })
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/delete?${params.toString()}`, {
    method: 'DELETE'
  })
  const data = await parseResult<boolean>(response)
  return Boolean(data)
}

export async function renameSession(payload: RenameSessionRequest): Promise<SessionVO> {
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/session/rename`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })
  return parseResult<SessionVO>(response)
}

export async function uploadDocument(knowledgeBaseId: string, file: File): Promise<KnowledgeBaseUploadTaskVO> {
  const formData = new FormData()
  formData.append('knowledgeBaseId', knowledgeBaseId)
  formData.append('file', file)

  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/kb/upload`, {
    method: 'POST',
    body: formData
  })

  return parseResult<KnowledgeBaseUploadTaskVO>(response)
}

export async function getKnowledgeBaseUploadTask(taskId: string): Promise<KnowledgeBaseUploadTaskVO> {
  const params = new URLSearchParams({ taskId })
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/kb/task?${params.toString()}`)
  return parseResult<KnowledgeBaseUploadTaskVO>(response)
}

export async function listKnowledgeBases(): Promise<string[]> {
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/kb/list`)
  const data = await parseResult<string[]>(response)
  return data ?? []
}

export async function deleteKnowledgeBase(knowledgeBaseId: string): Promise<void> {
  const params = new URLSearchParams({ knowledgeBaseId })
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/kb/delete?${params.toString()}`, {
    method: 'DELETE'
  })

  await parseResult<void>(response)
}

export async function streamChat(
  payload: ChatRequest,
  onEvent: (event: ChatEvent) => void
): Promise<void> {
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })

  const contentType = response.headers.get('content-type') || ''

  if (!response.ok) {
    throw await buildResponseError(response, getUiText().requestFailedPrefix)
  }

  if (contentType.includes('application/json')) {
    const jsonPayload = await readJsonSafely(response)
    if (isResultPayload(jsonPayload)) {
      if (isSuccessCode(jsonPayload.code)) {
        return
      }

      throw buildResultError(jsonPayload, buildHttpErrorMessage(response, getUiText().requestFailedPrefix))
    }

    throw new ApiError(getUiText().invalidResponse, 'system')
  }

  if (!response.body) {
    throw new ApiError(buildHttpErrorMessage(response, getUiText().requestFailedPrefix), 'system')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const result = await reader.read()
    if (result.done) {
      break
    }

    buffer += decoder.decode(result.value, { stream: true })
    const lines = buffer.split(/\r?\n/)
    buffer = lines.pop() ?? ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed.startsWith('data:')) {
        continue
      }

      const jsonText = trimmed.slice(5).trim()
      if (!jsonText) {
        continue
      }

      const event: ChatEvent = JSON.parse(jsonText)
      onEvent(event)
    }
  }

  const last = buffer.trim()
  if (last.startsWith('data:')) {
    const jsonText = last.slice(5).trim()
    if (jsonText) {
      const event: ChatEvent = JSON.parse(jsonText)
      onEvent(event)
    }
  }
}

export async function stopChat(sessionId: string): Promise<void> {
  const params = new URLSearchParams({ sessionId })
  const response = await fetchSafely(`${getBehaviorConfig().apiBaseUrl}/ai/chat/stop?${params.toString()}`, {
    method: 'POST'
  })

  const contentType = response.headers.get('content-type') || ''
  if (!contentType) {
    if (!response.ok) {
      throw new ApiError(
        buildHttpErrorMessage(response, `${getUiText().stopRequestFailed}: `),
        resolveErrorKind(response.status),
        response.status
      )
    }
    return
  }

  if (contentType.includes('application/json')) {
    const jsonPayload = await readJsonSafely(response)
    if (isResultPayload(jsonPayload)) {
      if (isSuccessCode(jsonPayload.code)) {
        return
      }

      throw buildResultError(jsonPayload, buildHttpErrorMessage(response, `${getUiText().stopRequestFailed}: `))
    }
  }

  if (!response.ok) {
    throw await buildResponseError(response, `${getUiText().stopRequestFailed}: `)
  }
}
