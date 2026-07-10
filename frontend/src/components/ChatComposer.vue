<script setup lang="ts">
import { useAppConfig } from '../config/app-config'

interface Props {
  question: string
  useKnowledgeBase: boolean
  knowledgeBaseId: string
  knowledgeBases: string[]
  topKInput: string
  similarityThresholdInput: string
  isStreaming: boolean
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
  updateUseKnowledgeBase: [value: boolean]
  updateKnowledgeBaseId: [value: string]
  updateTopKInput: [value: string]
  updateSimilarityThresholdInput: [value: string]
  send: []
  stop: []
}>()
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
          :placeholder="`留空默认 ${behaviorConfig.defaultTopK}，范围 ${behaviorConfig.topKRange.min}-${behaviorConfig.topKRange.max}`"
          @input="emit('updateTopKInput', ($event.target as HTMLInputElement).value)"
        />
      </label>

      <label class="inline-field">
        <span>{{ chatText.similarityThresholdLabel }}</span>
        <input
          :value="similarityThresholdInput"
          inputmode="decimal"
          :placeholder="`留空默认 ${behaviorConfig.defaultSimilarityThreshold}，范围 ${behaviorConfig.similarityThresholdRange.min}-${behaviorConfig.similarityThresholdRange.max}`"
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

    <label class="composer-field">
      <textarea
        :value="question"
        rows="4"
        :placeholder="chatText.questionPlaceholder"
        @input="emit('updateQuestion', ($event.target as HTMLTextAreaElement).value)"
      ></textarea>
    </label>

    <div class="composer-actions">
      <div class="button-row">
        <button class="secondary-button" :disabled="!isStreaming" @click="emit('stop')">
          {{ chatText.stopButton }}
        </button>
        <button class="primary-button" :disabled="isStreaming" @click="emit('send')">
          {{ chatText.sendButton }}
        </button>
      </div>
    </div>
  </div>
</template>
