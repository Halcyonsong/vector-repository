import { computed, onBeforeUnmount, ref } from 'vue'

export function useChatTimers() {
  const nowMs = ref(Date.now())
  const requestStartedAt = ref<number | null>(null)
  const firstOutputAt = ref<number | null>(null)
  const reasoningStartedAt = ref<number | null>(null)
  const reasoningEndedAt = ref<number | null>(null)
  const answerStartedAt = ref<number | null>(null)
  const answerEndedAt = ref<number | null>(null)
  let timerId: number | null = null

  const startupElapsedText = computed(() => {
    return formatElapsed(requestStartedAt.value, firstOutputAt.value)
  })

  const reasoningElapsedText = computed(() => {
    return formatElapsed(reasoningStartedAt.value, reasoningEndedAt.value)
  })

  const answerElapsedText = computed(() => {
    return formatElapsed(answerStartedAt.value, answerEndedAt.value)
  })

  const showStartupTimer = computed(() => {
    return requestStartedAt.value !== null
  })

  const showReasoningTimer = computed(() => {
    return reasoningStartedAt.value !== null
  })

  const showAnswerTimer = computed(() => {
    return answerStartedAt.value !== null
  })

  function startTimer(): void {
    stopTimer()
    nowMs.value = Date.now()
    timerId = window.setInterval(() => {
      nowMs.value = Date.now()
    }, 50)
  }

  function stopTimer(): void {
    if (timerId !== null) {
      window.clearInterval(timerId)
      timerId = null
    }
  }

  function formatElapsed(startTime: number | null, endTime: number | null): string {
    if (startTime === null) {
      return '0.00'
    }

    const effectiveEndTime = endTime ?? nowMs.value
    return Math.max((effectiveEndTime - startTime) / 1000, 0).toFixed(2)
  }

  function resetTimers(): void {
    stopTimer()
    requestStartedAt.value = null
    firstOutputAt.value = null
    reasoningStartedAt.value = null
    reasoningEndedAt.value = null
    answerStartedAt.value = null
    answerEndedAt.value = null
  }

  function markRequestStarted(timestamp = Date.now()): void {
    requestStartedAt.value = timestamp
    startTimer()
  }

  function markFirstOutput(timestamp: number): void {
    if (firstOutputAt.value === null) {
      firstOutputAt.value = timestamp
    }
  }

  function markReasoningStarted(timestamp: number): void {
    markFirstOutput(timestamp)
    if (reasoningStartedAt.value === null) {
      reasoningStartedAt.value = timestamp
    }
  }

  function markAnswerStarted(timestamp: number): void {
    markFirstOutput(timestamp)
    if (answerStartedAt.value === null) {
      answerStartedAt.value = timestamp
    }
    if (reasoningStartedAt.value !== null && reasoningEndedAt.value === null) {
      reasoningEndedAt.value = timestamp
    }
  }

  function finishActiveTimers(timestamp = Date.now()): void {
    if (reasoningStartedAt.value !== null && reasoningEndedAt.value === null) {
      reasoningEndedAt.value = timestamp
    }
    if (answerStartedAt.value !== null && answerEndedAt.value === null) {
      answerEndedAt.value = timestamp
    }
    if (firstOutputAt.value === null) {
      firstOutputAt.value = timestamp
    }
    stopTimer()
  }

  onBeforeUnmount(() => {
    stopTimer()
  })

  return {
    startupElapsedText,
    reasoningElapsedText,
    answerElapsedText,
    showStartupTimer,
    showReasoningTimer,
    showAnswerTimer,
    resetTimers,
    markRequestStarted,
    markReasoningStarted,
    markAnswerStarted,
    finishActiveTimers,
    stopTimer
  }
}
