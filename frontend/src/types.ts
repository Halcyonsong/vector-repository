export interface SessionVO {
  sessionId: string
  title: string
  createTime: string
  updateTime: string
}

export interface Result<T> {
  code: number
  message: string
  data: T | null
  timestamp?: string
}

export interface ChatRequest {
  question: string
  sessionId: string
  useKnowledgeBase: boolean
  knowledgeBaseId: string
  topK: number
  similarityThreshold: number
}

export interface RenameSessionRequest {
  sessionId: string
  title: string
}

export interface ChatEvent {
  eventData: string
  eventType: number
}

export interface ChatHistoryMessageVO {
  role: 'user' | 'assistant' | string
  content: string
  status: 'completed' | 'interrupted' | 'error' | string
  createTime: string
}

export interface SessionGroup {
  label: string
  items: SessionVO[]
}

export type ViewMode = 'chat' | 'knowledge'

export type ChatStatus = 'idle' | 'streaming' | 'interrupted' | 'completed' | 'error'

export type ErrorKind = 'validation' | 'document' | 'system' | 'interrupted'

export interface AppErrorState {
  kind: ErrorKind
  title: string
  message: string
}

export const CHAT_EVENT = {
  DATA: 1001,
  REASONING: 1002,
  STOP: 1003,
  INTERRUPTED: 1004,
  ERROR: 1005
} as const
