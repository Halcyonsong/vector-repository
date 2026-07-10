import { DEFAULT_APP_CONFIG } from './config/app-config'

export const APP_CONFIG = DEFAULT_APP_CONFIG.behavior
export const SESSION_GROUP_LABEL = DEFAULT_APP_CONFIG.labels.sessionGroups
export const SESSION_LIMIT = {
  titleMaxLength: DEFAULT_APP_CONFIG.behavior.sessionTitleMaxLength
} as const
export const FEEDBACK_TEXT = DEFAULT_APP_CONFIG.labels.feedback
export const UI_TEXT = DEFAULT_APP_CONFIG.labels.ui
export const CHAT_VIEW_TEXT = DEFAULT_APP_CONFIG.labels.chat
export const KNOWLEDGE_VIEW_TEXT = DEFAULT_APP_CONFIG.labels.knowledge

export const VIEW_MODE = {
  chat: 'chat',
  knowledge: 'knowledge'
} as const

export const CHAT_STATUS = {
  idle: 'idle',
  streaming: 'streaming',
  interrupted: 'interrupted',
  completed: 'completed',
  error: 'error'
} as const

export const RESULT_CODE = {
  success: 200,
  noContent: 204,
  paramError: 400,
  unauthorized: 401,
  forbidden: 403,
  notFound: 404,
  documentParseError: 422,
  systemError: 500
} as const
