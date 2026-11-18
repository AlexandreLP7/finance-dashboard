export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

type TokenProvider = () => string | null
type RefreshHandler = () => Promise<boolean>

let accessTokenProvider: TokenProvider = () => null
let refreshHandler: RefreshHandler | null = null

export function setAccessTokenProvider(provider: TokenProvider): void {
  accessTokenProvider = provider
}

export function setRefreshHandler(handler: RefreshHandler | null): void {
  refreshHandler = handler
}

function withAuthHeaders(headers: Record<string, string>): Record<string, string> {
  const token = accessTokenProvider()
  return token ? { ...headers, Authorization: `Bearer ${token}` } : headers
}

async function handleResponse<T>(response: Response, method: string, path: string): Promise<T> {
  if (!response.ok) {
    throw new ApiError(response.status, `${method} ${path} a echoue (${response.status})`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  if (typeof response.text === 'function') {
    const text = await response.text()
    return (text ? JSON.parse(text) : undefined) as T
  }
  return (await response.json()) as T
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const method = init.method ?? 'GET'
  const headers = withAuthHeaders((init.headers as Record<string, string>) ?? {})
  const response = await fetch(path, { ...init, headers, credentials: 'same-origin' })
  if (response.status === 401 && !path.startsWith('/api/auth/') && refreshHandler) {
    const refreshed = await refreshHandler()
    if (refreshed) {
      const retryHeaders = withAuthHeaders((init.headers as Record<string, string>) ?? {})
      const retry = await fetch(path, { ...init, headers: retryHeaders, credentials: 'same-origin' })
      return handleResponse<T>(retry, method, path)
    }
  }
  return handleResponse<T>(response, method, path)
}

export async function apiGet<T>(path: string): Promise<T> {
  return request<T>(path, { headers: { Accept: 'application/json' } })
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body)
  })
}

export async function apiUpload<T>(path: string, file: File): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  return request<T>(path, { method: 'POST', body: form })
}
