import { useState, useRef, useEffect } from 'react'
import { chatAPI } from '../services/api'

export default function Chatbot() {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState([{ sender: 'ai', text: 'Hi! I am Vyntra AI. How can I help you optimize your trip?' }])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const messagesEndRef = useRef(null)

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  const handleSend = async () => {
    if (!input.trim() || loading) return
    const userMsg = input.trim()
    const newMsgs = [...messages, { sender: 'user', text: userMsg }]
    setMessages(newMsgs)
    setInput('')
    setLoading(true)

    try {
      const res = await chatAPI.send(userMsg)
      setMessages([...newMsgs, { sender: 'ai', text: res.data.reply }])
    } catch (err) {
      // Fallback to client-side response if backend fails
      let reply = generateFallbackReply(userMsg)
      setMessages([...newMsgs, { sender: 'ai', text: reply }])
    } finally {
      setLoading(false)
    }
  }

  const generateFallbackReply = (msg) => {
    const lower = msg.toLowerCase()
    if (lower.includes('suggest') || lower.includes('place')) return "I can find great stops along your route. Check out the Plan Trip page to discover cafes, temples, and more without deviating far."
    if (lower.includes('optimize') || lower.includes('route')) return "My route optimization engine ranks places based on distance deviation, ratings, and your personal constraints."
    if (lower.includes('why') || lower.includes('selected')) return "Places are selected using a multi-factor score: (Rating × 20) - (Route Deviation × 15) - (Cost factor) + Interest & Weather bonuses. We remove anything closed or unsafe."
    if (lower.includes('replan') || lower.includes('rescue')) return "If you hit an issue like rain or fatigue, use Rescue Mode! I'll instantly generate a new nearby itinerary."
    if (lower.includes('plan') || lower.includes('trip')) return "Go to 'Plan Trip' from the sidebar. Enter your start & destination, set constraints, pick interests, and hit Generate!"
    if (lower.includes('hello') || lower.includes('hi')) return "Hello! 👋 How can I help with your travel planning today?"
    return "I can help with trip planning, scoring explanations, rescue mode, and more. Just ask!"
  }

  const handleQuickAction = (text) => {
    setInput(text)
    setTimeout(() => {
      const btn = document.getElementById('chat-send-btn')
      if (btn) btn.click()
    }, 100)
  }

  if (!isOpen) {
    return (
      <button onClick={() => setIsOpen(true)} className="fixed bottom-6 right-6 w-14 h-14 bg-[#0f766e] hover:bg-[#115e59] text-white rounded-full shadow-lg flex items-center justify-center text-2xl z-50 transition-transform hover:scale-105">
        💬
      </button>
    )
  }

  return (
    <div className="fixed bottom-6 right-6 w-80 bg-white rounded-2xl shadow-2xl border border-slate-200 z-50 flex flex-col overflow-hidden fade-in">
      {/* Header */}
      <div className="bg-[#0f766e] p-4 flex justify-between items-center text-white">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center font-black text-sm">V</div>
          <div>
            <h3 className="font-bold text-sm leading-tight" style={{ fontFamily: 'Outfit' }}>Vyntra AI</h3>
            <p className="text-[10px] text-teal-100">Travel Assistant</p>
          </div>
        </div>
        <button onClick={() => setIsOpen(false)} className="text-white/70 hover:text-white p-1 text-xl leading-none">×</button>
      </div>

      {/* Messages */}
      <div className="h-80 overflow-y-auto p-4 bg-slate-50 flex flex-col gap-3">
        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[85%] ${m.sender === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai'}`}>
              {m.text}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="chat-bubble-ai flex items-center gap-2">
              <div className="flex gap-1">
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Quick Actions */}
      <div className="px-3 pt-2 pb-1 bg-white border-t border-slate-100 flex gap-2 overflow-x-auto">
        <button onClick={() => handleQuickAction('How to plan a trip?')} className="whitespace-nowrap text-[10px] font-medium bg-slate-100 hover:bg-slate-200 text-slate-600 px-3 py-1.5 rounded-full transition-colors">Plan trip</button>
        <button onClick={() => handleQuickAction('How does scoring work?')} className="whitespace-nowrap text-[10px] font-medium bg-slate-100 hover:bg-slate-200 text-slate-600 px-3 py-1.5 rounded-full transition-colors">Scoring</button>
        <button onClick={() => handleQuickAction('What is rescue mode?')} className="whitespace-nowrap text-[10px] font-medium bg-slate-100 hover:bg-slate-200 text-slate-600 px-3 py-1.5 rounded-full transition-colors">Rescue</button>
      </div>

      {/* Input */}
      <div className="p-3 bg-white flex gap-2">
        <input 
          type="text" 
          className="flex-1 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-800 focus:outline-none focus:border-teal-400 placeholder:text-slate-400"
          placeholder="Ask Vyntra..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
        />
        <button id="chat-send-btn" onClick={handleSend} disabled={loading} className="w-10 h-10 bg-[#0f766e] hover:bg-[#115e59] text-white rounded-xl flex items-center justify-center transition-colors disabled:opacity-50">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" /></svg>
        </button>
      </div>
    </div>
  )
}
