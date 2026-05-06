import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { moodAPI } from '../services/api'

const MOOD_BUTTONS = [
  { id: 'Relaxing',      icon: '😌', color: '#0891b2',  bg: '#ecfeff' },
  { id: 'Adventure',     icon: '🏔️', color: '#16a34a',  bg: '#f0fdf4' },
  { id: 'Heritage',      icon: '🏛️', color: '#7c3aed',  bg: '#fdf4ff' },
  { id: 'Food',          icon: '🍛', color: '#ea580c',  bg: '#fff7ed' },
  { id: 'Nature',        icon: '🌿', color: '#15803d',  bg: '#f0fdf4' },
  { id: 'Budget',        icon: '💰', color: '#b45309',  bg: '#fffbeb' },
  { id: 'Romantic',      icon: '💑', color: '#db2777',  bg: '#fdf2f8' },
  { id: 'Spiritual',     icon: '🕉️', color: '#6d28d9',  bg: '#f5f3ff' },
  { id: 'Not Interested',icon: '😐', color: '#475569',  bg: '#f8fafc' },
]

const ENERGY_COLOR = { low: '#ef4444', medium: '#f59e0b', high: '#10b981' }
const TRIP_ICON    = { calm:'😌', adventure:'🏔️', heritage:'🏛️', food:'🍛', nature:'🌿', spiritual:'🕉️', shopping:'🛍️', budget:'💰', romantic:'💑' }

export default function MoodPlanner() {
  const navigate = useNavigate()
  const [source, setSource]   = useState('')
  const [dest, setDest]       = useState('')
  const [moodText, setMoodText] = useState('')
  const [selected, setSelected] = useState(null)
  const [loading, setLoading]   = useState(false)
  const [result, setResult]     = useState(null)
  const [error, setError]       = useState('')

  const toggle = (id) => { setSelected(p => p === id ? null : id); setError('') }

  const analyze = async () => {
    if (!moodText.trim() && !selected) { setError('Please type your mood or select a quick button.'); return }
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await moodAPI.analyze({
        source: source || 'My Location',
        destination: dest || 'Any Destination',
        userMoodText: moodText,
        selectedMood: selected || '',
        nearbyPlaces: [],
      })
      setResult(res.data)
    } catch (err) {
      console.error('Mood API error:', err.response?.data || err.message)
      setError(err.response?.data?.message || 'Could not connect to mood analysis service. Check the backend is running.')
    } finally { setLoading(false) }
  }

  const a = result?.moodAnalysis

  return (
    <div style={{ maxWidth:780, margin:'0 auto', paddingBottom:'4rem' }} className="fade-in">

      {/* Header */}
      <div style={{ marginBottom:'2rem' }}>
        <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', marginBottom:'0.5rem' }}>
          <div style={{ width:44, height:44, borderRadius:'1rem', background:'linear-gradient(135deg,#7c3aed,#db2777)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'1.4rem', boxShadow:'0 4px 12px rgba(124,58,237,0.3)' }}>🧠</div>
          <div>
            <h1 style={{ fontFamily:'Outfit', fontWeight:800, color:'#0f172a', fontSize:'1.6rem', margin:0 }}>EmotionRoute AI</h1>
            <p style={{ color:'#64748b', fontSize:'0.85rem', margin:0 }}>Let AI plan your trip based on how you feel right now</p>
          </div>
        </div>
        <button onClick={() => navigate('/plan')} style={{ fontSize:'0.78rem', color:'#0f766e', background:'none', border:'none', cursor:'pointer', textDecoration:'underline' }}>← Back to Trip Planner</button>
      </div>

      <div className="card" style={{ padding:'2rem', marginBottom:'1.5rem' }}>
        {/* Route Inputs */}
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'1rem', marginBottom:'1.5rem' }}>
          <div>
            <label style={{ display:'block', fontSize:'0.8rem', fontWeight:700, color:'#374151', marginBottom:'0.4rem' }}>From (optional)</label>
            <input value={source} onChange={e => setSource(e.target.value)} placeholder="e.g. Chennai" className="input-field" />
          </div>
          <div>
            <label style={{ display:'block', fontSize:'0.8rem', fontWeight:700, color:'#374151', marginBottom:'0.4rem' }}>To (optional)</label>
            <input value={dest} onChange={e => setDest(e.target.value)} placeholder="e.g. Bangalore" className="input-field" />
          </div>
        </div>

        {/* Mood Text */}
        <div style={{ marginBottom:'1rem' }}>
          <label style={{ display:'block', fontSize:'0.8rem', fontWeight:700, color:'#374151', marginBottom:'0.4rem' }}>Tell us your travel mood</label>
          <textarea value={moodText} onChange={e => { setMoodText(e.target.value); setError('') }} rows={3}
            placeholder="Example: I am tired and don't want crowded places. I prefer calm and peaceful spots..."
            style={{ width:'100%', boxSizing:'border-box', background:'#f8fafc', border:'1px solid #cbd5e1', color:'#0f172a', borderRadius:'0.75rem', padding:'0.75rem 1rem', fontSize:'0.875rem', resize:'vertical', outline:'none', fontFamily:'Inter,sans-serif', lineHeight:1.6 }}
            onFocus={e => e.target.style.borderColor='#7c3aed'} onBlur={e => e.target.style.borderColor='#cbd5e1'} />
        </div>

        {/* Quick Mood Buttons */}
        <div style={{ marginBottom:'1.5rem' }}>
          <p style={{ fontSize:'0.78rem', fontWeight:700, color:'#94a3b8', textTransform:'uppercase', letterSpacing:'0.05em', marginBottom:'0.6rem' }}>Quick Mood Select</p>
          <div style={{ display:'flex', flexWrap:'wrap', gap:'0.5rem' }}>
            {MOOD_BUTTONS.map(btn => {
              const active = selected === btn.id
              return (
                <button key={btn.id} type="button" onClick={() => toggle(btn.id)}
                  style={{ padding:'0.45rem 1rem', borderRadius:'9999px', fontSize:'0.82rem', fontWeight:600, cursor:'pointer', transition:'all 0.15s', border:`1.5px solid ${active ? btn.color : '#e2e8f0'}`, background: active ? btn.color : btn.bg, color: active ? '#ffffff' : btn.color, transform: active ? 'scale(1.05)' : 'scale(1)' }}>
                  {btn.icon} {btn.id}
                </button>
              )
            })}
          </div>
        </div>

        {error && (
          <div style={{ padding:'0.75rem 1rem', borderRadius:'0.75rem', background:'#fef2f2', border:'1px solid #fecaca', color:'#dc2626', fontSize:'0.82rem', marginBottom:'1rem' }}>
            ⚠️ {error}
          </div>
        )}

        <button onClick={analyze} disabled={loading}
          style={{ display:'inline-flex', alignItems:'center', gap:'0.6rem', padding:'0.75rem 2rem', borderRadius:'0.75rem', background: loading ? '#94a3b8' : 'linear-gradient(135deg,#7c3aed,#db2777)', color:'#fff', fontWeight:700, fontSize:'0.95rem', border:'none', cursor: loading ? 'not-allowed' : 'pointer', boxShadow:'0 4px 12px rgba(124,58,237,0.3)', transition:'all 0.2s' }}>
          {loading
            ? <><span style={{ width:18, height:18, borderRadius:'50%', border:'2px solid rgba(255,255,255,0.3)', borderTopColor:'#fff', animation:'spin 0.8s linear infinite', display:'inline-block' }} /> Analyzing your travel mood…</>
            : '🧠 Analyze My Mood'}
        </button>
      </div>

      {/* ── Results ── */}
      {result && a && (
        <div className="fade-in">
          {result.fallback && (
            <div style={{ padding:'0.75rem 1rem', borderRadius:'0.75rem', background:'#fffbeb', border:'1px solid #fde68a', color:'#92400e', fontSize:'0.82rem', marginBottom:'1rem' }}>
              ℹ️ Gemini AI is currently unavailable — showing smart rule-based suggestions instead.
            </div>
          )}

          {/* 4-card grid */}
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit, minmax(180px,1fr))', gap:'0.75rem', marginBottom:'1rem' }}>
            <Card bg="#fdf4ff" border="#e9d5ff" label="Mood Detected" color="#7c3aed">
              <p style={{ fontWeight:800, fontSize:'1.2rem', margin:0, textTransform:'capitalize', color:'#0f172a' }}>{a.mood || 'relaxed'} 😊</p>
            </Card>
            <Card bg="#f0fdf4" border="#bbf7d0" label="Energy Level" color="#15803d">
              <p style={{ fontWeight:800, fontSize:'1.2rem', margin:0, color: ENERGY_COLOR[a.energyLevel] || '#0f172a' }}>
                {a.energyLevel === 'low' ? '🔋 Low' : a.energyLevel === 'high' ? '⚡ High' : '🔆 Medium'}
              </p>
            </Card>
            <Card bg="#eff6ff" border="#bfdbfe" label="Trip Type" color="#1d4ed8">
              <p style={{ fontWeight:800, fontSize:'1.2rem', margin:0, textTransform:'capitalize', color:'#0f172a' }}>
                {TRIP_ICON[a.tripType] || '🗺️'} {a.tripType || 'calm'}
              </p>
            </Card>
            <Card bg="#fff7ed" border="#fed7aa" label="Route Style" color="#ea580c">
              <p style={{ fontWeight:600, fontSize:'0.9rem', margin:0, color:'#0f172a' }}>{a.routeStyle || 'Comfortable'}</p>
            </Card>
          </div>

          {/* Avoid */}
          {a.avoid?.length > 0 && (
            <div style={{ background:'#fef2f2', border:'1px solid #fecaca', borderRadius:'0.75rem', padding:'1rem', marginBottom:'0.75rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#dc2626', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.5rem' }}>⛔ Avoid These</p>
              <div style={{ display:'flex', flexWrap:'wrap', gap:'0.4rem' }}>
                {a.avoid.map((item, i) => <span key={i} style={{ background:'#fee2e2', color:'#991b1b', padding:'0.2rem 0.65rem', borderRadius:'9999px', fontSize:'0.78rem', fontWeight:600 }}>{item}</span>)}
              </div>
            </div>
          )}

          {/* Recommended */}
          {(a.preferredPlaces?.length > 0 || result.recommendedPlaces?.length > 0) && (
            <div style={{ background:'#f0fdfa', border:'1px solid #99f6e4', borderRadius:'0.75rem', padding:'1rem', marginBottom:'0.75rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#0f766e', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.5rem' }}>✅ Recommended Places</p>
              <div style={{ display:'flex', flexWrap:'wrap', gap:'0.4rem' }}>
                {(a.preferredPlaces?.length ? a.preferredPlaces : result.recommendedPlaces).map((p, i) => (
                  <span key={i} style={{ background:'#ccfbf1', color:'#115e59', padding:'0.2rem 0.65rem', borderRadius:'9999px', fontSize:'0.78rem', fontWeight:600 }}>📍 {p}</span>
                ))}
              </div>
            </div>
          )}

          {/* Food */}
          {a.foodPreference && (
            <div style={{ background:'#fffbeb', border:'1px solid #fde68a', borderRadius:'0.75rem', padding:'1rem', marginBottom:'0.75rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#b45309', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.4rem' }}>🍽️ Food Preference</p>
              <p style={{ color:'#0f172a', fontSize:'0.875rem', fontWeight:500, margin:0 }}>{a.foodPreference}</p>
            </div>
          )}

          {/* AI Reason */}
          {a.suggestionReason && (
            <div style={{ background:'linear-gradient(135deg,#fdf4ff,#eff6ff)', border:'1px solid #e9d5ff', borderRadius:'0.75rem', padding:'1.25rem', marginBottom:'1.5rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#7c3aed', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.5rem' }}>🧠 AI Travel Advice</p>
              <p style={{ color:'#1e1b4b', fontSize:'0.9rem', lineHeight:1.7, margin:0 }}>{a.suggestionReason}</p>
              {result.tripAdvice && <p style={{ color:'#4c1d95', fontSize:'0.82rem', margin:'0.5rem 0 0', fontStyle:'italic' }}>{result.tripAdvice}</p>}
            </div>
          )}

          {/* CTA */}
          <button onClick={() => navigate('/plan')}
            style={{ width:'100%', padding:'0.875rem', background:'linear-gradient(135deg,#0f766e,#115e59)', color:'#fff', fontWeight:700, fontSize:'0.95rem', border:'none', borderRadius:'0.75rem', cursor:'pointer', boxShadow:'0 4px 16px rgba(15,118,110,0.3)' }}>
            🗺️ Plan Trip with These Insights →
          </button>
        </div>
      )}
    </div>
  )
}

// Reusable card
function Card({ bg, border, label, color, children }) {
  return (
    <div style={{ background:bg, border:`1px solid ${border}`, borderRadius:'0.75rem', padding:'1rem' }}>
      <p style={{ fontSize:'0.7rem', fontWeight:700, color, textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.4rem' }}>{label}</p>
      {children}
    </div>
  )
}
