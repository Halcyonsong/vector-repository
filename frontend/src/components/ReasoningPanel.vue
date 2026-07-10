<script setup lang="ts">
import { useAppConfig } from '../config/app-config'

interface Props {
  reasoningText: string
  reasoningElapsedText: string
  showReasoningTimer: boolean
}

defineProps<Props>()

const chatText = useAppConfig().labels.chat
</script>

<template>
  <aside class="reasoning-panel">
    <div class="panel-head">
      <div>
        <div class="eyebrow">{{ chatText.reasoningEyebrow }}</div>
        <h2>{{ chatText.reasoningTitle }}</h2>
      </div>
      <div v-if="showReasoningTimer" class="dialog-timer reasoning-timer">
        <span>{{ chatText.reasoningTimerLabel }}</span>
        <strong>{{ reasoningElapsedText }}</strong>
      </div>
    </div>
    <pre class="panel-content">{{ reasoningText || chatText.reasoningEmpty }}</pre>
  </aside>
</template>
