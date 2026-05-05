import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('vyntra_token'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const savedToken = localStorage.getItem('vyntra_token')
    const savedUser = localStorage.getItem('vyntra_user')
    if (savedToken && savedUser) {
      setToken(savedToken)
      try { setUser(JSON.parse(savedUser)) } catch(e) { logout() }
    }
    setLoading(false)
  }, [])

  const login = (authResponse) => {
    const { token, name, email, userId } = authResponse
    localStorage.setItem('vyntra_token', token)
    localStorage.setItem('vyntra_user', JSON.stringify({ name, email, userId }))
    setToken(token)
    setUser({ name, email, userId })
  }

  const logout = () => {
    localStorage.removeItem('vyntra_token')
    localStorage.removeItem('vyntra_user')
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
