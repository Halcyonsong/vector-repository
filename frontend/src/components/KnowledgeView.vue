<script setup lang="ts">
import { useAppConfig } from '../config/app-config'
import KnowledgeBaseList from './KnowledgeBaseList.vue'
import KnowledgeHero from './KnowledgeHero.vue'
import KnowledgeUploadForm from './KnowledgeUploadForm.vue'
import UiIcon from './UiIcon.vue'
import type { KnowledgeBaseUploadTaskVO } from '../types'

interface Props {
  knowledgeBaseId: string
  knowledgeBases: string[]
  selectedFileName: string
  uploadStatusText: string
  activeUploadTask: KnowledgeBaseUploadTaskVO | null
  uploadProgressPercent: number
  uploadProgressText: string
  isKnowledgeBaseListLoading: boolean
  isUploading: boolean
  deletingKnowledgeBaseId: string
}

defineProps<Props>()

const knowledgeText = useAppConfig().labels.knowledge

const emit = defineEmits<{
  updateKnowledgeBaseId: [value: string]
  selectFile: [event: Event]
  selectDroppedFile: [file: File]
  upload: []
  deleteKnowledgeBase: [value: string]
  refreshKnowledgeBases: []
  backToChat: []
}>()
</script>

<template>
  <div class="page-view">
    <section class="knowledge-topbar">
      <div class="knowledge-title-block">
        <div class="eyebrow">{{ knowledgeText.pageEyebrow }}</div>
        <h1><UiIcon name="spark" />{{ knowledgeText.pageTitle }}</h1>
        <p>{{ knowledgeText.heroDescription }}</p>
      </div>
      <div class="knowledge-topbar-meta">
        <div class="knowledge-meta-card">
          <span>{{ knowledgeText.modeLabel }}</span>
          <strong>{{ knowledgeText.pageMode }}</strong>
        </div>
        <div class="knowledge-meta-card wide">
          <span>{{ knowledgeText.formatsLabel }}</span>
          <strong>{{ knowledgeText.pageModeDescription }}</strong>
        </div>
        <div class="knowledge-meta-card">
          <span>{{ knowledgeText.librariesLabel }}</span>
          <strong>{{ knowledgeBases.length }}</strong>
        </div>
      </div>
    </section>

    <section class="knowledge-workbench">
      <div class="knowledge-main-column">
        <KnowledgeHero :knowledge-base-id="knowledgeBaseId" :selected-file-name="selectedFileName" />

        <KnowledgeUploadForm
          :knowledge-base-id="knowledgeBaseId"
          :upload-status-text="uploadStatusText"
          :active-upload-task="activeUploadTask"
          :upload-progress-percent="uploadProgressPercent"
          :upload-progress-text="uploadProgressText"
          :is-uploading="isUploading"
          @update-knowledge-base-id="emit('updateKnowledgeBaseId', $event)"
          @select-file="emit('selectFile', $event)"
          @select-dropped-file="emit('selectDroppedFile', $event)"
          @upload="emit('upload')"
          @back-to-chat="emit('backToChat')"
        >
          <template #file-name>{{ selectedFileName }}</template>
        </KnowledgeUploadForm>
      </div>

      <KnowledgeBaseList
        :active-knowledge-base-id="knowledgeBaseId"
        :knowledge-bases="knowledgeBases"
        :is-loading="isKnowledgeBaseListLoading"
        :deleting-knowledge-base-id="deletingKnowledgeBaseId"
        @select-knowledge-base="emit('updateKnowledgeBaseId', $event)"
        @delete-knowledge-base="emit('deleteKnowledgeBase', $event)"
        @refresh-knowledge-bases="emit('refreshKnowledgeBases')"
      />
    </section>
  </div>
</template>
