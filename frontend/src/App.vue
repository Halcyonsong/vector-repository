<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ChatView from './components/ChatView.vue'
import KnowledgeView from './components/KnowledgeView.vue'
import SidebarPanel from './components/SidebarPanel.vue'
import { buildErrorState } from './error-state'
import { getErrorKind } from './api'
import { useChat } from './composables/useChat'
import { useKnowledgeUpload } from './composables/useKnowledgeUpload'
import { useSessions } from './composables/useSessions'
import { DEFAULT_APP_CONFIG, provideAppConfig } from './config/app-config'
import { VIEW_MODE } from './constants'
import type { AppErrorState, ErrorKind, ViewMode } from './types'

const appConfig = provideAppConfig(DEFAULT_APP_CONFIG)
const currentView = ref<ViewMode>(VIEW_MODE.chat)
const knowledgeBaseId = ref(appConfig.behavior.defaultKnowledgeBaseId)
const errorState = ref<AppErrorState | null>(null)
const historyScrollKey = ref(0)
const errorTitle = computed(() => errorState.value?.title ?? '')
const errorMessage = computed(() => errorState.value?.message ?? '')

function setError(message: string, kind?: ErrorKind): void {
  errorState.value = buildErrorState(message, kind)
}

function clearError(): void {
  errorState.value = null
}

const {
  activeSessionId,
  sessionGroups,
  activeSessionTitle,
  loadSessions,
  createAndSelectSession,
  selectSession,
  renameExistingSession,
  deleteExistingSession
} = useSessions({
  setError
})

const {
  question,
  displayMessages,
  useKnowledgeBase,
  allowEmptyContext,
  topKInput,
  similarityThresholdInput,
  reasoningText,
  statusText,
  isStreaming,
  hasConversation,
  startupElapsedText,
  reasoningElapsedText,
  answerElapsedText,
  showStartupTimer,
  showReasoningTimer,
  showAnswerTimer,
  hasMoreHistory,
  isLoadingEarlierHistory,
  loadHistory,
  loadEarlierHistory,
  rollbackLatestRound,
  resetConversation,
  send,
  stop
} = useChat({
  knowledgeBaseId,
  getActiveSessionId: () => activeSessionId.value,
  reloadSessions: loadSessions,
  setError,
  clearError
})

const {
  knowledgeBases,
  selectedFileName,
  uploadStatusText,
  activeUploadTask,
  uploadProgressPercent,
  uploadProgressText,
  isKnowledgeBaseListLoading,
  isUploading,
  deletingKnowledgeBaseId,
  selectFile,
  selectDroppedFile,
  loadKnowledgeBases,
  uploadSelectedDocument,
  deleteExistingKnowledgeBase
} = useKnowledgeUpload({
  knowledgeBaseId,
  setError,
  clearError
})

async function handleCreateSession(): Promise<void> {
  try {
    await createAndSelectSession()
    currentView.value = VIEW_MODE.chat
    resetConversation()
    clearError()
    historyScrollKey.value += 1
  } catch (error) {
    setError(error instanceof Error ? error.message : appConfig.labels.ui.sessionCreateRequired, getErrorKind(error))
  }
}

async function handleSelectSession(sessionId: string): Promise<void> {
  try {
    await selectSession(sessionId)
    await loadHistory(sessionId)
    historyScrollKey.value += 1
  } catch (error) {
    setError(error instanceof Error ? error.message : appConfig.labels.ui.sessionHistoryFailed, getErrorKind(error))
  }
}

async function handleRenameSession(sessionId: string): Promise<void> {
  await renameExistingSession(sessionId)
}

async function handleDeleteSession(sessionId: string): Promise<void> {
  const hasNoActiveSession = await deleteExistingSession(sessionId)
  if (hasNoActiveSession) {
    resetConversation()
    return
  }

  await loadHistory(activeSessionId.value)
  historyScrollKey.value += 1
}

async function handleRollbackLatestRound(restoreQuestion?: string): Promise<void> {
  await rollbackLatestRound(restoreQuestion)
  historyScrollKey.value += 1
}

async function handleSend(): Promise<void> {
  if (!question.value.trim()) {
    await send()
    return
  }

  if (!activeSessionId.value) {
    try {
      await createAndSelectSession()
      currentView.value = VIEW_MODE.chat
      resetConversation()
      historyScrollKey.value += 1
    } catch (error) {
      setError(error instanceof Error ? error.message : appConfig.labels.ui.sessionCreateRequired, getErrorKind(error))
      return
    }
  }

  await send()
}

onMounted(async () => {
  try {
    await loadSessions()
    await loadHistory(activeSessionId.value)
    historyScrollKey.value += 1
    await loadKnowledgeBases()
  } catch (error) {
    setError(error instanceof Error ? error.message : appConfig.labels.ui.initFailed, getErrorKind(error))
  }
})
</script>

<template>
  <div class="app-shell">
    <SidebarPanel
      :current-view="currentView"
      :active-session-id="activeSessionId"
      :session-groups="sessionGroups"
      @change-view="currentView = $event"
      @select-session="handleSelectSession($event)"
      @create-session="handleCreateSession"
      @rename-session="handleRenameSession($event)"
      @delete-session="handleDeleteSession($event)"
    />

    <main class="workspace-main">
      <Transition name="view-slide" mode="out-in">
        <ChatView
          v-if="currentView === VIEW_MODE.chat"
          key="chat-view"
          :active-session-title="activeSessionTitle"
          :active-session-display-id="activeSessionId"
          :question="question"
          :status-text="statusText"
          :messages="displayMessages"
          :history-scroll-key="historyScrollKey"
          :has-more-history="hasMoreHistory"
          :is-loading-earlier-history="isLoadingEarlierHistory"
          :reasoning-text="reasoningText"
          :has-conversation="hasConversation"
          :use-knowledge-base="useKnowledgeBase"
          :allow-empty-context="allowEmptyContext"
          :knowledge-base-id="knowledgeBaseId"
          :knowledge-bases="knowledgeBases"
          :top-k-input="topKInput"
          :similarity-threshold-input="similarityThresholdInput"
          :is-streaming="isStreaming"
          :has-output-error="errorState !== null"
          :error-title="errorTitle"
          :error-message="errorMessage"
          :startup-elapsed-text="startupElapsedText"
          :reasoning-elapsed-text="reasoningElapsedText"
          :answer-elapsed-text="answerElapsedText"
          :show-startup-timer="showStartupTimer"
          :show-reasoning-timer="showReasoningTimer"
          :show-answer-timer="showAnswerTimer"
          @update-question="question = $event"
          @update-use-knowledge-base="useKnowledgeBase = $event"
          @update-allow-empty-context="allowEmptyContext = $event"
          @update-knowledge-base-id="knowledgeBaseId = $event"
          @update-top-k-input="topKInput = $event"
          @update-similarity-threshold-input="similarityThresholdInput = $event"
          @load-earlier-history="loadEarlierHistory"
          @rollback-latest-round="handleRollbackLatestRound()"
          @undo-latest-round="handleRollbackLatestRound($event)"
          @send="handleSend"
          @stop="stop"
        />

        <KnowledgeView
          v-else
          key="knowledge-view"
          :knowledge-base-id="knowledgeBaseId"
          :knowledge-bases="knowledgeBases"
          :selected-file-name="selectedFileName"
          :upload-status-text="uploadStatusText"
          :active-upload-task="activeUploadTask"
          :upload-progress-percent="uploadProgressPercent"
          :upload-progress-text="uploadProgressText"
          :is-knowledge-base-list-loading="isKnowledgeBaseListLoading"
          :is-uploading="isUploading"
          :deleting-knowledge-base-id="deletingKnowledgeBaseId"
          @update-knowledge-base-id="knowledgeBaseId = $event"
          @select-file="selectFile"
          @select-dropped-file="selectDroppedFile"
          @upload="uploadSelectedDocument"
          @delete-knowledge-base="deleteExistingKnowledgeBase"
          @refresh-knowledge-bases="loadKnowledgeBases"
          @back-to-chat="currentView = VIEW_MODE.chat"
        />
      </Transition>
    </main>
  </div>
</template>
