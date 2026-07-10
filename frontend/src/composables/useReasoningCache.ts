interface ReasoningCachePayload {
  sessionId: string
  content: string
  cachedAt: number
}

interface UseReasoningCacheOptions {
  cacheKey: string
  ttlMs: number
}

export function useReasoningCache(options: UseReasoningCacheOptions) {
  function read(sessionId: string): string {
    if (!sessionId) {
      return ''
    }

    try {
      const rawPayload = window.localStorage.getItem(options.cacheKey)
      if (!rawPayload) {
        return ''
      }

      const payload = JSON.parse(rawPayload) as Partial<ReasoningCachePayload>
      const isExpired = typeof payload.cachedAt !== 'number' || Date.now() - payload.cachedAt > options.ttlMs
      const isDifferentSession = payload.sessionId !== sessionId
      if (isExpired) {
        window.localStorage.removeItem(options.cacheKey)
        return ''
      }
      if (isDifferentSession || typeof payload.content !== 'string') {
        return ''
      }

      return payload.content
    } catch {
      return ''
    }
  }

  function write(sessionId: string, content: string): void {
    if (!sessionId || !content) {
      return
    }

    try {
      const payload: ReasoningCachePayload = {
        sessionId,
        content,
        cachedAt: Date.now()
      }
      window.localStorage.setItem(options.cacheKey, JSON.stringify(payload))
    } catch {
      // localStorage can be unavailable in restricted browser modes.
    }
  }

  function clear(): void {
    try {
      window.localStorage.removeItem(options.cacheKey)
    } catch {
      // Ignore storage failures; the live conversation state remains authoritative.
    }
  }

  return {
    read,
    write,
    clear
  }
}
