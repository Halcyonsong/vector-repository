import { computed, ref, type Ref } from 'vue'
import { getErrorKind, listSessionHistory, stopChat, streamChat } from '../api'
import { useAppConfig } from '../config/app-config'
import { CHAT_STATUS } from '../constants'
import { useChatTimers } from './useChatTimers'
import { useReasoningCache } from './useReasoningCache'
import {
  CHAT_EVENT,
  type ChatEvent,
  type ChatHistoryMessageVO,
  type ChatStatus,
  type ErrorKind
} from '../types'
import {
  buildSimilarityThresholdErrorMessage,
  buildTopKErrorMessage,
  resolveSimilarityThreshold,
  resolveTopK
} from '../utils'

interface UseChatOptions {
  knowledgeBaseId: Ref<string>
  getActiveSessionId: () => string
  reloadSessions: () => Promise<void>
  setError: (message: string, kind?: ErrorKind) => void
  clearError: () => void
}

export function useChat(options: UseChatOptions) {
  const appConfig = useAppConfig()
  const behaviorConfig = appConfig.behavior
  const uiText = appConfig.labels.ui
  const timers = useChatTimers()
  const reasoningCache = useReasoningCache({
    cacheKey: behaviorConfig.reasoningCacheKey,
    ttlMs: behaviorConfig.reasoningCacheTtlMs
  })

  const question = ref('')
  const historyMessages = ref<ChatHistoryMessageVO[]>([])
  const lastSubmittedQuestion = ref('')
  const useKnowledgeBase = ref(false)
  const topKInput = ref('')
  const similarityThresholdInput = ref('')
  const reasoningText = ref('')
  const answerText = ref('')
  const statusText = ref<ChatStatus>(CHAT_STATUS.idle)
  const isStreaming = ref(false)

  const displayMessages = computed<ChatHistoryMessageVO[]>(() => {
    const messages = [...historyMessages.value]

    if (lastSubmittedQuestion.value) {
      messages.push({
        role: 'user',
        content: lastSubmittedQuestion.value,
        status: 'completed',
        createTime: new Date().toISOString()
      })
    }

    if (lastSubmittedQuestion.value || answerText.value || isStreaming.value) {
      messages.push({
        role: 'assistant',
        content: answerText.value,
        status: statusText.value === CHAT_STATUS.error ? 'error' : statusText.value,
        createTime: new Date().toISOString()
      })
    }

    return messages
  })

  const hasConversation = computed(() => {
    return displayMessages.value.length > 0 || reasoningText.value.length > 0
  })

  function resetTransientConversation(shouldResetTimers = true): void {
    reasoningText.value = ''
    answerText.value = ''
    lastSubmittedQuestion.value = ''
    statusText.value = CHAT_STATUS.idle
    if (shouldResetTimers) {
      timers.resetTimers()
    }
  }

  function resetConversation(): void {
    historyMessages.value = []
    reasoningCache.clear()
    resetTransientConversation()
  }

  function finishWithStatus(status: ChatStatus): void {
    isStreaming.value = false
    statusText.value = status
    timers.finishActiveTimers()
  }

  async function loadHistory(sessionId?: string, preserveTimers = false): Promise<void> {
    const resolvedSessionId = sessionId ?? options.getActiveSessionId()
    if (!resolvedSessionId) {
      historyMessages.value = []
      return
    }

    historyMessages.value = await listSessionHistory(resolvedSessionId)
    const preservedReasoningText = preserveTimers ? reasoningText.value : ''
    resetTransientConversation(!preserveTimers)
    if (preserveTimers) {
      reasoningText.value = preservedReasoningText
      return
    }
    reasoningText.value = reasoningCache.read(resolvedSessionId)
  }

  function appendReasoning(eventData: string): void {
    if (eventData && !reasoningText.value) {
      timers.markReasoningStarted(Date.now())
    }

    reasoningText.value += eventData
    reasoningCache.write(options.getActiveSessionId(), reasoningText.value)
  }

  function appendAnswer(eventData: string): void {
    if (eventData && !answerText.value) {
      timers.markAnswerStarted(Date.now())
    }

    answerText.value += eventData
  }

  function appendEvent(event: ChatEvent): void {
    if (event.eventType === CHAT_EVENT.REASONING) {
      appendReasoning(event.eventData)
      return
    }

    if (event.eventType === CHAT_EVENT.DATA) {
      appendAnswer(event.eventData)
      return
    }

    if (event.eventType === CHAT_EVENT.ERROR) {
      options.setError(event.eventData || uiText.chatFailed)
      finishWithStatus(CHAT_STATUS.error)
      return
    }

    if (event.eventType === CHAT_EVENT.INTERRUPTED) {
      options.setError(uiText.chatInterrupted, 'interrupted')
      finishWithStatus(CHAT_STATUS.interrupted)
      return
    }

    if (event.eventType === CHAT_EVENT.STOP) {
      finishWithStatus(CHAT_STATUS.completed)
    }
  }

  function validateBeforeSend(sessionId: string): boolean {
    if (!sessionId) {
      options.setError(uiText.sessionCreateRequired, 'validation')
      return false
    }
    if (!question.value.trim()) {
      options.setError(uiText.questionRequired, 'validation')
      return false
    }
    if (useKnowledgeBase.value && !options.knowledgeBaseId.value.trim()) {
      options.setError(uiText.knowledgeBaseRequiredWhenEnabled, 'validation')
      return false
    }

    const topK = resolveTopK(topKInput.value, behaviorConfig)
    if (topK === null) {
      options.setError(buildTopKErrorMessage(behaviorConfig), 'validation')
      return false
    }

    const similarityThreshold = resolveSimilarityThreshold(similarityThresholdInput.value, behaviorConfig)
    if (similarityThreshold === null) {
      options.setError(buildSimilarityThresholdErrorMessage(behaviorConfig), 'validation')
      return false
    }

    return true
  }

  function prepareStreamingState(): void {
    reasoningText.value = ''
    answerText.value = ''
    lastSubmittedQuestion.value = question.value.trim()
    statusText.value = CHAT_STATUS.streaming
    isStreaming.value = true
    reasoningCache.clear()
    timers.resetTimers()
    timers.markRequestStarted()
  }

  async function send(): Promise<void> {
    options.clearError()

    const sessionId = options.getActiveSessionId()
    if (!validateBeforeSend(sessionId)) {
      return
    }

    const topK = resolveTopK(topKInput.value, behaviorConfig) ?? behaviorConfig.defaultTopK
    const similarityThreshold =
      resolveSimilarityThreshold(similarityThresholdInput.value, behaviorConfig) ?? behaviorConfig.defaultSimilarityThreshold

    prepareStreamingState()

    try {
      await streamChat(
        {
          question: question.value,
          sessionId,
          useKnowledgeBase: useKnowledgeBase.value,
          knowledgeBaseId: options.knowledgeBaseId.value.trim(),
          topK,
          similarityThreshold
        },
        appendEvent
      )
      question.value = ''
      await options.reloadSessions()
      await loadHistory(sessionId, true)
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.chatRequestFailed, getErrorKind(error))
      finishWithStatus(CHAT_STATUS.error)
    }
  }

  async function stop(): Promise<void> {
    const sessionId = options.getActiveSessionId()
    if (!sessionId) {
      return
    }

    try {
      await stopChat(sessionId)
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.stopRequestFailed, getErrorKind(error))
    }
  }

  return {
    question,
    historyMessages,
    displayMessages,
    lastSubmittedQuestion,
    useKnowledgeBase,
    topKInput,
    similarityThresholdInput,
    reasoningText,
    answerText,
    statusText,
    isStreaming,
    hasConversation,
    startupElapsedText: timers.startupElapsedText,
    reasoningElapsedText: timers.reasoningElapsedText,
    answerElapsedText: timers.answerElapsedText,
    showStartupTimer: timers.showStartupTimer,
    showReasoningTimer: timers.showReasoningTimer,
    showAnswerTimer: timers.showAnswerTimer,
    loadHistory,
    resetConversation,
    send,
    stop
  }
}
