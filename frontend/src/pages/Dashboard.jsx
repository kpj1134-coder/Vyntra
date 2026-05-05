import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { tripAPI } from '../services/api'

export default function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [recentTrips, setRecentTrips] = useState([])
  const [totalTrips, setTotalTrips] = useState(0)
  const [savedCount, setSavedCount] = useState(0)

  useEffect(() => {
    tripAPI.getHistory().then(res => {
      const trips = res.data || []
      setTotalTrips(trips.length)
      setSavedCount(trips.filter(t => t.hasSavedItinerary).length)
      setRecentTrips(trips.slice(0, 3))
    }).catch(console.error)
  }, [])

  return (
    <div className="max-w-6xl mx-auto fade-in">
      <header className="mb-10 flex justify-between items-end">
        <div>
          <h1 className="heading text-3xl mb-2">Welcome back, {user?.name?.split(' ')[0] || 'Traveler'} 👋</h1>
          <p className="text-slate-500">Ready for your next adventure?</p>
        </div>
        <button onClick={() => navigate('/plan')} className="btn-primary">
          <span className="mr-2">+</span> New Trip Plan
        </button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
        <div className="card p-6 bg-gradient-to-br from-teal-500 to-teal-700 border-none" style={{ color: '#ffffff' }}>
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center text-xl mb-4">🗺️</div>
          <h3 className="text-xl font-bold mb-1" style={{ fontFamily: 'Outfit', color: '#ffffff' }}>Plan a Trip</h3>
          <p className="text-sm mb-4" style={{ color: '#ccfbf1' }}>Discover optimized stops along your route.</p>
          <button onClick={() => navigate('/plan')} className="w-full font-bold py-2.5 rounded-xl hover:bg-teal-50 transition-colors" style={{ backgroundColor: '#ffffff', color: '#0f766e' }}>Start Planning</button>
        </div>

        <div className="card p-6 bg-gradient-to-br from-amber-400 to-orange-500 border-none" style={{ color: '#ffffff' }}>
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center text-xl mb-4">⚡</div>
          <h3 className="text-xl font-bold mb-1" style={{ fontFamily: 'Outfit', color: '#ffffff' }}>Rescue Mode</h3>
          <p className="text-sm mb-4" style={{ color: '#fff7ed' }}>Emergency replanning based on current constraints.</p>
          <button onClick={() => navigate('/rescue')} className="w-full font-bold py-2.5 rounded-xl hover:bg-orange-50 transition-colors" style={{ backgroundColor: '#ffffff', color: '#ea580c' }}>Trigger Rescue</button>
        </div>

        <div className="card p-6">
          <div className="w-10 h-10 bg-slate-100 text-slate-600 rounded-xl flex items-center justify-center text-xl mb-4">📊</div>
          <h3 className="text-xl font-bold mb-1 text-slate-800" style={{ fontFamily: 'Outfit' }}>Trip Stats</h3>
          <div className="mt-4 flex flex-col gap-3">
            <div className="flex justify-between items-center">
              <span className="text-sm text-slate-500">Total Trips Planned</span>
              <span className="font-bold text-teal-600 text-lg">{totalTrips}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-slate-500">Saved Itineraries</span>
              <span className="font-bold text-slate-700 text-lg">{savedCount}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="mb-6 flex justify-between items-center">
        <h2 className="heading text-xl">Recent Trips</h2>
        <button onClick={() => navigate('/history')} className="text-sm font-semibold text-teal-600 hover:text-teal-700">View All →</button>
      </div>

      {recentTrips.length === 0 ? (
        <div className="card-inner py-12 flex flex-col items-center justify-center border-dashed border-2">
          <span className="text-4xl mb-3 opacity-50">🏕️</span>
          <p className="text-slate-500 font-medium">No trips planned yet.</p>
          <button onClick={() => navigate('/plan')} className="text-teal-600 text-sm mt-2 font-semibold hover:underline">Create your first itinerary</button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {recentTrips.map(trip => (
            <div key={trip.id} onClick={() => navigate(`/trip/${trip.id}`)} className="card p-5 cursor-pointer hover:border-teal-300 group">
              <div className="flex justify-between items-start mb-4">
                <div className="badge badge-teal">{trip.mode || 'car'}</div>
                <span className="text-[10px] text-slate-400 font-medium">{new Date(trip.createdAt).toLocaleDateString()}</span>
              </div>
              <h3 className="font-bold text-slate-800 mb-1 flex items-center gap-2">
                <span className="truncate max-w-[100px]">{trip.startLocation}</span>
                <span className="text-slate-300">→</span>
                <span className="truncate max-w-[100px]">{trip.destination}</span>
              </h3>
              <p className="text-xs text-slate-500 mb-4">{trip.placesCount} stops • ₹{trip.budget}</p>
              <div className="flex justify-end opacity-0 group-hover:opacity-100 transition-opacity">
                <span className="text-teal-600 text-xs font-semibold">View Details →</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
