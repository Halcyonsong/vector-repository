<script setup lang="ts">
import { ref } from 'vue'
import { useAppConfig } from '../config/app-config'
import UiIcon from './UiIcon.vue'

interface Props {
  knowledgeBaseId: string
  uploadStatusText: string
  isUploading: boolean
}

defineProps<Props>()

const knowledgeText = useAppConfig().labels.knowledge

const emit = defineEmits<{
  updateKnowledgeBaseId: [value: string]
  selectFile: [event: Event]
  selectDroppedFile: [file: File]
  upload: []
  backToChat: []
}>()

const isDragActive = ref(false)

function handleDragEnter(): void {
  isDragActive.value = true
}

function handleDragLeave(event: DragEvent): void {
  if (event.currentTarget === event.target) {
    isDragActive.value = false
  }
}

function handleDrop(event: DragEvent): void {
  isDragActive.value = false
  const file = event.dataTransfer?.files.item(0)
  if (file) {
    emit('selectDroppedFile', file)
  }
}
</script>

<template>
  <article class="knowledge-card upload-card">
    <div class="upload-card-head">
      <div>
        <div class="eyebrow">{{ knowledgeText.uploadEyebrow }}</div>
        <h2><UiIcon name="upload" />{{ knowledgeText.uploadDocumentLabel }}</h2>
      </div>
      <span class="format-pill">{{ knowledgeText.pageModeDescription }}</span>
    </div>

    <div class="upload-form-grid">
    <label class="field">
      <span>{{ knowledgeText.knowledgeBaseLabel }}</span>
      <input
        :value="knowledgeBaseId"
        :placeholder="knowledgeText.knowledgeBasePlaceholder"
        @input="emit('updateKnowledgeBaseId', ($event.target as HTMLInputElement).value)"
      />
    </label>

    <label
      :class="['dropzone-field', isDragActive ? 'active' : '']"
      @dragenter.prevent="handleDragEnter"
      @dragover.prevent
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <input
        class="dropzone-input"
        type="file"
        accept=".txt,.pdf,.ppt,.pptx,text/plain,application/pdf,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation"
        @change="emit('selectFile', $event)"
      />
      <span class="dropzone-icon"><UiIcon name="upload" /></span>
      <span class="dropzone-copy">{{ knowledgeText.dropzoneTitle }}</span>
      <small>{{ knowledgeText.dropzoneDescription }}</small>
    </label>
    </div>

    <div class="upload-summary">
      <div class="upload-file-label">{{ knowledgeText.currentFile }}</div>
      <div class="upload-file-name"><slot name="file-name" /></div>
    </div>

    <div class="upload-actions">
      <button class="primary-button" :disabled="isUploading" @click="emit('upload')">{{ isUploading ? knowledgeText.uploadingButton : knowledgeText.uploadButton }}</button>
      <button class="secondary-button" @click="emit('backToChat')">{{ knowledgeText.backButton }}</button>
    </div>

    <p v-if="uploadStatusText" class="success-text">{{ uploadStatusText }}</p>
  </article>
</template>
