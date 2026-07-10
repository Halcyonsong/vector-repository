import { computed, ref } from 'vue'
import { createSession, deleteSession, getErrorKind, getSession, listSessions, renameSession } from '../api'
import { useAppConfig } from '../config/app-config'
import { CHAT_STATUS } from '../constants'
import { formatSessionId, groupSessionsByTime } from '../utils'
import type { ErrorKind, SessionGroup, SessionVO } from '../types'

interface UseSessionsOptions {
  setError: (message: string, kind?: ErrorKind) => void
}

export function useSessions(options: UseSessionsOptions) {
  const appConfig = useAppConfig()
  const behaviorConfig = appConfig.behavior
  const uiText = appConfig.labels.ui
  const sessions = ref<SessionVO[]>([])
  const activeSessionId = ref('')

  const activeSession = computed(() => {
    return sessions.value.find((item) => item.sessionId === activeSessionId.value) ?? null
  })

  const sessionGroups = computed<SessionGroup[]>(() => {
    return groupSessionsByTime(sessions.value)
  })

  const activeSessionDisplayId = computed(() => {
    return formatSessionId(activeSession.value?.sessionId || '')
  })

  const activeSessionTitle = computed(() => {
    return activeSession.value?.title || uiText.untitledSession
  })

  function findSession(sessionId: string): SessionVO | undefined {
    return sessions.value.find((item) => item.sessionId === sessionId)
  }

  function ensureActiveSessionStillExists(): void {
    const exists = sessions.value.some((item) => item.sessionId === activeSessionId.value)
    if (!exists) {
      activeSessionId.value = sessions.value[0]?.sessionId ?? ''
    }
  }

  async function loadSessions(): Promise<void> {
    sessions.value = await listSessions()
    ensureActiveSessionStillExists()
  }

  async function createAndSelectSession(): Promise<SessionVO> {
    const session = await createSession()
    sessions.value.unshift(session)
    activeSessionId.value = session.sessionId
    return session
  }

  async function selectSession(sessionId: string): Promise<void> {
    activeSessionId.value = sessionId
    const latestSession = await getSession(sessionId)
    if (!latestSession) {
      await loadSessions()
      return
    }

    sessions.value = sessions.value.map((session) => {
      return session.sessionId === sessionId ? latestSession : session
    })
  }

  async function renameExistingSession(sessionId: string): Promise<void> {
    const session = findSession(sessionId)
    if (!session) {
      return
    }

    const nextTitle = window.prompt(uiText.sessionRenamePrompt, session.title || uiText.untitledSession)
    if (nextTitle === null) {
      return
    }

    const trimmedTitle = nextTitle.trim()
    if (!trimmedTitle) {
      options.setError(uiText.sessionTitleEmpty, 'validation')
      return
    }
    if (trimmedTitle.length > behaviorConfig.sessionTitleMaxLength) {
      options.setError(uiText.sessionTitleTooLong, 'validation')
      return
    }

    try {
      const updatedSession = await renameSession({
        sessionId: session.sessionId,
        title: trimmedTitle
      })
      sessions.value = sessions.value.map((currentSession) => {
        return currentSession.sessionId === updatedSession.sessionId ? updatedSession : currentSession
      })
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.sessionRenameFailed, getErrorKind(error))
    }
  }

  async function deleteExistingSession(sessionId: string): Promise<boolean> {
    const session = findSession(sessionId)
    if (!session) {
      return false
    }

    const confirmed = window.confirm(`${uiText.sessionDeleteConfirmPrefix}${session.title}${uiText.sessionDeleteConfirmSuffix}`)
    if (!confirmed) {
      return false
    }

    try {
      const deletedSessionId = session.sessionId
      const deleted = await deleteSession(deletedSessionId)
      if (!deleted) {
        options.setError(uiText.sessionDeleteFailed)
        return false
      }

      sessions.value = sessions.value.filter((currentSession) => {
        return currentSession.sessionId !== deletedSessionId
      })
      ensureActiveSessionStillExists()
      return !activeSessionId.value
    } catch (error) {
      options.setError(error instanceof Error ? error.message : uiText.sessionDeleteFailed, getErrorKind(error))
      return false
    }
  }

  return {
    sessions,
    activeSessionId,
    activeSession,
    activeSessionDisplayId,
    activeSessionTitle,
    sessionGroups,
    loadSessions,
    createAndSelectSession,
    selectSession,
    renameExistingSession,
    deleteExistingSession,
    defaultStatus: CHAT_STATUS.idle
  }
}
