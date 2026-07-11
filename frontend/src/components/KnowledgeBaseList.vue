<script setup lang="ts">
import { computed } from 'vue'
import { useAppConfig } from '../config/app-config'
import UiIcon from './UiIcon.vue'

interface Props {
  activeKnowledgeBaseId: string
  knowledgeBases: string[]
  isLoading: boolean
  deletingKnowledgeBaseId: string
}

const props = defineProps<Props>()
const knowledgeText = useAppConfig().labels.knowledge

const emit = defineEmits<{
  selectKnowledgeBase: [value: string]
  deleteKnowledgeBase: [value: string]
  refreshKnowledgeBases: []
}>()

function isActiveKnowledgeBase(knowledgeBaseId: string): boolean {
  return props.activeKnowledgeBaseId === knowledgeBaseId
}

const listDescription = computed(() => {
  return `${props.knowledgeBases.length}${knowledgeText.availableKnowledgeBasesSuffix}`
})

function selectKnowledgeBase(knowledgeBaseId: string): void {
  emit('selectKnowledgeBase', knowledgeBaseId)
}
</script>

<template>
  <article class="knowledge-card kb-list-card">
    <div class="kb-list-head">
      <div>
        <div class="eyebrow">{{ knowledgeText.libraryEyebrow }}</div>
        <h2>{{ knowledgeText.existingKnowledgeBasesTitle }}</h2>
        <p>{{ listDescription }}</p>
      </div>
      <button class="icon-button subtle-button" :disabled="isLoading" :title="knowledgeText.refreshButton" @click="emit('refreshKnowledgeBases')">
        <UiIcon name="refresh" />
      </button>
    </div>

    <div v-if="isLoading" class="kb-list-state">{{ knowledgeText.listLoading }}</div>
    <div v-else-if="knowledgeBases.length === 0" class="kb-list-state muted-state">
      {{ knowledgeText.emptyKnowledgeBases }}
    </div>
    <div v-else class="kb-list">
      <div
        v-for="knowledgeBaseId in knowledgeBases"
        :key="knowledgeBaseId"
        :class="['kb-list-item', isActiveKnowledgeBase(knowledgeBaseId) ? 'active' : '']"
        role="button"
        tabindex="0"
        @click="selectKnowledgeBase(knowledgeBaseId)"
        @keydown.enter.prevent="selectKnowledgeBase(knowledgeBaseId)"
        @keydown.space.prevent="selectKnowledgeBase(knowledgeBaseId)"
      >
        <div class="kb-list-main">
          <span :class="['kb-dot', isActiveKnowledgeBase(knowledgeBaseId) ? 'active' : '']">
            <UiIcon v-if="isActiveKnowledgeBase(knowledgeBaseId)" name="check" />
          </span>
          <strong :title="knowledgeBaseId">{{ knowledgeBaseId }}</strong>
          <span v-if="isActiveKnowledgeBase(knowledgeBaseId)" class="mini-chip">
            {{ knowledgeText.selectedBadge }}
          </span>
        </div>
        <div class="kb-list-actions">
          <button
            class="icon-button danger-icon-button"
            :disabled="deletingKnowledgeBaseId === knowledgeBaseId"
            :title="deletingKnowledgeBaseId === knowledgeBaseId ? knowledgeText.deletingButton : knowledgeText.deleteButton"
            @click.stop="emit('deleteKnowledgeBase', knowledgeBaseId)"
          >
            <UiIcon name="trash" />
          </button>
        </div>
      </div>
    </div>
  </article>
</template>
