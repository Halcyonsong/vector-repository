import { getAppConfig } from './config/app-config'
import type { AppErrorState, ErrorKind } from './types'

const VALIDATION_KEYWORDS = [
  '参数',
  '校验',
  '不能为空',
  '缺少',
  '格式错误',
  '请输入',
  '仅支持',
  'topk',
  'similaritythreshold',
  'knowledgebaseid'
]

const DOCUMENT_KEYWORDS = [
  '文档解析',
  '未解析到',
  '可向量化',
  '可用文本',
  '图片型pdf',
  '损坏内容'
]

function normalizeMessage(message: string): string {
  return message.trim().toLowerCase()
}

export function inferErrorKind(message: string): ErrorKind {
  const normalizedMessage = normalizeMessage(message)
  const isDocumentError = DOCUMENT_KEYWORDS.some((keyword) => {
    return normalizedMessage.includes(keyword)
  })

  if (isDocumentError) {
    return 'document'
  }

  const isValidationError = VALIDATION_KEYWORDS.some((keyword) => {
    return normalizedMessage.includes(keyword)
  })

  if (isValidationError) {
    return 'validation'
  }

  return 'system'
}

export function buildErrorState(message: string, kind?: ErrorKind): AppErrorState {
  const resolvedKind = kind ?? inferErrorKind(message)
  const feedbackText = getAppConfig().labels.feedback

  if (resolvedKind === 'validation') {
    return {
      kind: resolvedKind,
      title: feedbackText.validationTitle,
      message
    }
  }

  if (resolvedKind === 'interrupted') {
    return {
      kind: resolvedKind,
      title: feedbackText.interruptedTitle,
      message
    }
  }

  if (resolvedKind === 'document') {
    return {
      kind: resolvedKind,
      title: feedbackText.documentTitle,
      message
    }
  }

  return {
    kind: resolvedKind,
    title: feedbackText.systemTitle,
    message
  }
}
