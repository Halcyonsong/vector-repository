<script setup lang="ts">
import { useAppConfig } from '../config/app-config'
import UiIcon from './UiIcon.vue'

interface Props {
  knowledgeBaseId: string
  selectedFileName: string
}

defineProps<Props>()

const appConfig = useAppConfig()
const knowledgeText = appConfig.labels.knowledge
const uiText = appConfig.labels.ui
</script>

<template>
  <article class="knowledge-card hero-card">
    <div class="hero-copy">
      <div class="eyebrow">{{ knowledgeText.heroEyebrow }}</div>
      <h2><UiIcon name="database" />{{ knowledgeText.heroTitle }}</h2>
      <p>{{ knowledgeText.heroDescription }}</p>
    </div>
    <div class="hero-status-grid">
      <div class="stat-box primary-stat">
        <span><UiIcon name="archive" />{{ knowledgeText.targetKnowledgeBase }}</span>
        <strong>{{ knowledgeBaseId || uiText.notConfigured }}</strong>
      </div>
      <div class="stat-box">
        <span><UiIcon name="file" />{{ knowledgeText.selectedFile }}</span>
        <strong>{{ selectedFileName }}</strong>
      </div>
    </div>
  </article>
</template>
