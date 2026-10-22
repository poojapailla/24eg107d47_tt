import { useCallback, useEffect, useState } from 'react'
import { API_BASE_URL } from './api/client'
import { getDbHealth, getHealth } from './api/health'
import './App.css'

const IDLE = { state: 'idle', message: 'Not tested yet' }

export default function App() {
  const [backend, setBackend] = useState(IDLE)
  const [database, setDatabase] = useState(IDLE)
  const [loading, setLoading] = useState(false)
  const [responses, setResponses] = useState({ health: null, database: null })

  const testConnection = useCallback(async () => {
    setLoading(true)
    setBackend({ state: 'loading', message: 'Checking backend...' })
    setDatabase({ state: 'loading', message: 'Checking database...' })
    setResponses({ health: null, database: null })

    let healthPayload = null

    try {
      const healthRes = await getHealth()
      healthPayload = healthRes.data
      setBackend({
        state: 'success',
        message: healthPayload?.data?.message || 'Backend is running',
      })
    } catch (error) {
      setBackend({ state: 'error', message: error.message })
    }

    try {
      const dbRes = await getDbHealth()
      const connected = dbRes.data?.data?.connected === true
      setDatabase({
        state: connected ? 'success' : 'error',
        message: dbRes.data?.data?.message || (connected ? 'Database connected' : 'Database unreachable'),
      })
      setResponses({ health: healthPayload, database: dbRes.data })
    } catch (error) {
      setDatabase({ state: 'error', message: error.message })
      setResponses({ health: healthPayload, database: null })
    } finally {
      setLoading(false)
    }
  }, [])

  // Auto test once when the page loads (deferred so the effect stays side-effect free).
  useEffect(() => {
    const timer = setTimeout(testConnection, 0)
    return () => clearTimeout(timer)
  }, [testConnection])

  return (
    <div className="page">
      <header className="header">
        <h1>Full Stack Lab Exam Starter</h1>
        <p>React + Vite frontend, Spring Boot backend, MySQL (Aiven) database.</p>
      </header>

      <main className="content">
        <section className="cards">
          <StatusCard title="Frontend" status="success" message="Running (React + Vite)" />
          <StatusCard title="Backend Health" status={backend.state} message={backend.message} />
          <StatusCard title="Database (MySQL)" status={database.state} message={database.message} />
        </section>

        <section className="toolbar">
          <button type="button" onClick={testConnection} disabled={loading}>
            {loading ? 'Testing...' : 'Test Backend Connection'}
          </button>
          <code className="api-url">API URL: {API_BASE_URL}</code>
        </section>

        {loading && <p className="notice loading">Contacting the backend, please wait...</p>}

        {backend.state === 'error' && (
          <p className="notice error">
            Backend unreachable: {backend.message}. Start the backend on port 8080 or update VITE_API_BASE_URL.
          </p>
        )}

        {backend.state === 'success' && database.state === 'success' && (
          <p className="notice success">All checks passed. Backend and database are reachable.</p>
        )}

        {(responses.health || responses.database) && (
          <section className="response">
            <h2>Backend response</h2>
            <pre>{JSON.stringify(responses, null, 2)}</pre>
          </section>
        )}
      </main>
    </div>
  )
}

function StatusCard({ title, status, message }) {
  const label =
    status === 'success'
      ? 'ONLINE'
      : status === 'error'
        ? 'OFFLINE'
        : status === 'loading'
          ? 'CHECKING'
          : 'IDLE'

  return (
    <article className={`card ${status}`}>
      <div className="card-top">
        <h3>{title}</h3>
        <span className="badge">{label}</span>
      </div>
      <p className="card-message">{message}</p>
    </article>
  )
}
