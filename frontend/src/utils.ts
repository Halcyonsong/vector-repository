import { getAppConfig, type AppBehaviorConfig } from './config/app-config'
import type { SessionGroup, SessionVO } from './types'

export function formatSessionId(sessionId: string): string {
  if (!sessionId) {
    return '未选择'
  }
  if (sessionId.length <= 12) {
    return sessionId
  }
  return `${sessionId.slice(0, 8)}...${sessionId.slice(-4)}`
}

export function groupSessionsByTime(sessions: SessionVO[]): SessionGroup[] {
  const sessionGroupLabels = getAppConfig().labels.sessionGroups
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const weekStart = new Date(todayStart)
  weekStart.setDate(todayStart.getDate() - 6)

  const todayList: SessionVO[] = []
  const weekList: SessionVO[] = []
  const olderList: SessionVO[] = []

  const sortedSessions = [...sessions].sort((left, right) => {
    return new Date(right.updateTime).getTime() - new Date(left.updateTime).getTime()
  })

  sortedSessions.forEach((session) => {
    const updateTime = new Date(session.updateTime)
    if (updateTime >= todayStart) {
      todayList.push(session)
      return
    }
    if (updateTime >= weekStart) {
      weekList.push(session)
      return
    }
    olderList.push(session)
  })

  const groups: SessionGroup[] = []
  if (todayList.length > 0) {
    groups.push({ label: sessionGroupLabels.today, items: todayList })
  }
  if (weekList.length > 0) {
    groups.push({ label: sessionGroupLabels.week, items: weekList })
  }
  if (olderList.length > 0) {
    groups.push({ label: sessionGroupLabels.older, items: olderList })
  }
  return groups
}

export function resolveTopK(inputValue: string, config: AppBehaviorConfig = getAppConfig().behavior): number | null {
  const trimmed = inputValue.trim()
  if (!trimmed) {
    return config.defaultTopK
  }

  const parsed = Number(trimmed)
  if (
    !Number.isInteger(parsed) ||
    parsed < config.topKRange.min ||
    parsed > config.topKRange.max
  ) {
    return null
  }

  return parsed
}

export function resolveSimilarityThreshold(
  inputValue: string,
  config: AppBehaviorConfig = getAppConfig().behavior
): number | null {
  const trimmed = inputValue.trim()
  if (!trimmed) {
    return config.defaultSimilarityThreshold
  }

  const parsed = Number(trimmed)
  if (
    Number.isNaN(parsed) ||
    parsed < config.similarityThresholdRange.min ||
    parsed > config.similarityThresholdRange.max
  ) {
    return null
  }

  return parsed
}

export function buildTopKErrorMessage(config: AppBehaviorConfig = getAppConfig().behavior): string {
  return `topK 请输入 ${config.topKRange.min} 到 ${config.topKRange.max} 的整数，留空则默认 ${config.defaultTopK}`
}

export function buildSimilarityThresholdErrorMessage(config: AppBehaviorConfig = getAppConfig().behavior): string {
  return `similarityThreshold 请输入 ${config.similarityThresholdRange.min} 到 ${config.similarityThresholdRange.max} 之间的小数，留空则默认 ${config.defaultSimilarityThreshold}`
}
