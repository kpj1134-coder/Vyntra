import { useState } from 'react'
import { moodAPI } from '../services/api'

// EmotionRoute AI Mood Planner – Quick mood buttons
const MOOD_BUTTONS = [
  { id: 'Relaxing',     icon: '😌', color: '#0891b2' },
  { id: 'Adventure',    icon: '🏔️', color: '#16a34a' },
  { id: 'Heritage',     icon: '🏛️', color: '#7c3aed' },
  { id: 'Food',         icon: '🍛', color: '#ea580c' },
  { id: 'Nature',       icon: '🌿', color: '#15803d' },
  { id: 'Budget',       icon: '💰', color: '#b45309' },
  { id: 'Romantic',     icon: '💑', color: '#db2777' },
  { id: 'Spiritual',    icon: '🕉️', color: '#6d28d9' },
  { id: 'Not Interested', icon: '😐', color: '#475569' },
]

const ENERGY_COLORS = { low: '#ef4444', medium: '#f59e0b', high: '#10b981' }
const TRIP_ICONS = {
  calm:'😌', adventure:'🏔️', heritage:'🏛️', food:'🍛',
  nature:'🌿', spiritual:'🕉️', shopping:'🛍️', budget:'💰', romantic:'💑',
}

/**
 * EmotionRoute AI Mood Planner Component
 * Renders mood input + quick buttons + AI analysis result cards.
 * Accepts onMoodResult(result) callback so PlanTrip can use the mood data.
 * Does NOT modify any existing trip planning logic.
 */
export default function EmotionRoute({ source, destination, onMoodResult }) {
  const [moodText, setMoodText] = useState('')
  const [selectedMood, setSelectedMood] = useState(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const analyze = async () => {
    if (!moodText.trim() && !selectedMood) {
      setError('Please enter your mood or select a quick mood button.')
      return
    }
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await moodAPI.analyze({
        source: source || '',
        destination: destination || '',
        userMoodText: moodText,
        selectedMood: selectedMood || '',
        nearbyPlaces: [],
      })
      setResult(res.data)
      if (onMoodResult) onMoodResult(res.data)
    } catch (err) {
      // Graceful fallback — show message, don't crash
      setError('AI mood planning is currently unavailable. Showing normal trip plan.')
    } finally { setLoading(false) }
  }

  const handleMoodButton = (id) => {
    setSelectedMood(prev => prev === id ? null : id)
    setError('')
  }

  const analysis = result?.moodAnalysis

  return (
    <div style={{ marginTop: '2rem' }}>
      {/* Section Header */}
      <div style={{ display:'flex', alignItems:'center', gap:'0.75rem', marginBottom:'1rem' }}>
        <div style={{ width:36, height:36, borderRadius:'0.75rem', background:'linear-gradient(135deg,#7c3aed,#db2777)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'1.1rem' }}>
          🧠
        </div>
        <div>
          <h3 style={{ fontFamily:'Outfit', fontWeight:700, color:'#0f172a', fontSize:'1rem', margin:0 }}>
            EmotionRoute AI — Tell us your travel mood
          </h3>
          <p style={{ color:'#64748b', fontSize:'0.78rem', margin:0 }}>
            AI will personalize your trip recommendations based on how you feel
          </p>
        </div>
      </div>

      {/* Mood Text Input */}
      <div style={{ position:'relative', marginBottom:'0.75rem' }}>
        <textarea
          value={moodText}
          onChange={e => { setMoodText(e.target.value); setError('') }}
          placeholder="Example: I am tired and don't want crowded places..."
          rows={2}
          style={{
            width:'100%', boxSizing:'border-box',
            background:'#f8fafc', border:'1px solid #cbd5e1', color:'#0f172a',
            borderRadius:'0.75rem', padding:'0.75rem 1rem',
            fontSize:'0.875rem', fontFamily:'Inter, sans-serif',
            resize:'vertical', outline:'none', lineHeight:1.6,
          }}
          onFocus={e => e.target.style.borderColor='#7c3aed'}
          onBlur={e => e.target.style.borderColor='#cbd5e1'}
        />
      </div>

      {/* Quick Mood Buttons */}
      <div style={{ display:'flex', flexWrap:'wrap', gap:'0.5rem', marginBottom:'1rem' }}>
        {MOOD_BUTTONS.map(btn => {
          const active = selectedMood === btn.id
          return (
            <button key={btn.id} type="button" onClick={() => handleMoodButton(btn.id)}
              style={{
                padding:'0.4rem 0.9rem', borderRadius:'9999px', fontSize:'0.8rem', fontWeight:600,
                cursor:'pointer', transition:'all 0.15s', border:'1px solid',
                background: active ? btn.color : '#f8fafc',
                color: active ? '#ffffff' : '#475569',
                borderColor: active ? btn.color : '#e2e8f0',
                transform: active ? 'scale(1.05)' : 'scale(1)',
              }}>
              {btn.icon} {btn.id}
            </button>
          )
        })}
      </div>

      {/* Error message */}
      {error && (
        <div style={{ padding:'0.75rem 1rem', borderRadius:'0.75rem', background:'#fffbeb', border:'1px solid #fde68a', color:'#92400e', fontSize:'0.82rem', marginBottom:'0.75rem' }}>
          ⚠️ {error}
        </div>
      )}

      {/* Analyze Button */}
      <button type="button" onClick={analyze} disabled={loading}
        style={{
          display:'inline-flex', alignItems:'center', gap:'0.5rem',
          padding:'0.625rem 1.5rem', borderRadius:'0.75rem',
          background: loading ? '#94a3b8' : 'linear-gradient(135deg,#7c3aed,#db2777)',
          color:'#ffffff', fontWeight:700, fontSize:'0.875rem',
          border:'none', cursor: loading ? 'not-allowed' : 'pointer',
          transition:'all 0.2s', marginBottom:'1.25rem',
        }}>
        {loading
          ? <><span style={{ width:16, height:16, borderRadius:'50%', border:'2px solid rgba(255,255,255,0.3)', borderLeftColor:'#fff', animation:'spin 0.8s linear infinite', display:'inline-block' }} /> Analyzing Mood…</>
          : '🧠 Analyze My Mood'}
      </button>

      {/* ── Results ── */}
      {result && analysis && (
        <div className="fade-in">
          {/* Fallback warning */}
          {result.fallback && (
            <div style={{ padding:'0.75rem 1rem', borderRadius:'0.75rem', background:'#fffbeb', border:'1px solid #fde68a', color:'#92400e', fontSize:'0.82rem', marginBottom:'1rem' }}>
              ⚠️ {result.fallbackMessage}
            </div>
          )}

          {/* Cards Grid */}
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fit, minmax(200px, 1fr))', gap:'0.75rem', marginBottom:'1rem' }}>

            {/* Mood Detected */}
            <div style={{ background:'#fdf4ff', border:'1px solid #e9d5ff', borderRadius:'0.75rem', padding:'1rem' }}>
              <p style={{ fontSize:'0.7rem', fontWeight:700, color:'#7c3aed', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.25rem' }}>Mood Detected</p>
              <p style={{ fontWeight:700, color:'#0f172a', fontSize:'1.1rem', margin:0, textTransform:'capitalize' }}>
                {analysis.mood || 'relaxed'} 😊
              </p>
            </div>

            {/* Energy Level */}
            <div style={{ background:'#f0fdf4', border:'1px solid #bbf7d0', borderRadius:'0.75rem', padding:'1rem' }}>
              <p style={{ fontSize:'0.7rem', fontWeight:700, color:'#15803d', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.25rem' }}>Energy Level</p>
              <p style={{ fontWeight:700, fontSize:'1.1rem', margin:0, textTransform:'capitalize',
                color: ENERGY_COLORS[analysis.energyLevel] || '#0f172a' }}>
                {analysis.energyLevel === 'low' ? '🔋 Low' : analysis.energyLevel === 'high' ? '⚡ High' : '🔆 Medium'}
              </p>
            </div>

            {/* Trip Type */}
            <div style={{ background:'#eff6ff', border:'1px solid #bfdbfe', borderRadius:'0.75rem', padding:'1rem' }}>
              <p style={{ fontSize:'0.7rem', fontWeight:700, color:'#1d4ed8', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.25rem' }}>Trip Type</p>
              <p style={{ fontWeight:700, color:'#0f172a', fontSize:'1.1rem', margin:0, textTransform:'capitalize' }}>
                {TRIP_ICONS[analysis.tripType] || '🗺️'} {analysis.tripType || 'calm'}
              </p>
            </div>

            {/* Route Style */}
            <div style={{ background:'#fff7ed', border:'1px solid #fed7aa', borderRadius:'0.75rem', padding:'1rem' }}>
              <p style={{ fontSize:'0.7rem', fontWeight:700, color:'#ea580c', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.25rem' }}>Route Style</p>
              <p style={{ fontWeight:600, color:'#0f172a', fontSize:'0.9rem', margin:0 }}>
                {analysis.routeStyle || 'Comfortable'}
              </p>
            </div>
          </div>

          {/* Avoid These */}
          {analysis.avoid?.length > 0 && (
            <div style={{ background:'#fef2f2', border:'1px solid #fecaca', borderRadius:'0.75rem', padding:'0.875rem 1rem', marginBottom:'0.75rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#dc2626', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.5rem' }}>⛔ Avoid These</p>
              <div style={{ display:'flex', flexWrap:'wrap', gap:'0.4rem' }}>
                {analysis.avoid.map((item, i) => (
                  <span key={i} style={{ background:'#fee2e2', color:'#991b1b', padding:'0.2rem 0.6rem', borderRadius:'9999px', fontSize:'0.78rem', fontWeight:600 }}>
                    {item}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Recommended Places */}
          {(analysis.preferredPlaces?.length > 0 || result.recommendedPlaces?.length > 0) && (
            <div style={{ background:'#f0fdfa', border:'1px solid #99f6e4', borderRadius:'0.75rem', padding:'0.875rem 1rem', marginBottom:'0.75rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#0f766e', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.5rem' }}>✅ Recommended Places</p>
              <div style={{ display:'flex', flexWrap:'wrap', gap:'0.4rem' }}>
                {(analysis.preferredPlaces?.length > 0 ? analysis.preferredPlaces : result.recommendedPlaces).map((item, i) => (
                  <span key={i} style={{ background:'#ccfbf1', color:'#115e59', padding:'0.2rem 0.6rem', borderRadius:'9999px', fontSize:'0.78rem', fontWeight:600 }}>
                    📍 {item}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Food Preference */}
          {analysis.foodPreference && (
            <div style={{ background:'#fffbeb', border:'1px solid #fde68a', borderRadius:'0.75rem', padding:'0.875rem 1rem', marginBottom:'0.75rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#b45309', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.25rem' }}>🍽️ Food Preference</p>
              <p style={{ color:'#0f172a', fontSize:'0.875rem', fontWeight:500, margin:0 }}>{analysis.foodPreference}</p>
            </div>
          )}

          {/* AI Reason */}
          {analysis.suggestionReason && (
            <div style={{ background:'linear-gradient(135deg,#fdf4ff,#eff6ff)', border:'1px solid #e9d5ff', borderRadius:'0.75rem', padding:'0.875rem 1rem' }}>
              <p style={{ fontSize:'0.72rem', fontWeight:700, color:'#7c3aed', textTransform:'uppercase', letterSpacing:'0.05em', margin:'0 0 0.4rem' }}>🧠 AI Reason</p>
              <p style={{ color:'#1e1b4b', fontSize:'0.875rem', lineHeight:1.6, margin:0 }}>{analysis.suggestionReason}</p>
              {result.tripAdvice && result.tripAdvice !== analysis.suggestionReason && (
                <p style={{ color:'#4c1d95', fontSize:'0.8rem', lineHeight:1.5, margin:'0.5rem 0 0', fontStyle:'italic' }}>{result.tripAdvice}</p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
