import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { authAPI } from '../services/api'

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.email || !form.password) { setError('All fields are required'); return }
    setLoading(true)
    try {
      const res = await authAPI.login(form)
      login(res.data)
      navigate('/dashboard')
    } catch (err) {
      if (err.backendDown) {
        setError('Backend not reachable. Please start Spring Boot on port 8091.')
      } else {
        setError(err.response?.data?.message || 'Login failed. Please check credentials.')
      }
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-slate-50">
      <div className="card p-10 w-full max-w-md slide-up">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="w-10 h-10 rounded-xl bg-teal-600 flex items-center justify-center text-white font-black text-lg shadow-md shadow-teal-600/20">V</div>
            <span className="heading text-2xl">VYNTRA</span>
          </div>
          <p className="text-sm text-slate-500">Welcome back to intelligent travel</p>
        </div>

        {error && <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-xl mb-6 text-sm flex items-center gap-2">
            <span className="font-bold">!</span> {error}
        </div>}

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">Email Address</label>
            <input id="login-email" type="email" className="input-field bg-slate-50" placeholder="you@example.com"
              value={form.email} onChange={e => setForm({...form, email: e.target.value})} />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">Password</label>
            <input id="login-password" type="password" className="input-field bg-slate-50" placeholder="••••••••"
              value={form.password} onChange={e => setForm({...form, password: e.target.value})} />
          </div>
          <button id="login-submit" type="submit" className="btn-primary w-full mt-2 py-3" disabled={loading}>
            {loading ? <span className="spinner w-5 h-5 border-2"></span> : 'Sign In'}
          </button>
        </form>

        <p className="text-center text-sm text-slate-600 mt-8">
          Don't have an account? <Link to="/register" className="text-teal-600 hover:text-teal-700 font-semibold">Register here</Link>
        </p>
        <Link to="/" className="block text-center text-sm text-slate-400 mt-4 hover:text-slate-600 transition-colors">← Back to home</Link>
      </div>
    </div>
  )
}
