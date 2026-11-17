export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin'
  })
  if (!response.ok) {
    throw new ApiError(response.status, `GET ${path} a echoue (${response.status})`)
  }
  return (await response.json()) as T
}
