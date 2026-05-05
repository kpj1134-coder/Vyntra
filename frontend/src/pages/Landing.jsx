import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Landing() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col relative overflow-hidden">
      {/* Navbar */}
      <nav className="p-6 flex justify-between items-center relative z-20">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-teal-600 flex items-center justify-center text-white font-black text-lg shadow-md shadow-teal-600/20">V</div>
          <span className="heading text-xl">VYNTRA</span>
        </div>
        <div className="flex gap-4 items-center">
          {isAuthenticated ? (
            <Link to="/dashboard" className="btn-primary">Go to Dashboard</Link>
          ) : (
            <>
              <Link to="/login" className="text-slate-600 font-medium hover:text-teal-700 px-4 py-2 transition-colors">Login</Link>
              <Link to="/register" className="btn-primary">Get Started</Link>
            </>
          )}
        </div>
      </nav>

      {/* Hero */}
      <main className="flex-1 flex flex-col items-center justify-center text-center px-4 relative z-20 fade-in">
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-teal-50 border border-teal-100 text-teal-700 text-sm font-medium mb-8">
          <span className="w-2 h-2 rounded-full bg-teal-500 animate-pulse"></span>
          Adaptive Travel Intelligence System
        </div>
        
        <h1 className="text-5xl md:text-7xl font-bold text-slate-900 tracking-tight max-w-4xl mb-6" style={{ fontFamily: 'Outfit' }}>
          Travel smarter with <span className="text-teal-600">Vyntra</span>
        </h1>
        
        <p className="text-lg md:text-xl text-slate-600 max-w-2xl mb-12">
          Vyntra plans your route, discovers hidden gems along the way, and dynamically replans your trip when unexpected constraints like rain or delays occur.
        </p>
        
        <div className="flex flex-col sm:flex-row gap-4">
          <Link to={isAuthenticated ? "/plan" : "/register"} className="btn-primary px-8 py-4 text-lg">
            Get Started
          </Link>
          <Link to={isAuthenticated ? "/plan" : "/register"} className="btn-secondary px-8 py-4 text-lg">
            Try Demo
          </Link>
        </div>
      </main>

      {/* Plan / Flow / Morph Features */}
      <section id="features" className="py-20 px-8 relative z-20 max-w-7xl mx-auto">
        <div className="text-center mb-14">
          <h2 className="heading text-3xl mb-4">How Vyntra Works</h2>
          <p className="text-slate-500 max-w-xl mx-auto">Three intelligent phases power every trip you take</p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="card p-8 text-center hover:border-teal-300 transition-colors">
            <div className="w-16 h-16 mx-auto bg-teal-50 text-teal-600 rounded-2xl flex items-center justify-center text-3xl mb-6 shadow-inner">1</div>
            <h3 className="text-xl font-bold text-slate-800 mb-2" style={{ fontFamily: 'Outfit' }}>Plan</h3>
            <p className="text-sm text-slate-500 leading-relaxed mb-3">Enter your source and destination. Vyntra analyzes the route, discovers stops along the way, and builds an optimized itinerary filtered by your budget, energy, time, and interests.</p>
            <div className="inline-flex items-center gap-2 text-xs font-semibold text-teal-600 bg-teal-50 px-3 py-1.5 rounded-full">Route-Aware Discovery</div>
          </div>
          <div className="card p-8 text-center hover:border-emerald-300 transition-colors">
            <div className="w-16 h-16 mx-auto bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center text-3xl mb-6 shadow-inner">2</div>
            <h3 className="text-xl font-bold text-slate-800 mb-2" style={{ fontFamily: 'Outfit' }}>Flow</h3>
            <p className="text-sm text-slate-500 leading-relaxed mb-3">The constraint engine filters places in real-time. A scoring algorithm ranks each stop by rating, cost, deviation, weather, and your preferences. Only the best stops make it.</p>
            <div className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-600 bg-emerald-50 px-3 py-1.5 rounded-full">Constraint-Based Scoring</div>
          </div>
          <div className="card p-8 text-center hover:border-amber-300 transition-colors">
            <div className="w-16 h-16 mx-auto bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center text-3xl mb-6 shadow-inner">3</div>
            <h3 className="text-xl font-bold text-slate-800 mb-2" style={{ fontFamily: 'Outfit' }}>Morph</h3>
            <p className="text-sm text-slate-500 leading-relaxed mb-3">Hit a roadblock? Rain ruined plans? Rescue Mode instantly generates a new plan based on your current location, remaining budget, time, and energy level.</p>
            <div className="inline-flex items-center gap-2 text-xs font-semibold text-amber-600 bg-amber-50 px-3 py-1.5 rounded-full">Dynamic Replanning</div>
          </div>
        </div>
      </section>

      {/* Why Different */}
      <section className="py-16 px-8 relative z-20 max-w-5xl mx-auto">
        <div className="card p-10 bg-gradient-to-br from-slate-800 to-slate-900 border-none" style={{ color: '#ffffff' }}>
          <h2 className="text-2xl font-bold mb-4" style={{ fontFamily: 'Outfit', color: '#ffffff' }}>Why Vyntra is different</h2>
          <p className="text-sm leading-relaxed mb-6" style={{ color: '#cbd5e1' }}>
            Unlike normal trip planners that show random places on a map, Vyntra is route-aware. It discovers stops that are directly along your driving route with minimal deviation. 
            It then applies budget, energy, weather, and time constraints to filter out unsuitable options. 
            Finally, it scores and ranks every place so you get the best possible itinerary — and if anything goes wrong, Rescue Mode replans instantly.
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="text-center">
              <p className="text-2xl font-bold" style={{ color: '#2dd4bf' }}>2km</p>
              <p className="text-xs mt-1" style={{ color: '#94a3b8' }}>Max route deviation</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold" style={{ color: '#2dd4bf' }}>6</p>
              <p className="text-xs mt-1" style={{ color: '#94a3b8' }}>Constraint filters</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold" style={{ color: '#2dd4bf' }}>5-Factor</p>
              <p className="text-xs mt-1" style={{ color: '#94a3b8' }}>Scoring engine</p>
            </div>
            <div className="text-center">
              <p className="text-2xl font-bold" style={{ color: '#2dd4bf' }}>Instant</p>
              <p className="text-xs mt-1" style={{ color: '#94a3b8' }}>Rescue replanning</p>
            </div>
          </div>
        </div>
      </section>

      {/* Decorative background elements */}
      <div className="absolute top-[-10%] right-[-5%] w-[800px] h-[800px] bg-teal-100/40 rounded-full blur-[100px] pointer-events-none z-0"></div>
      <div className="absolute bottom-[-10%] left-[-5%] w-[600px] h-[600px] bg-emerald-100/40 rounded-full blur-[100px] pointer-events-none z-0"></div>
    </div>
  )
}
