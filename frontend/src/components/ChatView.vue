<script setup lang="ts">
import { computed } from 'vue'
import { useAppConfig } from '../config/app-config'
import ChatComposer from './ChatComposer.vue'
import ConversationStream from './ConversationStream.vue'
import ReasoningPanel from './ReasoningPanel.vue'
import type { ChatHistoryMessageVO } from '../types'

interface Props {
  activeSessionTitle: string
  activeSessionDisplayId: string
  question: string
  systemPromptInput: string
  statusText: string
  messages: ChatHistoryMessageVO[]
  historyScrollKey: number
  hasMoreHistory: boolean
  isLoadingEarlierHistory: boolean
  reasoningText: string
  hasConversation: boolean
  useKnowledgeBase: boolean
  allowEmptyContext: boolean
  knowledgeBaseId: string
  knowledgeBases: string[]
  topKInput: string
  similarityThresholdInput: string
  isStreaming: boolean
  isStopping: boolean
  hasOutputError: boolean
  errorTitle: string
  errorMessage: string
  startupElapsedText: string
  reasoningElapsedText: string
  answerElapsedText: string
  showStartupTimer: boolean
  showReasoningTimer: boolean
  showAnswerTimer: boolean
}

const props = defineProps<Props>()
const appConfig = useAppConfig()
const behaviorConfig = appConfig.behavior
const chatText = appConfig.labels.chat
const uiText = appConfig.labels.ui

const emit = defineEmits<{
  updateQuestion: [value: string]
  updateSystemPromptInput: [value: string]
  updateUseKnowledgeBase: [value: boolean]
  updateAllowEmptyContext: [value: boolean]
  updateKnowledgeBaseId: [value: string]
  updateTopKInput: [value: string]
  updateSimilarityThresholdInput: [value: string]
  loadEarlierHistory: []
  rollbackLatestRound: []
  undoLatestRound: [question: string]
  send: []
  stop: []
}>()

const knowledgeBaseStatus = computed(() => {
  if (!props.useKnowledgeBase) {
    return chatText.knowledgeDisabled
  }
  return chatText.knowledgeEnabled
})

const allowEmptyContextStatus = computed(() => {
  return props.allowEmptyContext ? chatText.allowEmptyContextEnabled : chatText.allowEmptyContextDisabled
})

const activeTopK = computed(() => {
  return props.topKInput.trim() || String(behaviorConfig.defaultTopK)
})

const activeSimilarityThreshold = computed(() => {
  return props.similarityThresholdInput.trim() || String(behaviorConfig.defaultSimilarityThreshold)
})

const topbarKnowledgeBaseId = computed(() => {
  return props.knowledgeBaseId.trim() || uiText.notConfigured
})

const serviceStatusTitle = computed(() => {
  return props.hasOutputError ? props.errorTitle : chatText.serviceStatusLabel
})

const serviceStatusMessage = computed(() => {
  return props.hasOutputError ? props.errorMessage : chatText.serviceStatusNormal
})
</script>

<template>
  <div class="page-view">
    <section class="topbar-card">
      <div class="topbar-session">
        <span>{{ chatText.currentSessionEyebrow }}</span>
        <strong :title="activeSessionTitle">{{ activeSessionTitle }}</strong>
        <small>{{ activeSessionDisplayId }}</small>
      </div>
      <div class="topbar-inline-item status-item">
        <span>Status</span>
        <strong>{{ statusText }}</strong>
      </div>
      <div :class="['topbar-inline-item service-item', hasOutputError ? 'error' : 'normal']" :title="serviceStatusMessage">
        <span>{{ serviceStatusTitle }}</span>
        <strong>{{ serviceStatusMessage }}</strong>
      </div>
      <div :class="['topbar-inline-item knowledge-item', useKnowledgeBase ? 'active' : '']">
        <span>Knowledge</span>
        <strong>{{ knowledgeBaseStatus }}</strong>
      </div>
      <div :class="['topbar-inline-item forward-item', allowEmptyContext ? 'active' : 'blocked']">
        <span>{{ chatText.allowEmptyContextLabel }}</span>
        <strong>{{ allowEmptyContextStatus }}</strong>
      </div>
      <div class="topbar-inline-item kb-item" :title="topbarKnowledgeBaseId">
        <span>Knowledge Base</span>
        <strong>{{ topbarKnowledgeBaseId }}</strong>
      </div>
      <div class="topbar-inline-item number-item">
        <span>Top K</span>
        <strong>{{ activeTopK }}</strong>
      </div>
      <div class="topbar-inline-item number-item">
        <span>Similarity</span>
        <strong>{{ activeSimilarityThreshold }}</strong>
      </div>
    </section>

    <section class="chat-layout">
      <div class="conversation-panel">
        <div class="conversation-header">
          <div>
            <div class="eyebrow">{{ chatText.sessionEyebrow }}</div>
            <div class="conversation-title">{{ activeSessionDisplayId }}</div>
          </div>
        </div>

        <Transition name="session-slide" mode="out-in">
          <ConversationStream
            :key="activeSessionDisplayId"
            :messages="messages"
            :has-conversation="hasConversation"
            :history-scroll-key="historyScrollKey"
            :has-more-history="hasMoreHistory"
            :is-loading-earlier-history="isLoadingEarlierHistory"
            :is-streaming="isStreaming"
            @load-earlier-history="emit('loadEarlierHistory')"
            @rollback-latest-round="emit('rollbackLatestRound')"
            @undo-latest-round="emit('undoLatestRound', $event)"
          />
        </Transition>

        <ChatComposer
          :question="question"
          :system-prompt-input="systemPromptInput"
          :use-knowledge-base="useKnowledgeBase"
          :allow-empty-context="allowEmptyContext"
          :knowledge-base-id="knowledgeBaseId"
          :knowledge-bases="knowledgeBases"
          :top-k-input="topKInput"
          :similarity-threshold-input="similarityThresholdInput"
          :is-streaming="isStreaming"
          :is-stopping="isStopping"
          :has-output-error="hasOutputError"
          :startup-elapsed-text="startupElapsedText"
          :answer-elapsed-text="answerElapsedText"
          :show-startup-timer="showStartupTimer"
          :show-answer-timer="showAnswerTimer"
          @update-question="emit('updateQuestion', $event)"
          @update-system-prompt-input="emit('updateSystemPromptInput', $event)"
          @update-use-knowledge-base="emit('updateUseKnowledgeBase', $event)"
          @update-allow-empty-context="emit('updateAllowEmptyContext', $event)"
          @update-knowledge-base-id="emit('updateKnowledgeBaseId', $event)"
          @update-top-k-input="emit('updateTopKInput', $event)"
          @update-similarity-threshold-input="emit('updateSimilarityThresholdInput', $event)"
          @send="emit('send')"
          @stop="emit('stop')"
        />
      </div>

      <ReasoningPanel
        :reasoning-text="reasoningText"
        :reasoning-elapsed-text="reasoningElapsedText"
        :show-reasoning-timer="showReasoningTimer"
      />
    </section>
  </div>
</template>
