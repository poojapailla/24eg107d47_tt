import axios from 'axios'

/** Base URL of the Spring Boot API. Configure it with the VITE_API_BASE_URL env variable. */
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
).replace(/\/+$/, '')

const api = axios.create({
  baseURL: API_BASE_URL,
  // Render's free tier spins the backend down after inactivity; the first request
  // can take ~50s to wake it up, so allow a generous timeout.
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

// Turns every failure into a plain Error with a readable, secret-free message.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED'
        ? 'Request timed out. Is the backend running?'
        : error.message) ||
      'Request failed'
    return Promise.reject(new Error(message))
  },
)

export default api
