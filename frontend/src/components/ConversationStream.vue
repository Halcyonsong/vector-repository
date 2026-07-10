<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useAppConfig } from '../config/app-config'
import MessageContent from './MessageContent.vue'
import type { ChatHistoryMessageVO } from '../types'

interface Props {
  messages: ChatHistoryMessageVO[]
  hasConversation: boolean
  historyScrollKey: number
}

const props = defineProps<Props>()
const chatText = useAppConfig().labels.chat
const streamElement = ref<HTMLElement | null>(null)

const visibleMessages = computed(() => {
  return props.messages.filter((message) => {
    return message.role === 'assistant' || message.role === 'user'
  })
})

async function scrollToBottom(): Promise<void> {
  await nextTick()
  if (!streamElement.value) {
    return
  }

  streamElement.value.scrollTop = streamElement.value.scrollHeight
}

watch(
  () => props.historyScrollKey,
  () => {
    void scrollToBottom()
  }
)

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
</script>

<template>
  <div ref="streamElement" class="conversation-stream">
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
        </div>
      </article>
    </template>
  </div>
</template>
