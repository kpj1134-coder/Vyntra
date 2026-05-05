import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'

export default function Profile() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="max-w-2xl mx-auto fade-in">
      <div className="mb-8">
        <h1 className="heading text-3xl mb-2 text-slate-800">Profile</h1>
        <p className="text-slate-500">Your account details</p>
      </div>

      <div className="card p-8 mb-6">
        <div className="flex items-center gap-5 mb-8">
          <div className="w-16 h-16 rounded-2xl bg-teal-600 flex items-center justify-center text-white text-2xl font-bold shadow-md shadow-teal-600/20">
            {user?.name?.charAt(0)?.toUpperCase() || '?'}
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-800">{user?.name || 'Traveler'}</h2>
            <p className="text-slate-500">{user?.email || ''}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="card-inner">
            <p className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Name</p>
            <p className="text-sm font-semibold text-slate-700 mt-1">{user?.name}</p>
          </div>
          <div className="card-inner">
            <p className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Email</p>
            <p className="text-sm font-semibold text-slate-700 mt-1">{user?.email}</p>
          </div>
          <div className="card-inner">
            <p className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">User ID</p>
            <p className="text-sm font-medium text-slate-500 mt-1 truncate">{user?.userId}</p>
          </div>
          <div className="card-inner">
            <p className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Status</p>
            <p className="text-sm font-bold text-emerald-600 mt-1">Active</p>
          </div>
        </div>
      </div>

      <div className="card p-6">
        <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4">Quick Actions</h3>
        <div className="space-y-2">
          <button onClick={() => navigate('/plan')} className="w-full text-left card-inner px-4 py-3 text-sm font-medium text-slate-600 hover:text-teal-700 hover:border-teal-200 hover:bg-teal-50 transition-all flex items-center gap-3">
            <span className="opacity-60 text-lg">◎</span> Plan a New Trip
          </button>
          <button onClick={() => navigate('/history')} className="w-full text-left card-inner px-4 py-3 text-sm font-medium text-slate-600 hover:text-teal-700 hover:border-teal-200 hover:bg-teal-50 transition-all flex items-center gap-3">
            <span className="opacity-60 text-lg">☰</span> View Trip History
          </button>
          <button onClick={handleLogout} className="w-full text-left card-inner px-4 py-3 text-sm font-medium text-red-500 hover:text-red-700 hover:border-red-200 hover:bg-red-50 transition-all flex items-center gap-3">
            <span className="opacity-60 text-lg">→</span> Logout
          </button>
        </div>
      </div>
    </div>
  )
}
