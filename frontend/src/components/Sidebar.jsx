import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Sidebar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  
  const handleLogout = () => { logout(); navigate('/') }

  const links = [
    { to: '/dashboard',    icon: '⌂',  label: 'Dashboard' },
    { to: '/plan',         icon: '◎',  label: 'Plan Trip' },
    { to: '/mood-planner', icon: '🧠', label: 'AI Mood Planner' },
    { to: '/rescue',       icon: '⚡', label: 'Rescue Mode' },
    { to: '/history',      icon: '☰',  label: 'History' },
    { to: '/profile',      icon: '◉',  label: 'Profile' },
  ]

  return (
    <aside className="sidebar">
      {/* Logo */}
      <div className="flex items-center gap-3 px-2 mb-10 mt-2">
        <div className="w-8 h-8 rounded-xl bg-teal-600 flex items-center justify-center text-white font-black text-sm shadow-md shadow-teal-600/20">V</div>
        <span className="heading text-xl">VYNTRA</span>
      </div>

      {/* User */}
      {user && (
        <div className="card-inner mb-8 mx-1">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-slate-200 flex items-center justify-center text-teal-700 text-sm font-bold">
              {user.name?.charAt(0)?.toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-slate-800 truncate">{user.name}</p>
              <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
            </div>
          </div>
        </div>
      )}

      {/* Nav */}
      <nav className="flex flex-col gap-1.5 flex-1">
        <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider px-4 mb-2">Main Menu</div>
        {links.map(link => (
          <NavLink key={link.to} to={link.to}
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}>
            <span className="text-lg opacity-80">{link.icon}</span>
            <span className="font-medium">{link.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Logout */}
      <button onClick={handleLogout} className="sidebar-link text-red-600 hover:text-red-700 hover:bg-red-50 mt-auto">
        <span className="text-lg opacity-80">→</span>
        <span className="font-medium">Logout</span>
      </button>
    </aside>
  )
}
