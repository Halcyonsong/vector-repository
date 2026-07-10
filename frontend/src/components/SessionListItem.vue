<script setup lang="ts">
import { computed } from 'vue'
import type { SessionVO } from '../types'

interface Props {
  session: SessionVO
  active: boolean
  menuOpen: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [sessionId: string]
  toggleMenu: [sessionId: string]
  rename: [sessionId: string]
  delete: [sessionId: string]
}>()

const sessionId = computed(() => {
  return props.session.sessionId
})
</script>

<template>
  <div class="session-row">
    <button class="session-select-button" :class="{ active }" @click="emit('select', sessionId)">
      <span class="session-title only-title">{{ session.title }}</span>
    </button>

    <div class="session-menu-wrap" @click.stop>
      <button
        class="session-menu-trigger"
        :class="{ active: menuOpen }"
        title="会话操作"
        @click="emit('toggleMenu', sessionId)"
      >
        ···
      </button>

      <div v-if="menuOpen" class="session-menu-dropdown">
        <button class="session-menu-item" @click="emit('rename', sessionId)">重命名</button>
        <button class="session-menu-item danger" @click="emit('delete', sessionId)">删除</button>
      </div>
    </div>
  </div>
</template>
