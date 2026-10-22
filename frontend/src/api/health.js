import api from './client'

/** GET /api/health */
export const getHealth = () => api.get('/health')

/** GET /api/health/db */
export const getDbHealth = () => api.get('/health/db')
