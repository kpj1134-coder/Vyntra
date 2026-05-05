import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import PlanTrip from './pages/PlanTrip'
import TripResult from './pages/TripResult'
import Rescue from './pages/Rescue'
import History from './pages/History'
import Profile from './pages/Profile'
import Sidebar from './components/Sidebar'
import Chatbot from './components/Chatbot'

function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()
  if (loading) return (
    <div className="flex items-center justify-center min-h-screen bg-[#f8fafc]">
      <div className="spinner-teal w-10 h-10 border-4"></div>
    </div>
  )
  return isAuthenticated ? children : <Navigate to="/login" />
}

function AppLayout({ children }) {
  return (
    <div className="flex bg-[#f8fafc]">
      <Sidebar />
      <div className="main-content">{children}</div>
      <Chatbot />
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/dashboard" element={<ProtectedRoute><AppLayout><Dashboard /></AppLayout></ProtectedRoute>} />
      <Route path="/plan" element={<ProtectedRoute><AppLayout><PlanTrip /></AppLayout></ProtectedRoute>} />
      <Route path="/trip/:id" element={<ProtectedRoute><AppLayout><TripResult /></AppLayout></ProtectedRoute>} />
      <Route path="/result/:id" element={<ProtectedRoute><AppLayout><TripResult /></AppLayout></ProtectedRoute>} />
      <Route path="/rescue" element={<ProtectedRoute><AppLayout><Rescue /></AppLayout></ProtectedRoute>} />
      <Route path="/history" element={<ProtectedRoute><AppLayout><History /></AppLayout></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><AppLayout><Profile /></AppLayout></ProtectedRoute>} />
    </Routes>
  )
}
