# VYNTRA – Adaptive Travel Intelligence System

## Overview

Vyntra is a full-stack intelligent travel planner that goes beyond normal trip planning apps. It discovers stops **directly along your driving route**, filters them through a constraint engine, scores them with a multi-factor algorithm, and provides AI-powered explanations. When things go wrong mid-trip, **Rescue Mode** instantly generates a new plan.

## Problem Statement

Existing trip planners show random places on a map without considering:
- Whether the place is actually along your driving route
- Your current budget, energy, or time constraints
- Real-time weather conditions
- What to do when plans fail mid-trip

## Solution

Vyntra combines **route-aware discovery**, **constraint filtering**, **scoring optimization**, and **dynamic replanning** into one system.

### Why Different from Normal Trip Planners

> "Vyntra is different from normal trip planners because it combines trip planning, route-aware on-the-way recommendations, and real-time rescue replanning. It uses constraint filtering, route-deviation scoring, and optimization logic before AI creates human-readable explanations."

## Features

### Plan (Phase 1)
- Enter any source and destination worldwide
- Set budget, time, energy level, travel group, and interests
- Vyntra analyzes the route and discovers stops within 2km deviation

### Flow (Phase 2)  
- **Constraint Engine**: Filters by rain, budget, energy, time, family-safety, and open status
- **Scoring Engine**: `score = (rating * 20) - (deviation * 15) - (cost * 0.05) + interestBonus + weatherBonus + openBonus - energyPenalty`
- **Stop Optimization**: Nearest-neighbor algorithm minimizes total travel distance

### Morph (Phase 3)
- **Rescue Mode**: Instant replanning when rain, fatigue, closures, or budget issues occur
- Re-applies all constraints with updated parameters
- Saves rescue plan as a trip for history

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18 + Vite + Tailwind CSS v4 |
| Backend | Spring Boot 3.2.5 + Java 17 |
| Database | MongoDB |
| Auth | JWT (JJWT 0.12.5) + BCrypt |
| HTTP Client | Spring WebFlux (WebClient) |
| External APIs | SerpApi, OpenWeatherMap, Gemini/OpenAI (all optional) |

## Architecture

```
Frontend (React + Vite, port 5173)
    ↓ REST API (JWT Bearer token)
Backend (Spring Boot, port 8091)
    ├── AuthController → AuthService → UserRepository → MongoDB
    ├── TripController → TripService → SerpApiService + WeatherService
    │                                → ConstraintEngineService
    │                                → RouteOptimizationService
    │                                → AiExplanationService
    │                                → FallbackDataService
    ├── RescueController → RescueService
    ├── ChatController (rule-based AI assistant)
    └── HealthController (MongoDB connectivity check)
```

## API Endpoints

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT |

### Trip Planning (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/trips/plan` | Generate trip plan |
| GET | `/api/trips/history` | Get user's trip history |
| GET | `/api/trips/{id}` | Get trip details by ID |
| POST | `/api/trips/{id}/save` | Save itinerary |

### Rescue (Protected)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/rescue/replan` | Emergency replanning |

### Other
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check (public) |
| POST | `/api/chat` | AI chatbot (protected) |
| GET | `/api/weather?location=X` | Weather data (public) |

## MongoDB Setup

### Local
```bash
# Start MongoDB
mongod

# Database "vyntra_db" is auto-created on first use
```

### MongoDB Atlas (for deployment)
1. Create a free cluster at https://cloud.mongodb.com
2. Create a database user
3. Whitelist your IP (or 0.0.0.0/0 for Render)
4. Get the connection string

## Local Development

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+
- MongoDB 6+ (running locally)

### 1. Start MongoDB
```bash
mongod
```

### 2. Start Backend
```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8091
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

### 4. Open App
Visit http://localhost:5173

## Environment Variables

### Backend (`application.properties` with env overrides)
```
MONGODB_URI=mongodb://localhost:27017/vyntra_db
JWT_SECRET=change-this-secret-key-min-32-chars
SERPAPI_KEY=demo          # Use "demo" for fallback data
WEATHER_API_KEY=demo      # Use "demo" for fallback weather
AI_API_KEY=demo           # Use "demo" for fallback AI summary
FRONTEND_URL=http://localhost:5173
```

### Frontend (`.env`)
```
VITE_API_BASE_URL=http://localhost:8091/api
```

## Deployment

### Backend on Render
1. Push `backend/` to Git
2. Create Web Service on Render
3. **Build Command**: `mvn clean package -DskipTests`
4. **Start Command**: `java -jar target/vyntra-backend-1.0.0.jar`
5. Set environment variables:
   - `MONGODB_URI` (MongoDB Atlas connection string)
   - `JWT_SECRET`
   - `SERPAPI_KEY` / `WEATHER_API_KEY` / `AI_API_KEY`
   - `FRONTEND_URL` (your Vercel URL)

### Frontend on Vercel
1. Push `frontend/` to Git
2. Import on Vercel
3. **Build Command**: `npm run build`
4. **Output Directory**: `dist`
5. Set `VITE_API_BASE_URL` to your Render backend URL
6. Add `vercel.json`:
```json
{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
```

## Demo Credentials

Register a new account, or use:
```
Email: demo@vyntra.com
Password: demo123456
```

### Mysore Demo Trip
Use "Try Mysore Demo" button on the Plan Trip page to auto-fill:
- Start: Mysore Railway Station
- Destination: Brindavan Gardens
- Budget: Rs.1000 | Time: 5h | Family trip

## Screenshots

> Screenshots placeholder — run the app and capture from http://localhost:5173

## Interview Explanation

"Vyntra is different from normal trip planners because it combines trip planning, route-aware on-the-way recommendations, and real-time rescue replanning. It uses constraint filtering, route-deviation scoring, and optimization logic before AI creates human-readable explanations. The backend uses Spring Boot with MongoDB for persistence, JWT for authentication, and WebClient for external API calls. The frontend is built with React, Vite, and Tailwind CSS. The system supports fallback demo data when API keys are unavailable, making it fully functional without external dependencies."

## License

MIT
