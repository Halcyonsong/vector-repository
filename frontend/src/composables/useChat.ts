import { computed, ref, watch, type Ref } from 'vue'
import { getErrorKind, listSessionHistory, rollbackLastRound, stopChat, streamChat } from '../api'
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

interface SessionDraft {
  question: string
  systemPromptInput: string
  updatedAt: number
}

type SessionDraftMap = Record<string, SessionDraft>

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
  const historyNextCursor = ref<number | null>(null)
  const hasMoreHistory = ref(false)
  const isLoadingEarlierHistory = ref(false)
  const lastSubmittedQuestion = ref('')
  const systemPromptInput = ref('')
  const useKnowledgeBase = ref(false)
  const allowEmptyContext = ref(true)
  const topKInput = ref('')
  const similarityThresholdInput = ref('')
  const reasoningText = ref('')
  const answerText = ref('')
  const statusText = ref<ChatStatus>(CHAT_STATUS.idle)
  const isStreaming = ref(false)
  const isStopping = ref(false)
  const activeDraftSessionId = ref('')

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

  function readDraftMap(): SessionDraftMap {
    try {
      const raw = window.localStorage.getItem(behaviorConfig.sessionDraftCacheKey)
      if (!raw) {
        return {}
      }

      const parsed = JSON.parse(raw) as SessionDraftMap
      const now = Date.now()
      return Object.fromEntries(
        Object.entries(parsed).filter(([, draft]) => {
          return now - draft.updatedAt <= behaviorConfig.sessionDraftCacheTtlMs
        })
      )
    } catch {
      return {}
    }
  }

  function writeDraftMap(draftMap: SessionDraftMap): void {
    try {
      window.localStorage.setItem(behaviorConfig.sessionDraftCacheKey, JSON.stringify(draftMap))
    } catch {
      // Draft cache should never block chat usage.
    }
  }

  function persistDraft(sessionId = activeDraftSessionId.value): void {
    if (!sessionId) {
      return
    }

    const draftMap = readDraftMap()
    const nextQuestion = question.value
    const nextSystemPromptInput = systemPromptInput.value

    if (!nextQuestion.trim() && !nextSystemPromptInput.trim()) {
      delete draftMap[sessionId]
      writeDraftMap(draftMap)
      return
    }

    draftMap[sessionId] = {
      question: nextQuestion,
      systemPromptInput: nextSystemPromptInput,
      updatedAt: Date.now()
    }
    writeDraftMap(draftMap)
  }

  function restoreDraft(sessionId: string): void {
    const draft = readDraftMap()[sessionId]
    question.value = draft?.question ?? ''
    systemPromptInput.value = draft?.systemPromptInput ?? ''
  }

  function activateSessionDraft(sessionId: string, shouldPreserveCurrentInput = false): void {
    if (activeDraftSessionId.value && activeDraftSessionId.value !== sessionId) {
      persistDraft(activeDraftSessionId.value)
    }

    activeDraftSessionId.value = sessionId

    if (shouldPreserveCurrentInput) {
      persistDraft(sessionId)
      return
    }

    if (!sessionId) {
      question.value = ''
      systemPromptInput.value = ''
      return
    }

    restoreDraft(sessionId)
  }

  function clearSessionDraft(sessionId: string): void {
    if (!sessionId) {
      return
    }

    const draftMap = readDraftMap()
    delete draftMap[sessionId]
    writeDraftMap(draftMap)
  }

  watch([question, systemPromptInput], () => {
    persistDraft()
  })

  function resetHistoryPaging(): void {
    historyNextCursor.value = null
    hasMoreHistory.value = false
    isLoadingEarlierHistory.value = false
  }

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
    resetHistoryPaging()
    reasoningCache.clear()
    resetTransientConversation()
  }

  function finishWithStatus(status: ChatStatus): void {
    isStreaming.value = false
    isStopping.value = false
    statusText.value = status
    timers.finishActiveTimers()
  }

  async function loadHistory(sessionId?: string, preserveTimers = false): Promise<void> {
    const resolvedSessionId = sessionId ?? options.getActiveSessionId()
    if (!resolvedSessionId) {
      historyMessages.value = []
      resetHistoryPaging()
      return
    }

    const page = await listSessionHistory(resolvedSessionId)
    historyMessages.value = page.records ?? []
    historyNextCursor.value = page.nextCursor
    hasMoreHistory.value = page.hasMore

    const preservedReasoningText = preserveTimers ? reasoningText.value : ''
    resetTransientConversation(!preserveTimers)
    if (preserveTimers) {
      reasoningText.value = preservedReasoningText
      return
    }
    reasoningText.value = reasoningCache.read(resolvedSessionId)
  }

  async function loadEarlierHistory(): Promise<void> {
    const sessionId = options.getActiveSessionId()
    if (!sessionId || !hasMoreHistory.value || isLoadingEarlierHistory.value) {
      return
    }

    isLoadingEarlierHistory.value = true

    try {
      const page = await listSessionHistory(sessionId, historyNextCursor.value)
      historyMessages.value = [...(page.records ?? []), ...historyMessages.value]
      historyNextCursor.value = page.nextCursor
      hasMoreHistory.value = page.hasMore
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.earlierHistoryFailed, getErrorKind(error))
    } finally {
      isLoadingEarlierHistory.value = false
    }
  }

  async function rollbackLatestRound(restoreQuestion?: string): Promise<void> {
    const sessionId = options.getActiveSessionId()
    if (!sessionId || isStreaming.value) {
      return
    }

    options.clearError()

    try {
      const page = await rollbackLastRound(sessionId)
      historyMessages.value = page.records ?? []
      historyNextCursor.value = page.nextCursor
      hasMoreHistory.value = page.hasMore
      resetTransientConversation()
      reasoningCache.clear()
      if (restoreQuestion !== undefined) {
        question.value = restoreQuestion
      }
      await options.reloadSessions()
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.rollbackFailed, getErrorKind(error))
    }
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
    if (!question.value.trim()) {
      options.setError(uiText.questionRequired, 'validation')
      return false
    }
    if (!sessionId) {
      options.setError(uiText.sessionCreateRequired, 'validation')
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
          systemPrompt: systemPromptInput.value.trim(),
          useKnowledgeBase: useKnowledgeBase.value,
          allowEmptyContext: allowEmptyContext.value,
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
    if (!sessionId || !isStreaming.value || isStopping.value) {
      return
    }

    isStopping.value = true

    try {
      await stopChat(sessionId)
    } catch (error) {
      isStopping.value = false
      options.setError(error instanceof Error ? error.message : uiText.stopRequestFailed, getErrorKind(error))
    }
  }

  return {
    question,
    historyMessages,
    historyNextCursor,
    hasMoreHistory,
    isLoadingEarlierHistory,
    displayMessages,
    lastSubmittedQuestion,
    systemPromptInput,
    useKnowledgeBase,
    allowEmptyContext,
    topKInput,
    similarityThresholdInput,
    reasoningText,
    answerText,
    statusText,
    isStreaming,
    isStopping,
    hasConversation,
    startupElapsedText: timers.startupElapsedText,
    reasoningElapsedText: timers.reasoningElapsedText,
    answerElapsedText: timers.answerElapsedText,
    showStartupTimer: timers.showStartupTimer,
    showReasoningTimer: timers.showReasoningTimer,
    showAnswerTimer: timers.showAnswerTimer,
    loadHistory,
    loadEarlierHistory,
    rollbackLatestRound,
    activateSessionDraft,
    clearSessionDraft,
    resetConversation,
    send,
    stop
  }
}
