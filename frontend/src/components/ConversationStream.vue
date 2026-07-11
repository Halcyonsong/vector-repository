<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useAppConfig } from '../config/app-config'
import MessageContent from './MessageContent.vue'
import UiIcon from './UiIcon.vue'
import type { ChatHistoryMessageVO } from '../types'

const PULL_TRIGGER_DISTANCE = 64
const PULL_MAX_DISTANCE = 96
const PULL_REVEAL_DISTANCE = 48
const PULL_RELEASE_DELAY_MS = 520
const WHEEL_RELEASE_DELAY_MS = 180

interface Props {
  messages: ChatHistoryMessageVO[]
  hasConversation: boolean
  historyScrollKey: number
  hasMoreHistory: boolean
  isLoadingEarlierHistory: boolean
  isStreaming: boolean
}

const props = defineProps<Props>()
const chatText = useAppConfig().labels.chat
const streamElement = ref<HTMLElement | null>(null)
const pullDistance = ref(0)
const isPointerDown = ref(false)
const isRebounding = ref(false)
const copiedMessageIndex = ref<number | null>(null)
const startY = ref(0)
const pendingPrependSnapshot = ref<{ scrollHeight: number; scrollTop: number } | null>(null)
let releaseTimer: number | null = null
let wheelReleaseTimer: number | null = null
let copiedStateTimer: number | null = null

const emit = defineEmits<{
  loadEarlierHistory: []
  rollbackLatestRound: []
  undoLatestRound: [question: string]
}>()

const visibleMessages = computed(() => {
  return props.messages.filter((message) => {
    return message.role === 'assistant' || message.role === 'user'
  })
})

const latestRoundAction = computed(() => {
  if (props.isStreaming) {
    return null
  }

  for (let index = visibleMessages.value.length - 1; index >= 0; index -= 1) {
    const message = visibleMessages.value[index]
    if (message.role !== 'assistant') {
      continue
    }

    const userIndex = visibleMessages.value.slice(0, index).findLastIndex((item) => item.role === 'user')
    const userMessage = userIndex >= 0 ? visibleMessages.value[userIndex] : null
    if (!userMessage?.content) {
      return null
    }

    return {
      userIndex,
      assistantIndex: index,
      userQuestion: userMessage.content
    }
  }

  return null
})

const pullText = computed(() => {
  if (props.isLoadingEarlierHistory) {
    return chatText.loadingEarlierHistory
  }
  if (!props.hasMoreHistory && visibleMessages.value.length > 0) {
    return chatText.noMoreHistory
  }
  return chatText.pullEarlierHistory
})

const isReadyToLoad = computed(() => {
  return pullDistance.value >= PULL_TRIGGER_DISTANCE && props.hasMoreHistory
})

const isPullZoneVisible = computed(() => {
  return pullDistance.value > 0 || props.isLoadingEarlierHistory || isRebounding.value
})

const pullZoneStyle = computed(() => {
  const height = props.isLoadingEarlierHistory ? PULL_REVEAL_DISTANCE : pullDistance.value
  return {
    height: `${Math.min(height, PULL_MAX_DISTANCE)}px`
  }
})

async function scrollToBottom(): Promise<void> {
  await nextTick()
  if (!streamElement.value) {
    return
  }

  streamElement.value.scrollTop = streamElement.value.scrollHeight
}

function canRevealPullZone(): boolean {
  return Boolean(streamElement.value && streamElement.value.scrollTop <= 0 && props.hasConversation)
}

function clearTimer(timer: number | null): void {
  if (timer !== null) {
    window.clearTimeout(timer)
  }
}

function setPullDistance(nextDistance: number): void {
  pullDistance.value = Math.max(0, Math.min(nextDistance, PULL_MAX_DISTANCE))
}

function capturePrependSnapshot(): void {
  if (!streamElement.value) {
    return
  }

  pendingPrependSnapshot.value = {
    scrollHeight: streamElement.value.scrollHeight,
    scrollTop: streamElement.value.scrollTop
  }
}

async function restoreScrollAfterPrepend(): Promise<void> {
  await nextTick()
  if (!streamElement.value || !pendingPrependSnapshot.value) {
    return
  }

  const previous = pendingPrependSnapshot.value
  const heightDelta = streamElement.value.scrollHeight - previous.scrollHeight
  streamElement.value.scrollTop = previous.scrollTop + heightDelta
  pendingPrependSnapshot.value = null
}

function requestEarlierHistory(): void {
  if (props.isLoadingEarlierHistory || !props.hasMoreHistory) {
    return
  }

  capturePrependSnapshot()
  emit('loadEarlierHistory')
}

function reboundWithNotice(): void {
  clearTimer(releaseTimer)
  isRebounding.value = true
  setPullDistance(PULL_REVEAL_DISTANCE)

  releaseTimer = window.setTimeout(() => {
    setPullDistance(0)
    isRebounding.value = false
  }, PULL_RELEASE_DELAY_MS)
}

function releasePull(): void {
  const shouldLoadEarlier = pullDistance.value >= PULL_TRIGGER_DISTANCE && props.hasMoreHistory
  isPointerDown.value = false

  if (shouldLoadEarlier) {
    setPullDistance(0)
    requestEarlierHistory()
    return
  }

  if (!props.hasMoreHistory && pullDistance.value > 0) {
    reboundWithNotice()
    return
  }

  setPullDistance(0)
}

function handlePointerDown(event: PointerEvent): void {
  if (!canRevealPullZone()) {
    return
  }

  clearTimer(releaseTimer)
  isRebounding.value = false
  isPointerDown.value = true
  startY.value = event.clientY
  streamElement.value?.setPointerCapture(event.pointerId)
}

function handlePointerMove(event: PointerEvent): void {
  if (!isPointerDown.value || !canRevealPullZone()) {
    return
  }

  const distance = event.clientY - startY.value
  if (distance <= 0) {
    setPullDistance(0)
    return
  }

  event.preventDefault()
  setPullDistance(distance * 0.58)
}

function handlePointerEnd(): void {
  if (!isPointerDown.value) {
    return
  }

  releasePull()
}

function handleWheel(event: WheelEvent): void {
  if (!canRevealPullZone() || event.deltaY >= 0 || props.isLoadingEarlierHistory) {
    return
  }

  event.preventDefault()
  clearTimer(wheelReleaseTimer)
  clearTimer(releaseTimer)
  isRebounding.value = false
  setPullDistance(pullDistance.value + Math.min(Math.abs(event.deltaY) * 0.36, 18))

  wheelReleaseTimer = window.setTimeout(() => {
    releasePull()
  }, WHEEL_RELEASE_DELAY_MS)
}

watch(
  () => props.historyScrollKey,
  () => {
    void scrollToBottom()
  }
)

watch(
  () => props.isLoadingEarlierHistory,
  (isLoading, wasLoading) => {
    if (!isLoading && wasLoading) {
      void restoreScrollAfterPrepend()
    }
  }
)

onBeforeUnmount(() => {
  clearTimer(releaseTimer)
  clearTimer(wheelReleaseTimer)
  clearTimer(copiedStateTimer)
})

function isUserMessage(message: ChatHistoryMessageVO): boolean {
  return message.role === 'user'
}

function resolveRoleText(message: ChatHistoryMessageVO): string {
  return isUserMessage(message) ? chatText.userRole : chatText.assistantRole
}

function resolveMessageStatus(message: ChatHistoryMessageVO): string {
  if (message.role !== 'assistant' || message.status === 'completed') {
    return ''
  }
  return message.status
}

function resolveMessageText(message: ChatHistoryMessageVO): string {
  if (message.role === 'assistant' && !message.content) {
    return chatText.answerPending
  }
  return message.content
}

function shouldShowLatestRoundActions(message: ChatHistoryMessageVO, index: number): boolean {
  if (message.role === 'assistant') {
    return latestRoundAction.value?.assistantIndex === index
  }

  if (message.role === 'user') {
    return latestRoundAction.value?.userIndex === index
  }

  return false
}

async function writeClipboardText(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}

async function handleCopyLatestRound(index: number, content: string): Promise<void> {
  if (!content) {
    return
  }

  await writeClipboardText(content)
  copiedMessageIndex.value = index
  clearTimer(copiedStateTimer)
  copiedStateTimer = window.setTimeout(() => {
    copiedMessageIndex.value = null
  }, 1300)
}

function isCopiedMessage(index: number): boolean {
  return copiedMessageIndex.value === index
}

function handleUndoLatestRound(): void {
  const userQuestion = latestRoundAction.value?.userQuestion
  if (userQuestion) {
    emit('undoLatestRound', userQuestion)
  }
}

function handleRollbackPointerDown(event: PointerEvent): void {
  event.stopPropagation()
}
</script>

<template>
  <div
    ref="streamElement"
    class="conversation-stream"
    @pointerdown="handlePointerDown"
    @pointermove="handlePointerMove"
    @pointerup="handlePointerEnd"
    @pointercancel="handlePointerEnd"
    @mouseleave="handlePointerEnd"
    @wheel="handleWheel"
  >
    <div
      :class="[
        'history-pull-zone',
        isPullZoneVisible ? 'visible' : '',
        isReadyToLoad ? 'ready' : '',
        isLoadingEarlierHistory ? 'loading' : '',
        !hasMoreHistory ? 'exhausted' : ''
      ]"
      :style="pullZoneStyle"
    >
      <span>{{ pullText }}</span>
    </div>

    <div v-if="!hasConversation" class="empty-state">
      <div class="empty-title">{{ chatText.emptyTitle }}</div>
      <p>{{ chatText.emptyDescription }}</p>
    </div>

    <template v-else>
      <article
        v-for="(message, index) in visibleMessages"
        :key="`${message.createTime}-${message.role}-${index}`"
        class="message-row"
        :class="isUserMessage(message) ? 'user-row' : 'assistant-row'"
      >
        <div
          class="message-bubble"
          :class="isUserMessage(message) ? 'user-bubble' : 'assistant-bubble'"
        >
          <div class="message-role">
            {{ resolveRoleText(message) }}
            <span v-if="resolveMessageStatus(message)" class="message-status">
              {{ resolveMessageStatus(message) }}
            </span>
          </div>
          <MessageContent :content="resolveMessageText(message)" />
          <div v-if="shouldShowLatestRoundActions(message, index)" class="message-actions" :aria-label="chatText.latestRoundActionsLabel">
            <button
              class="message-action-button"
              type="button"
              :title="isCopiedMessage(index) ? chatText.copyDoneTitle : chatText.copyMessageTitle"
              :aria-label="isCopiedMessage(index) ? chatText.copyDoneTitle : chatText.copyMessageTitle"
              @pointerdown="handleRollbackPointerDown"
              @click.stop="handleCopyLatestRound(index, resolveMessageText(message))"
            >
              <UiIcon :name="isCopiedMessage(index) ? 'check' : 'copy'" />
            </button>
            <button
              class="message-action-button danger"
              type="button"
              :title="chatText.rollbackDeleteTitle"
              :aria-label="chatText.rollbackDeleteTitle"
              @pointerdown="handleRollbackPointerDown"
              @click.stop="emit('rollbackLatestRound')"
            >
              <UiIcon name="trash" />
            </button>
            <button
              class="message-action-button"
              type="button"
              :title="chatText.rollbackUndoTitle"
              :aria-label="chatText.rollbackUndoTitle"
              @pointerdown="handleRollbackPointerDown"
              @click.stop="handleUndoLatestRound"
            >
              <UiIcon name="undo" />
            </button>
          </div>
        </div>
      </article>
    </template>
  </div>
</template>
