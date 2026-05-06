import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { tripAPI } from '../services/api'
import EmotionRoute from '../components/EmotionRoute' // EmotionRoute AI Mood Planner

const TRAVEL_STYLES = [
  { id: 'adventure',  label: 'Adventure',  icon: '🏔️', desc: 'Trek, viewpoints, activities' },
  { id: 'heritage',   label: 'Heritage',   icon: '🏛️', desc: 'Forts, monuments, museums' },
  { id: 'nature',     label: 'Nature',     icon: '🌿', desc: 'Parks, lakes, waterfalls' },
  { id: 'foodie',     label: 'Foodie',     icon: '🍛', desc: 'Famous food spots & cafes' },
  { id: 'spiritual',  label: 'Spiritual',  icon: '🕉️', desc: 'Temples, shrines, ashrams' },
  { id: 'shopping',   label: 'Shopping',   icon: '🛍️', desc: 'Markets, malls, bazaars' },
  { id: 'family',     label: 'Family',     icon: '👨‍👩‍👧', desc: 'Safe, kid-friendly spots' },
  { id: 'romantic',   label: 'Romantic',   icon: '💑', desc: 'Scenic, peaceful locations' },
  { id: 'luxury',     label: 'Luxury',     icon: '💎', desc: 'Premium stays & dining' },
  { id: 'budget',     label: 'Budget',     icon: '💰', desc: 'Cheap & high-rated places' },
]

const INTERESTS = [
  { id: 'food',     label: 'Food & Dining',      icon: '🍽️' },
  { id: 'temple',   label: 'Temples & Shrines',  icon: '🕉️' },
  { id: 'nature',   label: 'Nature & Parks',     icon: '🌳' },
  { id: 'museum',   label: 'Museums & History',  icon: '🏛️' },
  { id: 'shopping', label: 'Shopping',            icon: '🛍️' },
  { id: 'rest',     label: 'Rest Stops',          icon: '☕' },
]

export default function PlanTrip() {
  const [step, setStep] = useState(1)  // 1=route, 2=style, 3=details
  const [form, setForm] = useState({
    startLocation: '', destination: '',
    budget: 2000, timeAvailable: 8,
    energyLevel: 'normal', travelType: 'family',
    mode: 'car', interests: [], problemFaced: '',
    travelStyle: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  // EmotionRoute AI Mood Planner state
  const [moodResult, setMoodResult] = useState(null)
  const navigate = useNavigate()

  const toggleInterest = (id) => {
    setForm(prev => ({
      ...prev,
      interests: prev.interests.includes(id)
        ? prev.interests.filter(i => i !== id)
        : [...prev.interests, id],
    }))
  }

  const handleStyleSelect = (id) => {
    setForm(prev => ({ ...prev, travelStyle: id }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.startLocation || !form.destination) {
      setError('Start location and destination are required.')
      return
    }
    setLoading(true); setError('')
    try {
      const res = await tripAPI.plan(form)
      navigate(`/trip/${res.data.tripId}`)
    } catch (err) {
      if (err.backendDown) {
        setError('Backend not reachable. Please start Spring Boot on port 8091.')
      } else {
        setError(err.response?.data?.message || 'Failed to plan trip. Check your connection.')
      }
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-4xl mx-auto fade-in pb-16">
      {/* Header */}
      <div className="mb-8">
        <h1 className="heading text-3xl mb-2" style={{ color: '#0f172a' }}>Plan Your Trip</h1>
        <p style={{ color: '#64748b' }}>Enter any source & destination — Vyntra finds real places dynamically worldwide.</p>
      </div>

      {/* Step Indicator */}
      <div className="flex items-center gap-2 mb-8">
        {[1, 2, 3].map(s => (
          <div key={s} className="flex items-center gap-2">
            <button
              onClick={() => s < step && setStep(s)}
              className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold transition-all"
              style={{
                backgroundColor: step === s ? '#0f766e' : step > s ? '#f0fdfa' : '#f1f5f9',
                color: step === s ? '#ffffff' : step > s ? '#0f766e' : '#94a3b8',
                cursor: s < step ? 'pointer' : 'default',
              }}>
              <span className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold"
                style={{ backgroundColor: step >= s ? 'rgba(255,255,255,0.3)' : '#e2e8f0' }}>
                {step > s ? '✓' : s}
              </span>
              {s === 1 ? 'Route' : s === 2 ? 'Travel Style' : 'Details'}
            </button>
            {s < 3 && <div className="w-8 h-[2px]" style={{ backgroundColor: step > s ? '#0f766e' : '#e2e8f0' }} />}
          </div>
        ))}
      </div>

      {error && (
        <div className="mb-6 px-4 py-3 rounded-xl flex items-center gap-2 text-sm"
          style={{ backgroundColor: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626' }}>
          <span className="font-bold">!</span> {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* ── STEP 1: Route ── */}
        {step === 1 && (
          <div className="card p-8 fade-in">
            <h2 className="text-lg font-bold mb-6" style={{ color: '#0f172a', fontFamily: 'Outfit' }}>
              🗺️ Where are you going?
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-8">
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Starting Point
                </label>
                <input
                  type="text" className="input-field"
                  placeholder="e.g. Chennai, Tamil Nadu"
                  value={form.startLocation}
                  onChange={e => setForm({ ...form, startLocation: e.target.value })}
                  required
                />
                <p className="text-xs mt-1" style={{ color: '#94a3b8' }}>Works for any city worldwide</p>
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Destination
                </label>
                <input
                  type="text" className="input-field"
                  placeholder="e.g. Bangalore, Karnataka"
                  value={form.destination}
                  onChange={e => setForm({ ...form, destination: e.target.value })}
                  required
                />
                <p className="text-xs mt-1" style={{ color: '#94a3b8' }}>Enter any destination globally</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-8">
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Travel Mode
                </label>
                <select className="select-field" value={form.mode}
                  onChange={e => setForm({ ...form, mode: e.target.value })}>
                  <option value="car">🚗 Car / Cab</option>
                  <option value="bike">🏍️ Motorcycle</option>
                  <option value="bus">🚌 Bus</option>
                  <option value="train">🚂 Train</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Travel Group
                </label>
                <select className="select-field" value={form.travelType}
                  onChange={e => setForm({ ...form, travelType: e.target.value })}>
                  <option value="family">👨‍👩‍👧 Family</option>
                  <option value="friends">👯 Friends</option>
                  <option value="solo">🧳 Solo</option>
                  <option value="couple">💑 Couple</option>
                </select>
              </div>
            </div>

            {/* Quick Demo Buttons */}
            <div className="mb-6">
              <p className="text-xs font-semibold mb-3" style={{ color: '#94a3b8' }}>QUICK DEMOS</p>
              <div className="flex flex-wrap gap-2">
                {[
                  { from: 'Chennai', to: 'Bangalore', label: '🏙️ Chennai → Bangalore' },
                  { from: 'Delhi', to: 'Agra', label: '🕌 Delhi → Agra' },
                  { from: 'Mumbai', to: 'Pune', label: '🌊 Mumbai → Pune' },
                  { from: 'Hyderabad', to: 'Goa', label: '🏖️ Hyderabad → Goa' },
                ].map(demo => (
                  <button key={demo.label} type="button"
                    onClick={() => setForm({ ...form, startLocation: demo.from, destination: demo.to })}
                    className="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all hover:scale-[1.02]"
                    style={{ backgroundColor: '#f0fdfa', color: '#0f766e', border: '1px solid #99f6e4' }}>
                    {demo.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex justify-end">
              <button type="button"
                onClick={() => {
                  if (!form.startLocation || !form.destination) {
                    setError('Please enter start and destination first.')
                    return
                  }
                  setError('')
                  setStep(2)
                }}
                className="btn-primary px-8 py-3">
                Next: Choose Travel Style →
              </button>
            </div>
          </div>
        )}

        {/* ── STEP 2: Travel Style ── */}
        {step === 2 && (
          <div className="card p-8 fade-in">
            <h2 className="text-lg font-bold mb-2" style={{ color: '#0f172a', fontFamily: 'Outfit' }}>
              ✨ What type of trip do you want?
            </h2>
            <p className="text-sm mb-6" style={{ color: '#64748b' }}>
              Select a travel style — Vyntra will find the best places that match your vibe.
            </p>

            <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mb-8">
              {TRAVEL_STYLES.map(style => {
                const active = form.travelStyle === style.id
                return (
                  <button type="button" key={style.id}
                    onClick={() => handleStyleSelect(style.id)}
                    className="p-4 rounded-xl border text-center transition-all hover:scale-[1.02]"
                    style={{
                      backgroundColor: active ? '#f0fdfa' : '#ffffff',
                      borderColor: active ? '#0f766e' : '#e2e8f0',
                      boxShadow: active ? '0 0 0 2px rgba(15,118,110,0.2)' : 'none',
                    }}>
                    <span className="text-2xl block mb-1">{style.icon}</span>
                    <span className="text-xs font-bold block" style={{ color: active ? '#0f766e' : '#0f172a' }}>
                      {style.label}
                    </span>
                    <span className="text-[10px] block mt-0.5" style={{ color: '#94a3b8' }}>
                      {style.desc}
                    </span>
                  </button>
                )
              })}
            </div>

            {/* Also pick specific interests */}
            <div className="mb-8">
              <p className="text-sm font-semibold mb-3" style={{ color: '#374151' }}>
                Also interested in: <span className="font-normal" style={{ color: '#94a3b8' }}>(optional)</span>
              </p>
              <div className="flex flex-wrap gap-2">
                {INTERESTS.map(item => {
                  const active = form.interests.includes(item.id)
                  return (
                    <button type="button" key={item.id} onClick={() => toggleInterest(item.id)}
                      className="px-4 py-2 rounded-full text-sm font-medium flex items-center gap-2 transition-all"
                      style={{
                        backgroundColor: active ? '#f0fdfa' : '#f8fafc',
                        borderColor: active ? '#0f766e' : '#e2e8f0',
                        color: active ? '#0f766e' : '#475569',
                        border: '1px solid',
                      }}>
                      {item.icon} {item.label}
                    </button>
                  )
                })}
              </div>
            </div>

            {/* ── EmotionRoute AI Mood Planner ── */}
            <div style={{ borderTop:'1px solid #e2e8f0', paddingTop:'1.5rem', marginTop:'0.5rem', marginBottom:'1.5rem' }}>
              <EmotionRoute
                source={form.startLocation}
                destination={form.destination}
                onMoodResult={(result) => {
                  setMoodResult(result)
                  // Auto-map AI trip type to form travelStyle if not already set
                  const tripType = result?.moodAnalysis?.tripType
                  if (tripType && !form.travelStyle) {
                    setForm(prev => ({ ...prev, travelStyle: tripType }))
                  }
                  // Auto-set energy level from AI
                  const energy = result?.moodAnalysis?.energyLevel
                  if (energy) setForm(prev => ({ ...prev, energyLevel: energy }))
                }}
              />
            </div>

            <div className="flex justify-between">
              <button type="button" onClick={() => setStep(1)} className="btn-secondary px-6 py-3">
                ← Back
              </button>
              <button type="button" onClick={() => setStep(3)} className="btn-primary px-8 py-3">
                Next: Set Constraints →
              </button>
            </div>
          </div>
        )}

        {/* ── STEP 3: Details ── */}
        {step === 3 && (
          <div className="card p-8 fade-in">
            <h2 className="text-lg font-bold mb-6" style={{ color: '#0f172a', fontFamily: 'Outfit' }}>
              ⚙️ Trip Constraints
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-6">
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Budget (₹) <span className="font-normal" style={{ color: '#94a3b8' }}>for stops</span>
                </label>
                <input type="number" className="input-field" min="0" step="100"
                  value={form.budget}
                  onChange={e => setForm({ ...form, budget: Number(e.target.value) })} />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Time Available (hours)
                </label>
                <input type="number" className="input-field" min="1" max="48" step="0.5"
                  value={form.timeAvailable}
                  onChange={e => setForm({ ...form, timeAvailable: Number(e.target.value) })} />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Energy Level
                </label>
                <select className="select-field" value={form.energyLevel}
                  onChange={e => setForm({ ...form, energyLevel: e.target.value })}>
                  <option value="high">⚡ High — Adventure / Trek</option>
                  <option value="normal">😊 Normal — Sightseeing</option>
                  <option value="low">🛋️ Low — Rest / Relax</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-semibold mb-1.5" style={{ color: '#374151' }}>
                  Any Issues? (optional)
                </label>
                <select className="select-field" value={form.problemFaced}
                  onChange={e => setForm({ ...form, problemFaced: e.target.value })}>
                  <option value="">No problem</option>
                  <option value="rain">🌧️ Expecting rain</option>
                  <option value="tired">😴 Feeling tired</option>
                  <option value="low_budget">💸 Low budget</option>
                  <option value="closed_place">🚫 Some places may be closed</option>
                </select>
              </div>
            </div>

            {/* Summary Card */}
            <div className="rounded-xl p-5 mb-8" style={{ backgroundColor: '#f0fdfa', border: '1px solid #99f6e4' }}>
              <p className="text-sm font-bold mb-2" style={{ color: '#0f766e' }}>Trip Summary</p>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                <div>
                  <span style={{ color: '#94a3b8' }}>From</span>
                  <p className="font-semibold" style={{ color: '#0f172a' }}>{form.startLocation}</p>
                </div>
                <div>
                  <span style={{ color: '#94a3b8' }}>To</span>
                  <p className="font-semibold" style={{ color: '#0f172a' }}>{form.destination}</p>
                </div>
                <div>
                  <span style={{ color: '#94a3b8' }}>Style</span>
                  <p className="font-semibold capitalize" style={{ color: '#0f172a' }}>
                    {form.travelStyle || 'General'}
                  </p>
                </div>
                <div>
                  <span style={{ color: '#94a3b8' }}>Mode</span>
                  <p className="font-semibold capitalize" style={{ color: '#0f172a' }}>{form.mode}</p>
                </div>
              </div>
            </div>

            <div className="flex justify-between">
              <button type="button" onClick={() => setStep(2)} className="btn-secondary px-6 py-3">
                ← Back
              </button>
              <button type="submit" className="btn-primary py-3 px-10 text-base" disabled={loading}>
                {loading
                  ? <span className="flex items-center gap-2">
                      <span className="spinner w-5 h-5 border-2" />
                      Finding real places along your route...
                    </span>
                  : '🗺️ Generate My Trip Plan'}
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  )
}
