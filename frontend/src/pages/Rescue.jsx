import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { rescueAPI } from '../services/api'

export default function Rescue() {
  const [form, setForm] = useState({
    currentLocation: '', destination: '', issue: 'rain', remainingBudget: 1000, 
    remainingTime: 4, currentEnergy: 'low', interests: []
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const navigate = useNavigate()

  const interestsList = [
    { id: 'food', label: 'Food', icon: '🍲' },
    { id: 'rest', label: 'Rest', icon: '🛏️' },
    { id: 'emergency', label: 'Emergency', icon: '🏥' },
    { id: 'shopping', label: 'Shopping', icon: '🛍️' },
    { id: 'temple', label: 'Indoor/Temple', icon: '🏛️' }
  ]

  const toggleInterest = (id) => {
    setForm(prev => {
      const ints = prev.interests.includes(id) 
        ? prev.interests.filter(i => i !== id)
        : [...prev.interests, id]
      return { ...prev, interests: ints }
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.currentLocation) { setError('Current location is required'); return }
    const submitData = { ...form }
    if (submitData.interests.length === 0) submitData.interests = ['food', 'rest', 'emergency']
    
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await rescueAPI.replan(submitData)
      if (res.data.tripId) {
        navigate(`/trip/${res.data.tripId}`)
      } else {
        setResult(res.data)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to trigger rescue mode. Check connection.')
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-3xl mx-auto fade-in pb-12">
      <div className="text-center mb-10">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-orange-100 text-orange-600 text-3xl mb-4">🚨</div>
        <h1 className="heading text-3xl mb-2 text-slate-800">Rescue Mode</h1>
        <p className="text-slate-500 max-w-md mx-auto">Plans ruined by rain, delays, or fatigue? Let Vyntra instantly replan your journey based on your current constraints.</p>
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-xl mb-6 text-sm flex items-center gap-2">
          <span className="font-bold">!</span> {error}
      </div>}

      <div className="card p-8 border-t-4 border-t-orange-500">
        <form onSubmit={handleSubmit} className="space-y-6">
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">Where are you currently?</label>
              <input type="text" className="input-field bg-slate-50 border-orange-200 focus:border-orange-500 focus:ring-orange-500/20" placeholder="e.g. Chennai"
                value={form.currentLocation} onChange={e => setForm({...form, currentLocation: e.target.value})} required />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">Where do you need to reach?</label>
              <input type="text" className="input-field bg-slate-50" placeholder="e.g. Bangalore"
                value={form.destination} onChange={e => setForm({...form, destination: e.target.value})} />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">What went wrong?</label>
              <select className="select-field bg-slate-50" value={form.issue} onChange={e => setForm({...form, issue: e.target.value})}>
                <option value="rain">Unexpected Rain / Bad Weather</option>
                <option value="tired">Too Tired / Low Energy</option>
                <option value="closed_place">Destination is Closed</option>
                <option value="vehicle_issue">Vehicle Breakdown / Delay</option>
                <option value="low_budget">Running out of budget</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">Current Energy Level</label>
              <select className="select-field bg-slate-50" value={form.currentEnergy} onChange={e => setForm({...form, currentEnergy: e.target.value})}>
                <option value="low">Exhausted (Need immediate rest)</option>
                <option value="normal">Normal (Can drive a bit)</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">Remaining Budget (₹)</label>
              <input type="number" className="input-field bg-slate-50" min="0" step="100"
                value={form.remainingBudget} onChange={e => setForm({...form, remainingBudget: Number(e.target.value)})} />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">Remaining Time (Hours)</label>
              <input type="number" className="input-field bg-slate-50" min="0.5" max="24" step="0.5"
                value={form.remainingTime} onChange={e => setForm({...form, remainingTime: Number(e.target.value)})} />
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">What do you need right now?</label>
            <div className="flex flex-wrap gap-3">
              {interestsList.map(item => {
                const active = form.interests.includes(item.id)
                return (
                  <button type="button" key={item.id} onClick={() => toggleInterest(item.id)}
                    className={`px-4 py-2 rounded-full border text-sm font-medium flex items-center gap-2 transition-all ${
                      active ? 'bg-orange-50 border-orange-300 text-orange-700 shadow-sm' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}>
                    <span>{item.icon}</span> {item.label}
                  </button>
                )
              })}
            </div>
          </div>

          <div className="pt-4">
            <button type="submit" className="w-full bg-orange-600 hover:bg-orange-700 text-white font-bold py-3.5 px-6 rounded-xl shadow-lg shadow-orange-600/20 active:scale-[0.98] transition-all flex items-center justify-center gap-2" disabled={loading}>
              {loading ? <span className="spinner w-5 h-5 border-2 border-white/30 border-l-white"></span> : <><span>🚨</span> Generate Emergency Route</>}
            </button>
          </div>
        </form>
      </div>

      {/* Show inline rescue result if no tripId */}
      {result && (
        <div className="mt-8 space-y-6 fade-in">
          <div className="card p-6 bg-orange-50 border-orange-200">
            <h2 className="heading text-xl text-orange-800 mb-2">🚨 Rescue Plan Generated</h2>
            <p className="text-slate-600 text-sm">{result.routeSummary || `${result.startLocation} → ${result.destination}`}</p>
            <div className="flex gap-4 mt-3 text-sm text-slate-600">
              <span>💰 ₹{result.estimatedTotalCost}</span>
              <span>⏱️ {result.estimatedTravelTime}</span>
            </div>
          </div>

          {result.suggestedPlaces?.map((place, idx) => (
            <div key={idx} className="card p-5">
              <div className="flex justify-between items-start mb-2">
                <span className="badge badge-teal">{place.category}</span>
                {place.score > 0 && <span className="text-sm font-bold text-emerald-600">Score: {Math.round(place.score)}</span>}
              </div>
              <h3 className="text-lg font-bold text-slate-800 mb-1">{place.name}</h3>
              <p className="text-sm text-slate-500 mb-2">{place.address}</p>
              <p className="text-sm text-slate-600">{place.reason}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
