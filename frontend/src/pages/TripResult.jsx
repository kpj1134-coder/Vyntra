import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { tripAPI } from '../services/api'
import jsPDF from 'jspdf'
import html2canvas from 'html2canvas'

const NEARBY_CATS = [
  { id: 'food', label: 'Restaurants', icon: '🍽️' },
  { id: 'hotel', label: 'Hotels', icon: '🏨' },
  { id: 'attraction', label: 'Attractions', icon: '🎯' },
  { id: 'heritage', label: 'Heritage', icon: '🏛️' },
  { id: 'emergency', label: 'ATM/Hospital', icon: '🏥' },
  { id: 'nature', label: 'Nature', icon: '🌿' },
]

const CAT_IMG = {
  food: 'restaurant,food,cuisine', hotel: 'hotel,resort,accommodation',
  attraction: 'tourist,attraction,landmark', heritage: 'heritage,fort,monument',
  nature: 'nature,park,waterfall', spiritual: 'temple,shrine,spiritual',
  shopping: 'market,shopping,bazaar', adventure: 'adventure,trekking,sport',
  emergency: 'hospital,clinic,pharmacy', general: 'travel,tourism,journey',
}

const placeImg = (p) => {
  const kw = CAT_IMG[p.category] || 'travel,place'
  const seed = (p.name || 'place').replace(/[^a-z0-9]/gi, '').toLowerCase()
  return `https://source.unsplash.com/240x160/?${kw}&sig=${seed}`
}

export default function TripResult() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const printRef = useRef()

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadMsg, setLoadMsg] = useState('Planning your route…')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [pdfLoading, setPdfLoading] = useState(false)
  const [showMap, setShowMap] = useState(true)
  const [morePlaces, setMorePlaces] = useState([])
  const [moreLoading, setMoreLoading] = useState(false)
  const [selectedCat, setSelectedCat] = useState(null)
  const [moreOffset, setMoreOffset] = useState(0)

  // Rotate loading messages so user knows progress
  useEffect(() => {
    const msgs = [
      'Geocoding your locations…',
      'Fetching real route via OSRM…',
      'Searching nearby places…',
      'Applying your preferences…',
      'Generating AI summary…',
      'Almost ready!',
    ]
    let i = 0
    const t = setInterval(() => { i = (i + 1) % msgs.length; setLoadMsg(msgs[i]) }, 3000)
    return () => clearInterval(t)
  }, [])

  useEffect(() => {
    if (location.state?.rescueData) { setData(location.state.rescueData); setLoading(false); return }
    if (!id || id === 'rescue') { setError('No trip ID.'); setLoading(false); return }
    tripAPI.getTrip(id)
      .then(r => setData(r.data))
      .catch(() => setError('Failed to load trip. Please try again.'))
      .finally(() => setLoading(false))
  }, [id, location.state])

  const handleSave = async () => {
    if (!data?.tripId) return
    setSaving(true)
    try { await tripAPI.save(data.tripId); alert('Saved!') }
    catch { alert('Save failed.') }
    finally { setSaving(false) }
  }

  const handleShowMore = async (cat) => {
    const offset = cat === selectedCat ? moreOffset + 5 : 0
    setSelectedCat(cat); setMoreOffset(offset); setMoreLoading(true)
    try { const r = await tripAPI.morePlaces(id, cat, offset); setMorePlaces(r.data || []) }
    catch { setMorePlaces([]) }
    finally { setMoreLoading(false) }
  }

  // PDF with images using html2canvas
  const downloadPDF = async () => {
    setPdfLoading(true)
    try {
      const el = printRef.current
      const canvas = await html2canvas(el, { scale: 1.5, useCORS: true, allowTaint: true, logging: false })
      const imgData = canvas.toDataURL('image/jpeg', 0.85)
      const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
      const pW = pdf.internal.pageSize.getWidth()
      const pH = pdf.internal.pageSize.getHeight()
      const ratio = canvas.height / canvas.width
      const imgH = pW * ratio
      let yPos = 0
      while (yPos < imgH) {
        pdf.addImage(imgData, 'JPEG', 0, -yPos, pW, imgH, undefined, 'FAST')
        yPos += pH
        if (yPos < imgH) pdf.addPage()
      }
      pdf.save(`vyntra-${data?.startLocation}-to-${data?.destination}.pdf`)
    } catch (e) {
      // fallback to text
      alert('PDF ready! If it did not download, try the text plan instead.')
    } finally { setPdfLoading(false) }
  }

  const mapsDir = () => {
    const ps = data?.suggestedPlaces || []
    const wps = ps.filter(p => p.latitude && p.longitude).map(p => `${p.latitude},${p.longitude}`).join('|')
    return `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(data?.startLocation || '')}&destination=${encodeURIComponent(data?.destination || '')}${wps ? `&waypoints=${encodeURIComponent(wps)}` : ''}&travelmode=driving`
  }

  const viewUrl  = (p) => p.latitude && p.longitude
    ? `https://www.google.com/maps/search/?api=1&query=${p.latitude},${p.longitude}`
    : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(p.name)}`
  const dirUrl   = (p) => `https://www.google.com/maps/dir/?api=1&destination=${p.latitude && p.longitude ? `${p.latitude},${p.longitude}` : encodeURIComponent(p.name)}&travelmode=driving`
  const foodUrl  = (p) => `https://www.google.com/maps/search/restaurants+near+${p.latitude || 0},${p.longitude || 0}`

  if (loading) return (
    <div style={{ display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', minHeight:'60vh', gap:'1.5rem', textAlign:'center' }}>
      <div style={{ width:56, height:56, borderRadius:'50%', border:'4px solid #e2e8f0', borderTopColor:'#0f766e', animation:'spin 0.8s linear infinite' }} />
      <div>
        <p style={{ color:'#0f172a', fontWeight:700, fontSize:'1.1rem', margin:0 }}>Building Your Trip</p>
        <p style={{ color:'#64748b', fontSize:'0.875rem', marginTop:'0.5rem' }}>{loadMsg}</p>
      </div>
      <div style={{ display:'flex', gap:'0.5rem' }}>
        {['Geocoding','Routing','Places','AI Summary'].map((s,i) => (
          <span key={i} style={{ padding:'0.25rem 0.75rem', borderRadius:'9999px', fontSize:'0.72rem', fontWeight:600, background:'#f0fdfa', color:'#0f766e', border:'1px solid #99f6e4' }}>{s}</span>
        ))}
      </div>
    </div>
  )

  if (error) return <div style={{ padding:'1.5rem', background:'#fef2f2', color:'#dc2626', borderRadius:'0.75rem' }}>{error}</div>
  if (!data) return null

  const places = data.suggestedPlaces || []
  const removed = data.removedPlaces || []

  return (
    <div style={{ maxWidth:1100, margin:'0 auto', paddingBottom:'5rem' }} className="fade-in">

      {/* Printable area for PDF */}
      <div ref={printRef}>

        {/* ── Header ── */}
        <div style={{ background:'linear-gradient(135deg,#0f766e,#115e59)', borderRadius:'1rem', padding:'2rem', marginBottom:'1.5rem', boxShadow:'0 8px 32px rgba(15,118,110,0.25)' }}>
          <h1 style={{ color:'#fff', fontFamily:'Outfit', fontSize:'clamp(1.3rem,3vw,1.9rem)', fontWeight:800, margin:'0 0 0.75rem' }}>
            {data.startLocation} <span style={{ opacity:0.5 }}>→</span> {data.destination}
          </h1>
          <div style={{ display:'flex', flexWrap:'wrap', gap:'1rem', fontSize:'0.85rem', color:'#ccfbf1', marginBottom:'1.25rem' }}>
            <span>⏱️ {data.estimatedTravelTime || 'N/A'}</span>
            <span>💰 ₹{data.estimatedTotalCost || 0}</span>
            <span>📍 {places.length} stops</span>
            {data.weatherSummary && <span style={{ background:'rgba(255,255,255,0.1)', padding:'0.2rem 0.75rem', borderRadius:'9999px' }}>🌤️ {data.weatherSummary}</span>}
          </div>
          <div style={{ display:'flex', flexWrap:'wrap', gap:'0.75rem' }}>
            <a href={mapsDir()} target="_blank" rel="noopener noreferrer"
              style={{ display:'inline-flex', alignItems:'center', gap:'0.5rem', background:'#fff', color:'#0f766e', fontWeight:700, fontSize:'0.85rem', padding:'0.6rem 1.25rem', borderRadius:'0.75rem', textDecoration:'none' }}>
              🗺️ Full Route in Google Maps
            </a>
            <button onClick={() => setShowMap(m => !m)}
              style={{ background:'rgba(255,255,255,0.15)', color:'#fff', border:'1px solid rgba(255,255,255,0.3)', fontWeight:600, fontSize:'0.85rem', padding:'0.6rem 1rem', borderRadius:'0.75rem', cursor:'pointer' }}>
              {showMap ? 'Hide Map' : 'Show Map'}
            </button>
            <button onClick={downloadPDF} disabled={pdfLoading}
              style={{ background:'rgba(255,255,255,0.1)', color:'#fff', border:'1px solid rgba(255,255,255,0.2)', fontWeight:600, fontSize:'0.85rem', padding:'0.6rem 1rem', borderRadius:'0.75rem', cursor:'pointer' }}>
              {pdfLoading ? '⏳ Generating PDF…' : '📄 Download PDF'}
            </button>
          </div>
        </div>

        {/* ── Map ── */}
        {showMap && (
          <div className="card" style={{ marginBottom:'1.5rem', overflow:'hidden' }}>
            <div style={{ padding:'0.75rem 1.25rem', borderBottom:'1px solid #e2e8f0', background:'#f8fafc', display:'flex', justifyContent:'space-between', alignItems:'center' }}>
              <span style={{ fontWeight:700, color:'#0f172a', fontSize:'0.9rem' }}>🗺️ Route Map — {data.startLocation} to {data.destination}</span>
              <a href={mapsDir()} target="_blank" rel="noopener noreferrer" style={{ fontSize:'0.75rem', color:'#0f766e', fontWeight:600 }}>Open Full ↗</a>
            </div>
            <div style={{ position:'relative', paddingBottom:'42%', minHeight:280 }}>
              <iframe style={{ position:'absolute', inset:0, width:'100%', height:'100%', border:0 }} loading="lazy" allowFullScreen
                src={`https://www.google.com/maps/embed/v1/directions?key=AIzaSyBFw0Qbyq9zTFTd-tUY6dZWTgaQzuU17R8&origin=${encodeURIComponent(data.startLocation)}&destination=${encodeURIComponent(data.destination)}&mode=driving`}
              />
            </div>
          </div>
        )}

        {/* ── Stops with Images ── */}
        <div style={{ marginBottom:'2rem' }}>
          <h2 style={{ fontFamily:'Outfit', fontWeight:700, color:'#0f172a', fontSize:'1.2rem', marginBottom:'1rem' }}>
            📍 Route Stops — {data.startLocation} → {data.destination}
          </h2>

          {/* Route Header */}
          <div style={{ display:'flex', gap:'0.75rem', alignItems:'center', padding:'1rem', background:'#ecfdf5', borderRadius:'0.75rem', marginBottom:'1rem', border:'1px solid #a7f3d0' }}>
            <div style={{ width:36, height:36, borderRadius:'50%', background:'#10b981', display:'flex', alignItems:'center', justifyContent:'center', color:'#fff', fontWeight:800, fontSize:'0.9rem', flexShrink:0 }}>A</div>
            <div><p style={{ fontSize:'0.7rem', color:'#059669', fontWeight:700, textTransform:'uppercase', margin:0 }}>Start</p><p style={{ fontWeight:700, color:'#0f172a', fontSize:'0.95rem', margin:0 }}>{data.startLocation}</p></div>
          </div>

          {places.length === 0
            ? <div className="card" style={{ padding:'2rem', textAlign:'center', color:'#64748b' }}>No stops found. Try a different travel style or route.</div>
            : places.map((place, idx) => (
              <div key={idx} className="fade-in" style={{ display:'flex', gap:'1rem', background:'#fff', border:'1px solid #e2e8f0', borderRadius:'1rem', overflow:'hidden', marginBottom:'1rem', boxShadow:'0 1px 8px rgba(0,0,0,0.06)', transition:'transform 0.2s', cursor:'default' }}
                onMouseEnter={e => e.currentTarget.style.transform='translateY(-2px)'}
                onMouseLeave={e => e.currentTarget.style.transform='translateY(0)'}>
                {/* Place Image */}
                <div style={{ width:160, flexShrink:0, position:'relative', overflow:'hidden', minHeight:140 }}>
                  <img
                    src={placeImg(place)}
                    alt={place.name}
                    crossOrigin="anonymous"
                    onError={e => { e.target.style.display='none'; e.target.nextSibling.style.display='flex' }}
                    style={{ width:'100%', height:'100%', objectFit:'cover', display:'block' }}
                  />
                  <div style={{ display:'none', width:'100%', height:'100%', background:'linear-gradient(135deg,#0f766e,#115e59)', alignItems:'center', justifyContent:'center', fontSize:'2.5rem' }}>
                    {place.category==='food'?'🍽️':place.category==='hotel'?'🏨':place.category==='heritage'?'🏛️':place.category==='nature'?'🌿':place.category==='spiritual'?'🕉️':'📍'}
                  </div>
                  <div style={{ position:'absolute', top:8, left:8, background:'rgba(15,118,110,0.9)', color:'#fff', fontSize:'0.7rem', fontWeight:700, padding:'0.2rem 0.5rem', borderRadius:'9999px' }}>
                    Stop {idx + 1}
                  </div>
                </div>

                {/* Place Info */}
                <div style={{ flex:1, padding:'1rem', minWidth:0 }}>
                  <div style={{ display:'flex', justifyContent:'space-between', flexWrap:'wrap', gap:'0.5rem', marginBottom:'0.4rem' }}>
                    <span style={{ background:'#f0fdfa', color:'#0f766e', padding:'0.15rem 0.5rem', borderRadius:'9999px', fontSize:'0.72rem', fontWeight:700 }}>{place.category}</span>
                    {place.rating && <span style={{ color:'#b45309', fontSize:'0.8rem', fontWeight:700 }}>★ {Number(place.rating).toFixed(1)}</span>}
                  </div>
                  <h3 style={{ color:'#0f172a', fontWeight:700, fontSize:'1rem', margin:'0 0 0.25rem' }}>{place.name}</h3>
                  <p style={{ color:'#64748b', fontSize:'0.8rem', margin:'0 0 0.6rem' }}>{place.address || 'Near your route'}</p>
                  {place.reason && <p style={{ background:'#f8fafc', border:'1px solid #f1f5f9', borderRadius:'0.5rem', padding:'0.5rem 0.65rem', fontSize:'0.78rem', color:'#475569', margin:'0 0 0.65rem' }}><span style={{ color:'#0f766e', fontWeight:700 }}>↳ </span>{place.reason}</p>}
                  <div style={{ display:'flex', gap:'0.75rem', fontSize:'0.75rem', color:'#64748b', fontWeight:600, marginBottom:'0.75rem', flexWrap:'wrap' }}>
                    {place.distanceFromRoute != null && <span>📍 {place.distanceFromRoute} km off-route</span>}
                    {place.estimatedCost != null && <span>💵 ₹{Math.round(place.estimatedCost)}</span>}
                    {place.openNow === false && <span style={{ color:'#ef4444' }}>⚠️ May be closed</span>}
                  </div>
                  <div style={{ display:'flex', flexWrap:'wrap', gap:'0.5rem' }}>
                    <a href={viewUrl(place)} target="_blank" rel="noopener noreferrer" style={{ padding:'0.35rem 0.75rem', borderRadius:'0.5rem', fontSize:'0.75rem', fontWeight:600, background:'#f0fdfa', color:'#0f766e', border:'1px solid #99f6e4', textDecoration:'none' }}>📍 View</a>
                    <a href={dirUrl(place)} target="_blank" rel="noopener noreferrer" style={{ padding:'0.35rem 0.75rem', borderRadius:'0.5rem', fontSize:'0.75rem', fontWeight:600, background:'#eff6ff', color:'#1d4ed8', border:'1px solid #bfdbfe', textDecoration:'none' }}>🧭 Directions</a>
                    <a href={foodUrl(place)} target="_blank" rel="noopener noreferrer" style={{ padding:'0.35rem 0.75rem', borderRadius:'0.5rem', fontSize:'0.75rem', fontWeight:600, background:'#fffbeb', color:'#b45309', border:'1px solid #fde68a', textDecoration:'none' }}>🍽️ Food Nearby</a>
                  </div>
                </div>
              </div>
            ))
          }

          {/* Destination */}
          <div style={{ display:'flex', gap:'0.75rem', alignItems:'center', padding:'1rem', background:'#fef2f2', borderRadius:'0.75rem', border:'1px solid #fecaca' }}>
            <div style={{ width:36, height:36, borderRadius:'50%', background:'#ef4444', display:'flex', alignItems:'center', justifyContent:'center', color:'#fff', fontWeight:800, fontSize:'0.9rem', flexShrink:0 }}>B</div>
            <div><p style={{ fontSize:'0.7rem', color:'#dc2626', fontWeight:700, textTransform:'uppercase', margin:0 }}>Destination</p><p style={{ fontWeight:700, color:'#0f172a', fontSize:'0.95rem', margin:0 }}>{data.destination}</p></div>
            <a href={mapsDir()} target="_blank" rel="noopener noreferrer" style={{ marginLeft:'auto', padding:'0.5rem 1rem', background:'#0f766e', color:'#fff', borderRadius:'0.75rem', fontSize:'0.8rem', fontWeight:700, textDecoration:'none' }}>Navigate Full Route 🗺️</a>
          </div>
        </div>

        {/* ── AI Summary ── */}
        {data.aiSummary && (
          <div className="card" style={{ padding:'1.5rem', marginBottom:'1.5rem', borderTop:'4px solid #8b5cf6' }}>
            <h3 style={{ fontFamily:'Outfit', fontWeight:700, color:'#0f172a', marginBottom:'0.75rem', fontSize:'1rem' }}>✨ AI Trip Summary</h3>
            <p style={{ color:'#475569', fontSize:'0.875rem', lineHeight:1.7, margin:0, whiteSpace:'pre-line' }}>{data.aiSummary}</p>
          </div>
        )}
      </div>{/* end printRef */}

      {/* ── Show More (not in PDF) ── */}
      <div className="card" style={{ padding:'1.5rem', marginBottom:'1.5rem', borderTop:'4px solid #10b981' }}>
        <h3 style={{ fontFamily:'Outfit', fontWeight:700, color:'#0f172a', marginBottom:'0.5rem', fontSize:'1rem' }}>📍 Find More Places</h3>
        <div style={{ display:'flex', flexWrap:'wrap', gap:'0.5rem', marginBottom:'1rem' }}>
          {NEARBY_CATS.map(cat => (
            <button key={cat.id} onClick={() => handleShowMore(cat.id)}
              style={{ padding:'0.5rem 1rem', borderRadius:'0.75rem', fontSize:'0.8rem', fontWeight:600, cursor:'pointer',
                background: selectedCat === cat.id ? '#0f766e' : '#f8fafc',
                color: selectedCat === cat.id ? '#fff' : '#475569',
                border: selectedCat === cat.id ? '1px solid #0f766e' : '1px solid #e2e8f0' }}>
              {cat.icon} {cat.label}
            </button>
          ))}
        </div>
        {moreLoading && <p style={{ color:'#94a3b8', fontSize:'0.85rem' }}>Searching real places…</p>}
        {!moreLoading && morePlaces.length > 0 && (
          <div style={{ display:'grid', gap:'0.75rem' }}>
            {morePlaces.map((p, i) => (
              <div key={i} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'0.75rem 1rem', background:'#f8fafc', borderRadius:'0.75rem', border:'1px solid #e2e8f0', gap:'1rem' }}>
                <div style={{ display:'flex', gap:'0.75rem', alignItems:'center' }}>
                  <img src={placeImg(p)} alt={p.name} crossOrigin="anonymous" style={{ width:48, height:48, borderRadius:'0.5rem', objectFit:'cover', flexShrink:0 }} onError={e => e.target.style.display='none'} />
                  <div>
                    <p style={{ fontWeight:600, color:'#0f172a', fontSize:'0.875rem', margin:0 }}>{p.name}</p>
                    <p style={{ color:'#64748b', fontSize:'0.75rem', margin:0 }}>{p.address || 'Near route'} {p.rating ? `★${Number(p.rating).toFixed(1)}` : ''} {p.estimatedCost ? `• ₹${Math.round(p.estimatedCost)}` : ''}</p>
                  </div>
                </div>
                <a href={viewUrl(p)} target="_blank" rel="noopener noreferrer"
                  style={{ padding:'0.4rem 0.75rem', background:'#f0fdfa', color:'#0f766e', borderRadius:'0.5rem', fontSize:'0.75rem', fontWeight:600, textDecoration:'none', border:'1px solid #99f6e4', whiteSpace:'nowrap' }}>View →</a>
              </div>
            ))}
            <button onClick={() => handleShowMore(selectedCat)} style={{ padding:'0.6rem', border:'1px dashed #cbd5e1', borderRadius:'0.75rem', color:'#64748b', fontSize:'0.8rem', cursor:'pointer', background:'none' }}>Load More →</button>
          </div>
        )}
      </div>

      {/* ── Actions ── */}
      <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'0.75rem' }}>
        {data.tripId && <button onClick={handleSave} disabled={saving} className="btn-primary">{saving ? 'Saving…' : '💾 Save to History'}</button>}
        <button onClick={downloadPDF} disabled={pdfLoading} className="btn-secondary" style={{ color:'#0f172a' }}>
          {pdfLoading ? '⏳ Generating…' : '📄 Download PDF with Images'}
        </button>
        <button onClick={() => navigate('/rescue')} className="btn-secondary" style={{ color:'#ea580c', borderColor:'#fed7aa' }}>🚨 Rescue Mode</button>
        <button onClick={() => navigate('/plan')} className="btn-secondary">🗺️ Plan Another Trip</button>
      </div>
    </div>
  )
}
