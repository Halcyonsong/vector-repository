<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import SessionListItem from './SessionListItem.vue'
import type { SessionGroup } from '../types'

interface Props {
  currentView: 'chat' | 'knowledge'
  activeSessionId: string
  sessionGroups: SessionGroup[]
}

defineProps<Props>()

const emit = defineEmits<{
  changeView: [view: 'chat' | 'knowledge']
  selectSession: [sessionId: string]
  createSession: []
  renameSession: [sessionId: string]
  deleteSession: [sessionId: string]
}>()

const openedMenuSessionId = ref('')

function toggleMenu(sessionId: string): void {
  openedMenuSessionId.value = openedMenuSessionId.value === sessionId ? '' : sessionId
}

function closeMenu(): void {
  openedMenuSessionId.value = ''
}

function handleRename(sessionId: string): void {
  closeMenu()
  emit('renameSession', sessionId)
}

function handleDelete(sessionId: string): void {
  closeMenu()
  emit('deleteSession', sessionId)
}

function handleSelect(sessionId: string): void {
  closeMenu()
  emit('selectSession', sessionId)
}

function handleDocumentClick(): void {
  closeMenu()
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})
</script>

<template>
  <aside class="workspace-sidebar">
    <div class="brand-block">
      <div>
        <div class="brand-name">Vector Repository</div>
        <div class="brand-subtitle">Local RAG Workspace</div>
      </div>
    </div>

    <nav class="sidebar-nav">
      <button
        class="nav-button"
        :class="{ active: currentView === 'chat' }"
        @click="emit('changeView', 'chat')"
      >
        对话
      </button>
      <button
        class="nav-button"
        :class="{ active: currentView === 'knowledge' }"
        @click="emit('changeView', 'knowledge')"
      >
        知识库
      </button>
    </nav>

    <div class="sidebar-section">
      <div class="sidebar-section-head">
        <span>会话</span>
        <button class="new-session-button" title="新建会话" aria-label="新建会话" @click="emit('createSession')">+</button>
      </div>

      <div class="session-list grouped-session-list">
        <section v-for="group in sessionGroups" :key="group.label" class="session-group">
          <header class="session-group-title">{{ group.label }}</header>
          <div class="session-group-items">
            <SessionListItem
              v-for="session in group.items"
              :key="session.sessionId"
              :session="session"
              :active="session.sessionId === activeSessionId"
              :menu-open="openedMenuSessionId === session.sessionId"
              @select="handleSelect($event)"
              @toggle-menu="toggleMenu($event)"
              @rename="handleRename($event)"
              @delete="handleDelete($event)"
            />
          </div>
        </section>
      </div>
    </div>
  </aside>
</template>
