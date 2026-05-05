import { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { tripAPI } from '../services/api'

export default function TripResult() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [showMap, setShowMap] = useState(true)

  useEffect(() => {
    // Check if rescue data was passed via location state
    if (location.state?.rescueData) {
      setData(location.state.rescueData)
      setLoading(false)
      return
    }

    if (!id || id === 'rescue') {
      setError('No trip ID available.')
      setLoading(false)
      return
    }

    tripAPI.getTrip(id).then(res => {
      setData(res.data)
    }).catch(err => {
      setError('Failed to load trip details.')
    }).finally(() => setLoading(false))
  }, [id, location.state])

  const handleSave = async () => {
    if (!data?.tripId) return
    setSaving(true)
    try {
      await tripAPI.save(data.tripId)
      alert('Itinerary saved to history successfully!')
    } catch(err) {
      alert('Failed to save itinerary.')
    } finally { setSaving(false) }
  }

  // Build Google Maps directions URL with waypoints
  const buildGoogleMapsDirectionsUrl = () => {
    if (!data) return '#'
    const origin = encodeURIComponent(data.startLocation)
    const destination = encodeURIComponent(data.destination)
    const places = data.suggestedPlaces || []
    
    let waypointsParam = ''
    if (places.length > 0) {
      const wpList = places.map(p => {
        if (p.latitude && p.longitude) return `${p.latitude},${p.longitude}`
        return encodeURIComponent(p.name + ' ' + (p.address || ''))
      }).join('|')
      waypointsParam = `&waypoints=${wpList}`
    }
    
    return `https://www.google.com/maps/dir/?api=1&origin=${origin}&destination=${destination}${waypointsParam}&travelmode=driving`
  }

  // Build Google Maps embed URL for directions
  const buildEmbedUrl = () => {
    if (!data) return ''
    const origin = encodeURIComponent(data.startLocation)
    const destination = encodeURIComponent(data.destination)
    const places = data.suggestedPlaces || []
    
    let waypointsParam = ''
    if (places.length > 0) {
      const wpList = places.map(p => {
        if (p.latitude && p.longitude) return `${p.latitude},${p.longitude}`
        return p.name
      }).join('|')
      waypointsParam = `&waypoints=${encodeURIComponent(wpList)}`
    }
    
    // Use Google Maps embed with directions mode
    return `https://www.google.com/maps/embed/v1/directions?key=AIzaSyBFw0Qbyq9zTFTd-tUY6dZWTgaQzuU17R8&origin=${origin}&destination=${destination}&mode=driving&avoid=tolls`
  }

  // Build Google Maps search/place URL for a specific place
  const getPlaceGoogleMapsUrl = (place) => {
    if (place.latitude && place.longitude) {
      return `https://www.google.com/maps/search/?api=1&query=${place.latitude},${place.longitude}`
    }
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(place.name + ' ' + (place.address || ''))}`
  }

  // Build Google Maps directions URL from current place to next place
  const getDirectionsToPlace = (place) => {
    if (!data) return '#'
    const destination = place.latitude && place.longitude 
      ? `${place.latitude},${place.longitude}` 
      : encodeURIComponent(place.name + ' ' + (place.address || ''))
    return `https://www.google.com/maps/dir/?api=1&destination=${destination}&travelmode=driving`
  }

  // Get nearby places search URL for a specific category near a place
  const getNearbySearchUrl = (place, category) => {
    const query = encodeURIComponent(category)
    if (place.latitude && place.longitude) {
      return `https://www.google.com/maps/search/${query}/@${place.latitude},${place.longitude},15z`
    }
    return `https://www.google.com/maps/search/${query}+near+${encodeURIComponent(place.name)}`
  }

  if (loading) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <div className="spinner-teal w-10 h-10 border-4"></div>
      <p className="text-slate-500 font-medium animate-pulse">Running constraint engine & scoring algorithms...</p>
    </div>
  )

  if (error) return <div className="p-6 bg-red-50 text-red-600 rounded-xl font-medium">{error}</div>
  if (!data) return null

  const places = data.suggestedPlaces || []
  const removed = data.removedPlaces || []
  const nearbyCategories = ['Restaurants', 'Hotels', 'ATM', 'Fuel Station', 'Hospital', 'Parking']

  return (
    <div className="max-w-5xl mx-auto fade-in pb-20">
      {/* Header Banner */}
      <div className="card bg-[#0f766e] border-none p-8 mb-8 relative overflow-hidden shadow-lg">
        <div className="absolute right-0 top-0 w-64 h-64 bg-teal-500/30 rounded-full blur-3xl -translate-y-1/2 translate-x-1/3"></div>
        <div className="relative z-10">
          {(data.demoMode || data.fallback) && (
            <div className="inline-flex items-center px-2.5 py-1 rounded-full bg-orange-500/20 text-orange-200 border border-orange-500/30 text-xs font-bold uppercase tracking-wider mb-4">
              {data.fallback ? 'Fallback Data' : 'Demo Data'}
            </div>
          )}
          <div className="flex items-center gap-3 text-teal-100 text-sm font-medium mb-3">
            <span style={{ color: '#99f6e4' }}>START</span> <span className="w-10 h-[1px] bg-teal-400/50"></span> <span style={{ color: '#99f6e4' }}>DESTINATION</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-bold mb-4 flex items-center gap-3" style={{ fontFamily: 'Outfit', color: '#ffffff' }}>
            {data.startLocation} <span style={{ color: 'rgba(255,255,255,0.5)' }}>→</span> {data.destination}
          </h1>
          <div className="flex flex-wrap gap-4 text-sm font-medium" style={{ color: '#f0fdfa' }}>
            <div className="flex items-center gap-1.5"><span style={{ opacity: 0.7 }}>⏱️</span> {data.estimatedTravelTime || 'Est. 4 hours'}</div>
            <div className="flex items-center gap-1.5"><span style={{ opacity: 0.7 }}>💰</span> ₹{data.estimatedTotalCost || 0} est. stops cost</div>
            <div className="flex items-center gap-1.5"><span style={{ opacity: 0.7 }}>📍</span> {places.length} stops</div>
            {data.weatherSummary && (
              <div className="flex items-center gap-1.5 ml-auto bg-teal-800/50 px-3 py-1 rounded-full border border-teal-600/50" style={{ color: '#ccfbf1' }}>
                {data.weatherSummary}
              </div>
            )}
            {!data.weatherSummary && data.weather && (
              <div className="flex items-center gap-1.5 ml-auto bg-teal-800/50 px-3 py-1 rounded-full border border-teal-600/50" style={{ color: '#ccfbf1' }}>
                <span style={{ opacity: 0.9 }}>{data.weather.rainy ? '🌧️' : '⛅'}</span>
                {data.weather.temperature}°C {data.weather.condition}
              </div>
            )}
          </div>
          {data.routeSummary && (
            <p className="text-sm mt-3" style={{ color: '#99f6e4', opacity: 0.8 }}>📍 {data.routeSummary}</p>
          )}
          
          {/* Open full directions in Google Maps */}
          <div className="mt-5 flex flex-wrap gap-3">
            <a href={buildGoogleMapsDirectionsUrl()} target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-sm transition-all hover:scale-[1.02] active:scale-[0.98] shadow-md"
              style={{ backgroundColor: '#ffffff', color: '#0f766e' }}>
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
              Open Directions in Google Maps
            </a>
            <button onClick={() => setShowMap(!showMap)}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-sm transition-all hover:scale-[1.02] active:scale-[0.98] border"
              style={{ backgroundColor: 'rgba(255,255,255,0.15)', color: '#ffffff', borderColor: 'rgba(255,255,255,0.3)' }}>
              {showMap ? '🗺️ Hide Map' : '🗺️ Show Map'}
            </button>
          </div>
        </div>
      </div>

      {/* Google Maps Embed - Directions View */}
      {showMap && (
        <div className="card mb-8 overflow-hidden border-2 border-teal-100 shadow-lg fade-in">
          <div className="bg-slate-50 px-5 py-3 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-lg">🗺️</span>
              <h3 className="text-sm font-bold text-slate-700" style={{ fontFamily: 'Outfit' }}>Route Map & Directions</h3>
            </div>
            <a href={buildGoogleMapsDirectionsUrl()} target="_blank" rel="noopener noreferrer"
              className="text-xs font-bold text-teal-600 hover:text-teal-800 transition-colors flex items-center gap-1">
              Open Full Map ↗
            </a>
          </div>
          <div className="relative w-full" style={{ paddingBottom: '50%', minHeight: '350px' }}>
            <iframe
              className="absolute inset-0 w-full h-full"
              style={{ border: 0 }}
              loading="lazy"
              allowFullScreen
              referrerPolicy="no-referrer-when-downgrade"
              src={`https://www.google.com/maps/embed/v1/directions?key=AIzaSyBFw0Qbyq9zTFTd-tUY6dZWTgaQzuU17R8&origin=${encodeURIComponent(data.startLocation)}&destination=${encodeURIComponent(data.destination)}&mode=driving`}
            />
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Col: Timeline */}
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-xl font-bold text-slate-800" style={{ fontFamily: 'Outfit' }}>Optimized Route Stops</h2>
            <span className="badge badge-slate">{places.length} stops selected</span>
          </div>

          {places.length === 0 ? (
            <div className="card p-8 text-center text-slate-500">No suitable stops found along this route based on your constraints.</div>
          ) : (
            <div className="relative border-l-2 border-slate-200 ml-4 space-y-8 pb-4">
              {places.map((place, idx) => (
                <div key={idx} className="relative pl-8 fade-in" style={{ animationDelay: `${idx * 0.1}s` }}>
                  {/* Timeline Node */}
                  <div className="absolute left-[-9px] top-1 w-4 h-4 rounded-full bg-white border-4 border-teal-500 shadow-sm"></div>
                  
                  <div className="card p-5 hover:border-teal-300 transition-colors group">
                    <div className="flex justify-between items-start mb-2">
                      <div className="flex items-center gap-2">
                        <span className="badge badge-teal">{place.category || 'general'}</span>
                        {place.openNow === false && <span className="badge badge-amber">May be closed</span>}
                      </div>
                      <div className="flex items-center gap-2 text-xs font-bold text-slate-400">
                        {place.score != null && place.score > 0 && <span className="text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-100">Score: {Math.round(place.score)}</span>}
                        {place.rating != null && <span className="text-amber-600">★ {typeof place.rating === 'number' ? place.rating.toFixed(1) : place.rating}</span>}
                      </div>
                    </div>
                    
                    <h3 className="text-lg font-bold text-slate-800 mb-1">{place.name}</h3>
                    <p className="text-sm text-slate-500 mb-3">{place.address || 'Near route'}</p>
                    
                    {place.reason && (
                      <div className="card-inner p-3 mb-3 text-sm text-slate-700 flex items-start gap-2">
                        <span className="text-teal-600 font-bold">↳</span> {place.reason}
                      </div>
                    )}
                    
                    <div className="flex items-center gap-4 text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">
                      {place.distanceFromRoute != null && <span className="flex items-center gap-1">📍 {place.distanceFromRoute} km deviation</span>}
                      {place.estimatedCost != null && <span className="flex items-center gap-1">💵 ₹{place.estimatedCost}</span>}
                    </div>

                    {/* Google Maps Action Buttons */}
                    <div className="flex flex-wrap gap-2 pt-3 border-t border-slate-100">
                      <a href={getPlaceGoogleMapsUrl(place)} target="_blank" rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-teal-50 text-teal-700 text-xs font-semibold hover:bg-teal-100 transition-colors border border-teal-100">
                        <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
                        View on Map
                      </a>
                      <a href={getDirectionsToPlace(place)} target="_blank" rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-50 text-blue-700 text-xs font-semibold hover:bg-blue-100 transition-colors border border-blue-100">
                        <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor"><path d="M21.71 11.29l-9-9c-.39-.39-1.02-.39-1.41 0l-9 9c-.39.39-.39 1.02 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9c.39-.38.39-1.01 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z"/></svg>
                        Get Directions
                      </a>
                      <a href={getNearbySearchUrl(place, 'restaurants cafes')} target="_blank" rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-50 text-amber-700 text-xs font-semibold hover:bg-amber-100 transition-colors border border-amber-100">
                        🍽️ Nearby Food
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Explore Nearby Places Section */}
          {places.length > 0 && (
            <div className="card p-6 border-t-4 border-t-emerald-500 mt-8">
              <h3 className="font-bold text-slate-800 flex items-center gap-2 mb-4" style={{ fontFamily: 'Outfit' }}>
                <span className="text-emerald-500">📍</span> Explore Nearby Places
              </h3>
              <p className="text-sm text-slate-500 mb-4">Discover what's around your stops — restaurants, fuel, ATMs, and more.</p>
              
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                {nearbyCategories.map(category => {
                  // Use the midpoint stop or first stop as reference
                  const refPlace = places[Math.floor(places.length / 2)] || places[0]
                  const url = getNearbySearchUrl(refPlace, category)
                  
                  const icons = {
                    'Restaurants': '🍽️', 'Hotels': '🏨', 'ATM': '🏧',
                    'Fuel Station': '⛽', 'Hospital': '🏥', 'Parking': '🅿️'
                  }
                  
                  return (
                    <a key={category} href={url} target="_blank" rel="noopener noreferrer"
                      className="card p-4 text-center hover:border-emerald-300 hover:bg-emerald-50/50 transition-all group cursor-pointer">
                      <span className="text-2xl block mb-2">{icons[category] || '📍'}</span>
                      <span className="text-sm font-semibold text-slate-700 group-hover:text-emerald-700 transition-colors">{category}</span>
                    </a>
                  )
                })}
              </div>
            </div>
          )}
        </div>

        {/* Right Col: AI & Removed & Directions */}
        <div className="space-y-6">
          {/* Directions Overview */}
          <div className="card p-6 border-t-4 border-t-blue-500">
            <h3 className="font-bold text-slate-800 flex items-center gap-2 mb-4" style={{ fontFamily: 'Outfit' }}>
              <span className="text-blue-500">🧭</span> Trip Directions
            </h3>
            <div className="space-y-3 mb-4">
              <div className="flex items-center gap-3 p-3 bg-emerald-50 rounded-xl border border-emerald-100">
                <div className="w-8 h-8 rounded-full bg-emerald-500 flex items-center justify-center flex-shrink-0">
                  <span className="text-white text-xs font-bold">A</span>
                </div>
                <div className="min-w-0">
                  <p className="text-[10px] text-emerald-600 font-bold uppercase tracking-wider">Start</p>
                  <p className="text-sm font-semibold text-slate-700 truncate">{data.startLocation}</p>
                </div>
              </div>
              
              {places.map((place, idx) => (
                <div key={idx} className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100 hover:border-teal-200 transition-colors">
                  <div className="w-8 h-8 rounded-full bg-teal-500 flex items-center justify-center flex-shrink-0">
                    <span className="text-white text-xs font-bold">{idx + 1}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-slate-700 truncate">{place.name}</p>
                    <p className="text-[11px] text-slate-400 truncate">{place.category}</p>
                  </div>
                  <a href={getDirectionsToPlace(place)} target="_blank" rel="noopener noreferrer" 
                    className="text-teal-600 hover:text-teal-800 flex-shrink-0" title="Get directions">
                    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor"><path d="M21.71 11.29l-9-9c-.39-.39-1.02-.39-1.41 0l-9 9c-.39.39-.39 1.02 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9c.39-.38.39-1.01 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z"/></svg>
                  </a>
                </div>
              ))}
              
              <div className="flex items-center gap-3 p-3 bg-red-50 rounded-xl border border-red-100">
                <div className="w-8 h-8 rounded-full bg-red-500 flex items-center justify-center flex-shrink-0">
                  <span className="text-white text-xs font-bold">B</span>
                </div>
                <div className="min-w-0">
                  <p className="text-[10px] text-red-600 font-bold uppercase tracking-wider">Destination</p>
                  <p className="text-sm font-semibold text-slate-700 truncate">{data.destination}</p>
                </div>
              </div>
            </div>

            <a href={buildGoogleMapsDirectionsUrl()} target="_blank" rel="noopener noreferrer"
              className="btn-primary w-full text-sm gap-2">
              <svg className="w-4 h-4" viewBox="0 0 24 24" fill="currentColor"><path d="M21.71 11.29l-9-9c-.39-.39-1.02-.39-1.41 0l-9 9c-.39.39-.39 1.02 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9c.39-.38.39-1.01 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z"/></svg>
              Navigate Full Route
            </a>
          </div>

          {/* AI Summary */}
          <div className="card p-6 border-t-4 border-t-indigo-500">
            <h3 className="font-bold text-slate-800 flex items-center gap-2 mb-4" style={{ fontFamily: 'Outfit' }}>
              <span className="text-indigo-500">✨</span> AI Itinerary Summary
            </h3>
            {data.aiSummary ? (
              <div className="prose prose-sm prose-slate max-w-none text-slate-600 text-sm leading-relaxed whitespace-pre-line">
                {data.aiSummary}
              </div>
            ) : (
              <p className="text-sm text-slate-500 italic">No AI explanation available.</p>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col gap-3">
            {data.tripId && (
              <button onClick={handleSave} disabled={saving} className="btn-primary w-full">
                {saving ? 'Saving...' : '💾 Save Itinerary to History'}
              </button>
            )}
            <button onClick={() => navigate('/rescue')} className="btn-secondary w-full text-orange-600 hover:text-orange-700 hover:bg-orange-50 hover:border-orange-200">
              🚨 Trigger Rescue Mode
            </button>
            <button onClick={() => navigate('/plan')} className="btn-secondary w-full text-slate-600">
              🗺️ Plan Another Trip
            </button>
          </div>

          {/* Filtered Out Places */}
          {removed.length > 0 && (
            <div className="card p-6 bg-slate-50 border-dashed">
              <h3 className="font-bold text-sm text-slate-700 mb-4 uppercase tracking-wider flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-slate-200 flex items-center justify-center text-slate-500 text-[10px]">!</span>
                Filtered by Constraints
              </h3>
              <div className="space-y-3">
                {removed.map((rm, idx) => (
                  <div key={idx} className="text-sm pb-3 border-b border-slate-200 last:border-0 last:pb-0">
                    <p className="font-semibold text-slate-600 line-through opacity-70">{rm.name}</p>
                    <p className="text-[11px] font-medium text-red-500 mt-0.5">{rm.reasonRemoved}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
