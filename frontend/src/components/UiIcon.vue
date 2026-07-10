<script setup lang="ts">
type IconName = 'archive' | 'check' | 'database' | 'file' | 'refresh' | 'spark' | 'trash' | 'upload'

interface Props {
  name: IconName
}

defineProps<Props>()
</script>

<template>
  <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
    <path v-if="name === 'archive'" d="M5 7.5h14M7 7.5v10.2c0 .7.6 1.3 1.3 1.3h7.4c.7 0 1.3-.6 1.3-1.3V7.5M8.4 4.5h7.2c.5 0 .9.4.9.9v2.1h-9V5.4c0-.5.4-.9.9-.9Z" />
    <path v-else-if="name === 'check'" d="m5 12.5 4.2 4.1L19 6.8" />
    <path v-else-if="name === 'database'" d="M5 7c0-1.7 3.1-3 7-3s7 1.3 7 3-3.1 3-7 3-7-1.3-7-3Zm0 0v5c0 1.7 3.1 3 7 3s7-1.3 7-3V7M5 12v5c0 1.7 3.1 3 7 3s7-1.3 7-3v-5" />
    <path v-else-if="name === 'file'" d="M7 3.8h6.2L17 7.6v12.6H7V3.8Zm6 0v4h4" />
    <path v-else-if="name === 'refresh'" d="M18.5 8.2A7 7 0 0 0 6 6.6L4.5 8.2M5.5 15.8A7 7 0 0 0 18 17.4l1.5-1.6M4.5 4.8v3.4h3.4M19.5 19.2v-3.4h-3.4" />
    <path v-else-if="name === 'spark'" d="M12 3.8 13.7 9l5.1 1.8-5.1 1.8L12 17.8l-1.7-5.2-5.1-1.8L10.3 9 12 3.8Zm6.2 12.4.7 2.1 2.1.7-2.1.7-.7 2.1-.7-2.1-2.1-.7 2.1-.7.7-2.1Z" />
    <path v-else-if="name === 'trash'" d="M5.5 7h13M9 7V5.2c0-.5.4-.9.9-.9h4.2c.5 0 .9.4.9.9V7m2 0-.7 12H7.7L7 7m3.2 3.2v5.6m3.6-5.6v5.6" />
    <path v-else d="M12 15.5V4.8m0 0L8.2 8.6M12 4.8l3.8 3.8M5.8 14.5v3.7c0 .6.5 1 1 1h10.4c.6 0 1-.4 1-1v-3.7" />
  </svg>
</template>
