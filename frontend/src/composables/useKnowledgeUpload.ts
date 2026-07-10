import { computed, ref, type Ref } from 'vue'
import { useAppConfig } from '../config/app-config'
import { deleteKnowledgeBase, getErrorKind, listKnowledgeBases, uploadDocument } from '../api'
import type { ErrorKind } from '../types'

interface UseKnowledgeUploadOptions {
  knowledgeBaseId: Ref<string>
  setError: (message: string, kind?: ErrorKind) => void
  clearError: () => void
}

export function useKnowledgeUpload(options: UseKnowledgeUploadOptions) {
  const appConfig = useAppConfig()
  const knowledgeText = appConfig.labels.knowledge
  const uiText = appConfig.labels.ui
  const knowledgeBases = ref<string[]>([])
  const selectedFile = ref<File | null>(null)
  const uploadStatusText = ref('')
  const isKnowledgeBaseListLoading = ref(false)
  const isUploading = ref(false)
  const deletingKnowledgeBaseId = ref('')

  const selectedFileName = computed(() => {
    return selectedFile.value ? selectedFile.value.name : uiText.noFileSelected
  })

  const hasKnowledgeBases = computed(() => {
    return knowledgeBases.value.length > 0
  })

  function selectFile(event: Event): void {
    const input = event.target as HTMLInputElement
    selectedFile.value = input.files && input.files.length > 0 ? input.files[0] : null
  }

  function selectDroppedFile(file: File): void {
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

  async function uploadSelectedDocument(): Promise<void> {
    options.clearError()
    uploadStatusText.value = ''

    if (!selectedFile.value) {
      options.setError(uiText.uploadSelectDocumentFirst, 'validation')
      return
    }
    if (!options.knowledgeBaseId.value.trim()) {
      options.setError(uiText.knowledgeBaseRequired, 'validation')
      return
    }

    isUploading.value = true

    try {
      const targetKnowledgeBaseId = options.knowledgeBaseId.value.trim()
      const chunkCount = await uploadDocument(targetKnowledgeBaseId, selectedFile.value)
      uploadStatusText.value = `${uiText.uploadSuccessPrefix}${chunkCount}${uiText.uploadSuccessSuffix}`
      selectedFile.value = null
      await loadKnowledgeBases()
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.uploadFailed, getErrorKind(error))
    } finally {
      isUploading.value = false
    }
  }

  async function deleteExistingKnowledgeBase(knowledgeBaseId: string): Promise<void> {
    const normalizedKnowledgeBaseId = knowledgeBaseId.trim()
    if (!normalizedKnowledgeBaseId) {
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

  return {
    knowledgeBases,
    selectedFileName,
    uploadStatusText,
    isKnowledgeBaseListLoading,
    isUploading,
    deletingKnowledgeBaseId,
    hasKnowledgeBases,
    selectFile,
    selectDroppedFile,
    loadKnowledgeBases,
    uploadSelectedDocument,
    deleteExistingKnowledgeBase
  }
}
