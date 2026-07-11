<script setup lang="ts">
import { computed, ref } from 'vue'
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

const props = defineProps<Props>()

const appConfig = useAppConfig()
const behaviorConfig = appConfig.behavior
const chatText = appConfig.labels.chat
const isKnowledgeBasePickerOpen = ref(false)
const shouldFilterKnowledgeBases = ref(false)

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

const filteredKnowledgeBases = computed(() => {
  const keyword = shouldFilterKnowledgeBases.value ? props.knowledgeBaseId.trim().toLowerCase() : ''

  if (!keyword) {
    return props.knowledgeBases
  }

  return props.knowledgeBases.filter((knowledgeBase) => {
    return knowledgeBase.toLowerCase().includes(keyword)
  })
})

function emitKnowledgeBaseId(value: string): void {
  emit('updateKnowledgeBaseId', value)
}

function updateKnowledgeBaseId(value: string): void {
  shouldFilterKnowledgeBases.value = true
  emitKnowledgeBaseId(value)
}

function selectKnowledgeBaseId(value: string): void {
  shouldFilterKnowledgeBases.value = false
  emitKnowledgeBaseId(value)
  isKnowledgeBasePickerOpen.value = false
}

function openKnowledgeBasePicker(): void {
  if (props.knowledgeBases.length > 0) {
    shouldFilterKnowledgeBases.value = false
    isKnowledgeBasePickerOpen.value = true
  }
}

function closeKnowledgeBasePicker(): void {
  window.setTimeout(() => {
    isKnowledgeBasePickerOpen.value = false
  }, 120)
}

function toggleKnowledgeBasePicker(): void {
  shouldFilterKnowledgeBases.value = false
  isKnowledgeBasePickerOpen.value = !isKnowledgeBasePickerOpen.value && props.knowledgeBases.length > 0
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
        <div class="knowledge-base-combobox">
          <input
            :value="knowledgeBaseId"
            :placeholder="chatText.knowledgeBasePlaceholder"
            autocomplete="off"
            @focus="openKnowledgeBasePicker"
            @blur="closeKnowledgeBasePicker"
            @input="updateKnowledgeBaseId(($event.target as HTMLInputElement).value)"
          />
          <button
            class="knowledge-base-picker-button"
            type="button"
            :disabled="knowledgeBases.length === 0"
            :aria-label="chatText.knowledgeBaseSelectPlaceholder"
            @mousedown.prevent
            @click="toggleKnowledgeBasePicker"
          >
            <span></span>
          </button>
          <div v-if="isKnowledgeBasePickerOpen" class="knowledge-base-dropdown">
            <button
              v-for="knowledgeBase in filteredKnowledgeBases"
              :key="knowledgeBase"
              class="knowledge-base-option"
              type="button"
              @mousedown.prevent="selectKnowledgeBaseId(knowledgeBase)"
            >
              {{ knowledgeBase }}
            </button>
            <div v-if="filteredKnowledgeBases.length === 0" class="knowledge-base-empty-option">
              {{ chatText.knowledgeBaseEmptyOption }}
            </div>
          </div>
        </div>
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
