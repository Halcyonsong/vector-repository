<script setup lang="ts">
import { useAppConfig } from '../config/app-config'

interface Props {
  question: string
  systemPromptInput: string
  useKnowledgeBase: boolean
  allowEmptyContext: boolean
  knowledgeBaseId: string
  knowledgeBases: string[]
  topKInput: string
  similarityThresholdInput: string
  isStreaming: boolean
  isStopping: boolean
  hasOutputError: boolean
  startupElapsedText: string
  answerElapsedText: string
  showStartupTimer: boolean
  showAnswerTimer: boolean
}

defineProps<Props>()

const appConfig = useAppConfig()
const behaviorConfig = appConfig.behavior
const chatText = appConfig.labels.chat

const emit = defineEmits<{
  updateQuestion: [value: string]
  updateSystemPromptInput: [value: string]
  updateUseKnowledgeBase: [value: boolean]
  updateAllowEmptyContext: [value: boolean]
  updateKnowledgeBaseId: [value: string]
  updateTopKInput: [value: string]
  updateSimilarityThresholdInput: [value: string]
  send: []
  stop: []
}>()

function formatPlaceholder(template: string, values: Record<string, string | number>): string {
  return Object.entries(values).reduce((result, [key, value]) => {
    return result.replace(`{${key}}`, String(value))
  }, template)
}

function handleQuestionKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) {
    return
  }

  event.preventDefault()
  emit('send')
}
</script>

<template>
  <div class="composer-shell">
    <div class="composer-toolbar">
      <label class="switch-field">
        <span class="switch-label">{{ chatText.toggleKnowledgeBase }}</span>
        <span class="switch-control">
          <input
            :checked="useKnowledgeBase"
            type="checkbox"
            @change="emit('updateUseKnowledgeBase', ($event.target as HTMLInputElement).checked)"
          />
          <span class="switch-track"><span class="switch-thumb"></span></span>
        </span>
      </label>

      <label class="switch-field forward-switch-field">
        <span class="switch-label">{{ chatText.allowEmptyContextLabel }}</span>
        <span class="switch-control">
          <input
            :checked="allowEmptyContext"
            type="checkbox"
            @change="emit('updateAllowEmptyContext', ($event.target as HTMLInputElement).checked)"
          />
          <span class="switch-track"><span class="switch-thumb"></span></span>
        </span>
      </label>

      <label class="inline-field kb-inline-field">
        <span>{{ chatText.knowledgeBaseLabel }}</span>
        <input
          :value="knowledgeBaseId"
          :placeholder="chatText.knowledgeBasePlaceholder"
          list="composer-knowledge-bases"
          @input="emit('updateKnowledgeBaseId', ($event.target as HTMLInputElement).value)"
        />
        <datalist id="composer-knowledge-bases">
          <option v-for="knowledgeBase in knowledgeBases" :key="knowledgeBase" :value="knowledgeBase" />
        </datalist>
      </label>

      <label class="inline-field">
        <span>{{ chatText.topKLabel }}</span>
        <input
          :value="topKInput"
          inputmode="numeric"
          :placeholder="formatPlaceholder(chatText.topKPlaceholder, {
            default: behaviorConfig.defaultTopK,
            min: behaviorConfig.topKRange.min,
            max: behaviorConfig.topKRange.max
          })"
          @input="emit('updateTopKInput', ($event.target as HTMLInputElement).value)"
        />
      </label>

      <label class="inline-field">
        <span>{{ chatText.similarityThresholdLabel }}</span>
        <input
          :value="similarityThresholdInput"
          inputmode="decimal"
          :placeholder="formatPlaceholder(chatText.similarityThresholdPlaceholder, {
            default: behaviorConfig.defaultSimilarityThreshold,
            min: behaviorConfig.similarityThresholdRange.min,
            max: behaviorConfig.similarityThresholdRange.max
          })"
          @input="emit('updateSimilarityThresholdInput', ($event.target as HTMLInputElement).value)"
        />
      </label>

      <div :class="['output-status-box', hasOutputError ? 'error' : 'normal']">
        <span>{{ chatText.outputStatusLabel }}</span>
        <strong>{{ hasOutputError ? chatText.outputStatusError : chatText.outputStatusNormal }}</strong>
      </div>

      <div :class="['timer-cluster', showStartupTimer || showAnswerTimer ? 'visible' : '']">
        <div :class="['dialog-timer', showStartupTimer ? '' : 'hidden']">
          <span>{{ chatText.startupTimerLabel }}</span>
          <strong>{{ startupElapsedText }}</strong>
        </div>
        <div :class="['dialog-timer', showAnswerTimer ? '' : 'hidden']">
          <span>{{ chatText.answerTimerLabel }}</span>
          <strong>{{ answerElapsedText }}</strong>
        </div>
      </div>
    </div>

    <div class="composer-input-grid">
      <label class="composer-field question-field">
        <span>{{ chatText.questionInputLabel }}</span>
        <textarea
          :value="question"
          rows="4"
          :placeholder="chatText.questionPlaceholder"
          @input="emit('updateQuestion', ($event.target as HTMLTextAreaElement).value)"
          @keydown="handleQuestionKeydown"
        ></textarea>
      </label>

      <label class="composer-field system-prompt-field">
        <span>{{ chatText.systemPromptInputLabel }}</span>
        <textarea
          :value="systemPromptInput"
          rows="4"
          :placeholder="chatText.systemPromptPlaceholder"
          @input="emit('updateSystemPromptInput', ($event.target as HTMLTextAreaElement).value)"
        ></textarea>
      </label>
    </div>

    <div class="composer-actions">
      <div class="button-row">
        <button class="secondary-button" :disabled="!isStreaming || isStopping" @click="emit('stop')">
          {{ isStopping ? chatText.stoppingButton : chatText.stopButton }}
        </button>
        <button class="primary-button" :disabled="isStreaming" @click="emit('send')">
          {{ chatText.sendButton }}
        </button>
      </div>
    </div>
  </div>
</template>
