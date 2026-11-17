export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
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

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin'
  })
  return handleResponse<T>(response, 'GET', path)
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify(body)
  })
  return handleResponse<T>(response, 'POST', path)
}

export async function apiUpload<T>(path: string, file: File): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'same-origin',
    body: form
  })
  return handleResponse<T>(response, 'POST', path)
}
