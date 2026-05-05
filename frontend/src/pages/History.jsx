import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { tripAPI } from '../services/api'

export default function History() {
  const [trips, setTrips] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    tripAPI.getHistory().then(r => setTrips(r.data)).catch(() => {}).finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="spinner-teal w-10 h-10 border-4"></div>
    </div>
  )

  return (
    <div className="max-w-3xl fade-in">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="heading text-2xl">Trip History</h1>
          <p className="text-sm text-slate-500 mt-0.5">All your past trips</p>
        </div>
        <button onClick={() => navigate('/plan')} className="btn-primary text-sm">+ New Trip</button>
      </div>

      {trips.length === 0 ? (
        <div className="card p-14 text-center">
          <p className="text-3xl mb-3">📋</p>
          <h3 className="text-sm font-semibold text-slate-800 mb-1">No trips yet</h3>
          <p className="text-xs text-slate-500 mb-5">Plan your first trip!</p>
          <button onClick={() => navigate('/plan')} className="btn-primary text-sm">Plan a Trip</button>
        </div>
      ) : (
        <div className="space-y-3">
          {trips.map((t, i) => (
            <div key={t.id} onClick={() => navigate(`/trip/${t.id}`)}
              className="card px-5 py-4 cursor-pointer hover:border-teal-300 slide-in" style={{ animationDelay: `${i * 0.04}s` }}>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-9 h-9 rounded-lg bg-teal-50 flex items-center justify-center text-sm flex-shrink-0">🗺️</div>
                  <div className="min-w-0">
                    <h3 className="text-sm font-semibold text-slate-800 truncate">{t.startLocation} → {t.destination}</h3>
                    <div className="flex flex-wrap items-center gap-1.5 mt-1">
                      <span className="badge badge-teal capitalize">{t.mode || 'car'}</span>
                      <span className="badge badge-slate">₹{t.budget}</span>
                      <span className="badge badge-emerald">{t.placesCount} stops</span>
                      <span className="badge badge-slate capitalize">{t.travelType}</span>
                    </div>
                  </div>
                </div>
                <div className="text-right flex-shrink-0 ml-4">
                  <p className="text-[11px] text-slate-500">{new Date(t.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}</p>
                  {t.hasSavedItinerary && <span className="badge badge-emerald mt-0.5">Saved</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
