import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({ baseURL: API_BASE })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('vyntra_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('vyntra_token')
      localStorage.removeItem('vyntra_user')
      window.location.href = '/login'
    }
    if (!error.response) {
      error.backendDown = true
      error.message = 'Backend not reachable. Please start Spring Boot on port 8091.'
    }
    return Promise.reject(error)
  }
)

export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
}

export const tripAPI = {
  plan:       (data)              => api.post('/trips/plan', data),
  getHistory: ()                  => api.get('/trips/history'),
  getTrip:    (id)                => api.get(`/trips/${id}`),
  save:       (id)                => api.post(`/trips/${id}/save`),
  morePlaces: (id, category, offset) =>
    api.get(`/trips/${id}/more-places?category=${category}&offset=${offset || 0}`),
}

export const rescueAPI = {
  replan: (data) => api.post('/rescue/replan', data),
}

export const weatherAPI = {
  get: (location) => api.get(`/weather?location=${encodeURIComponent(location)}`),
}

export const placeAPI = {
  search: (query, lat, lng) =>
    api.get(`/places/search?query=${encodeURIComponent(query)}&lat=${lat}&lng=${lng}`),
}

export const chatAPI = {
  send: (message, tripId) => api.post('/chat', { message, tripId }),
}

export const healthAPI = {
  check: () => api.get('/health'),
}

export default api
