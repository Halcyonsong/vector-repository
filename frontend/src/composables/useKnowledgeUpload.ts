import { computed, onBeforeUnmount, ref, type Ref } from 'vue'
import { useAppConfig } from '../config/app-config'
import {
  deleteKnowledgeBase,
  getErrorKind,
  getKnowledgeBaseUploadTask,
  listKnowledgeBases,
  normalizeApiErrorMessage,
  uploadDocument
} from '../api'
import type { ErrorKind, KnowledgeBaseUploadTaskVO } from '../types'

const ACTIVE_UPLOAD_STATUSES = new Set(['pending', 'parsing', 'embedding'])
const FINISHED_UPLOAD_STATUSES = new Set(['completed', 'failed'])

interface UseKnowledgeUploadOptions {
  knowledgeBaseId: Ref<string>
  setError: (message: string, kind?: ErrorKind) => void
  clearError: () => void
}

export function useKnowledgeUpload(options: UseKnowledgeUploadOptions) {
  const appConfig = useAppConfig()
  const behaviorConfig = appConfig.behavior
  const knowledgeText = appConfig.labels.knowledge
  const uiText = appConfig.labels.ui
  const knowledgeBases = ref<string[]>([])
  const selectedFile = ref<File | null>(null)
  const uploadStatusText = ref('')
  const activeUploadTask = ref<KnowledgeBaseUploadTaskVO | null>(null)
  const isKnowledgeBaseListLoading = ref(false)
  const isSubmittingUpload = ref(false)
  const deletingKnowledgeBaseId = ref('')
  const uploadProgressPercent = ref(0)
  const activeProgressTaskId = ref('')
  let pollTimer: number | null = null

  const selectedFileName = computed(() => {
    return selectedFile.value ? selectedFile.value.name : uiText.noFileSelected
  })

  const hasKnowledgeBases = computed(() => {
    return knowledgeBases.value.length > 0
  })

  const hasActiveUploadTask = computed(() => {
    return activeUploadTask.value !== null && ACTIVE_UPLOAD_STATUSES.has(activeUploadTask.value.status)
  })

  const isUploading = computed(() => {
    return isSubmittingUpload.value || hasActiveUploadTask.value
  })

  function calculateUploadProgress(task: KnowledgeBaseUploadTaskVO): number {
    if (task.status === 'completed') {
      return 100
    }
    if (task.status === 'failed') {
      return uploadProgressPercent.value
    }
    if (!task.totalChunks || task.totalChunks <= 0) {
      return ACTIVE_UPLOAD_STATUSES.has(task.status) ? 8 : 0
    }

    return Math.min(99, Math.round((task.processedChunks / task.totalChunks) * 100))
  }

  function updateActiveUploadTask(task: KnowledgeBaseUploadTaskVO): void {
    if (task.taskId !== activeProgressTaskId.value) {
      activeProgressTaskId.value = task.taskId
      uploadProgressPercent.value = 0
    }

    activeUploadTask.value = task
    uploadProgressPercent.value = Math.max(uploadProgressPercent.value, calculateUploadProgress(task))
  }

  const uploadProgressText = computed(() => {
    const task = activeUploadTask.value
    if (!task) {
      return ''
    }
    if (task.status === 'completed') {
      return knowledgeText.taskCompletedMessage
    }
    if (task.status === 'failed') {
      return normalizeApiErrorMessage(task.errorMessage || knowledgeText.taskFailedMessage)
    }
    return task.message || knowledgeText.taskPendingMessage
  })

  function clearPollTimer(): void {
    if (pollTimer !== null) {
      window.clearTimeout(pollTimer)
      pollTimer = null
    }
  }

  function resetUploadProgress(): void {
    clearPollTimer()
    activeUploadTask.value = null
    activeProgressTaskId.value = ''
    uploadProgressPercent.value = 0
  }

  function scheduleUploadTaskPolling(taskId: string): void {
    clearPollTimer()
    pollTimer = window.setTimeout(() => {
      void pollUploadTask(taskId)
    }, behaviorConfig.uploadTaskPollIntervalMs)
  }

  function isTaskFinished(task: KnowledgeBaseUploadTaskVO): boolean {
    return FINISHED_UPLOAD_STATUSES.has(task.status)
  }

  function guardUploadLocked(): boolean {
    if (!hasActiveUploadTask.value && !isSubmittingUpload.value) {
      return false
    }

    options.setError(uiText.uploadTaskRunning, 'validation')
    return true
  }

  function selectFile(event: Event): void {
    const input = event.target as HTMLInputElement
    if (guardUploadLocked()) {
      input.value = ''
      return
    }

    selectedFile.value = input.files && input.files.length > 0 ? input.files[0] : null
  }

  function selectDroppedFile(file: File): void {
    if (guardUploadLocked()) {
      return
    }

    selectedFile.value = file
  }

  async function loadKnowledgeBases(): Promise<void> {
    isKnowledgeBaseListLoading.value = true

    try {
      knowledgeBases.value = await listKnowledgeBases()
    } catch (error) {
      options.setError(error instanceof Error ? error.message : knowledgeText.listFailed, getErrorKind(error))
    } finally {
      isKnowledgeBaseListLoading.value = false
    }
  }

  async function pollUploadTask(taskId: string): Promise<void> {
    try {
      const task = await getKnowledgeBaseUploadTask(taskId)
      updateActiveUploadTask(task)
      uploadStatusText.value = uploadProgressText.value

      if (!isTaskFinished(task)) {
        scheduleUploadTaskPolling(task.taskId)
        return
      }

      clearPollTimer()
      if (task.status === 'completed') {
        uploadStatusText.value = `${knowledgeText.taskCompletedMessage}: ${task.processedChunks}${uiText.uploadSuccessSuffix}`
        await loadKnowledgeBases()
        return
      }

      options.setError(normalizeApiErrorMessage(task.errorMessage || knowledgeText.taskFailedMessage), 'document')
    } catch (error) {
      clearPollTimer()
      options.setError(error instanceof Error ? error.message : uiText.uploadFailed, getErrorKind(error))
    }
  }

  async function uploadSelectedDocument(): Promise<void> {
    options.clearError()

    if (guardUploadLocked()) {
      return
    }
    uploadStatusText.value = ''

    if (!selectedFile.value) {
      options.setError(uiText.uploadSelectDocumentFirst, 'validation')
      return
    }
    if (!options.knowledgeBaseId.value.trim()) {
      options.setError(uiText.knowledgeBaseRequired, 'validation')
      return
    }

    isSubmittingUpload.value = true

    try {
      const targetKnowledgeBaseId = options.knowledgeBaseId.value.trim()
      resetUploadProgress()
      const task = await uploadDocument(targetKnowledgeBaseId, selectedFile.value)
      updateActiveUploadTask(task)
      uploadStatusText.value = task.message || knowledgeText.taskPendingMessage
      selectedFile.value = null

      if (isTaskFinished(task)) {
        await pollUploadTask(task.taskId)
        return
      }

      scheduleUploadTaskPolling(task.taskId)
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.uploadFailed, getErrorKind(error))
    } finally {
      isSubmittingUpload.value = false
    }
  }

  async function deleteExistingKnowledgeBase(knowledgeBaseId: string): Promise<void> {
    const normalizedKnowledgeBaseId = knowledgeBaseId.trim()
    if (!normalizedKnowledgeBaseId) {
      return
    }

    if (guardUploadLocked()) {
      return
    }

    const confirmed = window.confirm(`${knowledgeText.deleteConfirmPrefix}${normalizedKnowledgeBaseId}${knowledgeText.deleteConfirmSuffix}`)
    if (!confirmed) {
      return
    }

    options.clearError()
    deletingKnowledgeBaseId.value = normalizedKnowledgeBaseId

    try {
      await deleteKnowledgeBase(normalizedKnowledgeBaseId)
      if (options.knowledgeBaseId.value === normalizedKnowledgeBaseId) {
        options.knowledgeBaseId.value = ''
      }
      await loadKnowledgeBases()
    } catch (error) {
      options.setError(error instanceof Error ? error.message : knowledgeText.deleteFailed, getErrorKind(error))
    } finally {
      deletingKnowledgeBaseId.value = ''
    }
  }

  onBeforeUnmount(() => {
    clearPollTimer()
  })

  return {
    knowledgeBases,
    selectedFileName,
    uploadStatusText,
    activeUploadTask,
    uploadProgressPercent,
    uploadProgressText,
    isKnowledgeBaseListLoading,
    isUploading,
    deletingKnowledgeBaseId,
    hasKnowledgeBases,
    hasActiveUploadTask,
    selectFile,
    selectDroppedFile,
    loadKnowledgeBases,
    uploadSelectedDocument,
    deleteExistingKnowledgeBase
  }
}
