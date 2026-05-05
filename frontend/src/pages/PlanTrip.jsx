import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { tripAPI } from '../services/api'

export default function PlanTrip() {
  const [form, setForm] = useState({
    startLocation: '', destination: '', budget: 2000, timeAvailable: 8,
    energyLevel: 'normal', travelType: 'family', mode: 'car',
    interests: [], problemFaced: ''
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const interestsList = [
    { id: 'food', label: 'Food & Dining', icon: '🍽️' },
    { id: 'temple', label: 'Temples & Shrines', icon: '🕉️' },
    { id: 'nature', label: 'Nature & Parks', icon: '🌳' },
    { id: 'museum', label: 'Museums & History', icon: '🏛️' },
    { id: 'shopping', label: 'Shopping', icon: '🛍️' },
    { id: 'rest', label: 'Rest Stops', icon: '☕' }
  ]

  const toggleInterest = (id) => {
    setForm(prev => {
      const ints = prev.interests.includes(id) 
        ? prev.interests.filter(i => i !== id)
        : [...prev.interests, id]
      return { ...prev, interests: ints }
    })
  }

  const handleMysoreDemo = () => {
    setForm({
      startLocation: 'Mysore Railway Station',
      destination: 'Brindavan Gardens',
      budget: 1000,
      timeAvailable: 5,
      energyLevel: 'normal',
      travelType: 'family',
      mode: 'car',
      interests: ['food', 'nature', 'temple'],
      problemFaced: ''
    })
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
        setError(err.response?.data?.message || 'Failed to plan trip. Please check your connection.')
      }
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-4xl mx-auto fade-in pb-12">
      <div className="mb-8 flex items-end justify-between">
        <div>
          <h1 className="heading text-3xl mb-2">Plan Your Route</h1>
          <p className="text-slate-500">Vyntra will discover the best stops directly along your path.</p>
        </div>
        <button type="button" onClick={handleMysoreDemo}
          className="btn-secondary text-sm px-4 py-2 text-teal-700 border-teal-200 hover:bg-teal-50">
          🏰 Try Mysore Demo
        </button>
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-xl mb-6 text-sm flex items-center gap-2">
          <span className="font-bold">!</span> {error}
      </div>}

      <div className="card p-8">
        <form onSubmit={handleSubmit} className="space-y-8">
          
          {/* Section: Route */}
          <div>
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
              <span className="w-5 h-5 rounded bg-slate-100 flex items-center justify-center text-slate-500 text-[10px]">1</span> 
              Route Details
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Starting Point</label>
                <input type="text" className="input-field bg-slate-50" placeholder="e.g. Mysore Railway Station"
                  value={form.startLocation} onChange={e => setForm({...form, startLocation: e.target.value})} required />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Destination</label>
                <input type="text" className="input-field bg-slate-50" placeholder="e.g. Brindavan Gardens"
                  value={form.destination} onChange={e => setForm({...form, destination: e.target.value})} required />
              </div>
            </div>
          </div>

          <hr className="border-slate-100" />

          {/* Section: Constraints */}
          <div>
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
              <span className="w-5 h-5 rounded bg-slate-100 flex items-center justify-center text-slate-500 text-[10px]">2</span> 
              Trip Constraints
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Budget (Rs.) <span className="text-slate-400 font-normal">for extra stops</span></label>
                <input type="number" className="input-field bg-slate-50" min="0" step="100"
                  value={form.budget} onChange={e => setForm({...form, budget: Number(e.target.value)})} />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Extra Time Available (Hours)</label>
                <input type="number" className="input-field bg-slate-50" min="1" max="24" step="0.5"
                  value={form.timeAvailable} onChange={e => setForm({...form, timeAvailable: Number(e.target.value)})} />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Energy Level</label>
                <select className="select-field bg-slate-50" value={form.energyLevel} onChange={e => setForm({...form, energyLevel: e.target.value})}>
                  <option value="high">High (Adventure/Trek)</option>
                  <option value="normal">Normal (Sightseeing)</option>
                  <option value="low">Low (Rest/Relax)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Travel Group</label>
                <select className="select-field bg-slate-50" value={form.travelType} onChange={e => setForm({...form, travelType: e.target.value})}>
                  <option value="family">Family (Safe, Kids)</option>
                  <option value="friends">Friends (Fun, Active)</option>
                  <option value="solo">Solo (Flexible)</option>
                  <option value="couple">Couple (Romantic)</option>
                </select>
              </div>
            </div>
          </div>

          <hr className="border-slate-100" />

          {/* Section: Interests */}
          <div>
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
              <span className="w-5 h-5 rounded bg-slate-100 flex items-center justify-center text-slate-500 text-[10px]">3</span> 
              Interests & Preferences
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {interestsList.map(item => {
                const active = form.interests.includes(item.id)
                return (
                  <button type="button" key={item.id} onClick={() => toggleInterest(item.id)}
                    className={`p-3 rounded-xl border text-sm font-medium flex items-center gap-2 transition-all duration-200 ${
                      active ? 'bg-teal-50 border-teal-200 text-teal-700 shadow-sm' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}>
                    <span className="text-base">{item.icon}</span> {item.label}
                  </button>
                )
              })}
            </div>
          </div>

          <hr className="border-slate-100" />

          {/* Section: Problem */}
          <div>
            <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
              <span className="w-5 h-5 rounded bg-slate-100 flex items-center justify-center text-slate-500 text-[10px]">4</span> 
              Any Issues? (Optional)
            </h3>
            <select className="select-field bg-slate-50" value={form.problemFaced} onChange={e => setForm({...form, problemFaced: e.target.value})}>
              <option value="">No problem</option>
              <option value="rain">Expecting rain</option>
              <option value="tired">Feeling tired</option>
              <option value="low_budget">Low budget</option>
              <option value="closed_place">Some places may be closed</option>
            </select>
          </div>

          <div className="pt-4 flex justify-end">
            <button type="submit" className="btn-primary py-3 px-8 text-base w-full md:w-auto" disabled={loading}>
              {loading ? <span className="flex items-center gap-2"><span className="spinner w-4 h-4 border-2"></span> Generating Optimal Route...</span> : 'Generate Trip Plan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
