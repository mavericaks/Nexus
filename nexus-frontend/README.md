# Nexus Frontend — Next.js 15 Support Workspace

<div align="center">

[![Next.js](https://img.shields.io/badge/Next.js-15-black?style=for-the-badge&logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Framer Motion](https://img.shields.io/badge/Framer_Motion-13-black?style=for-the-badge&logo=framer&logoColor=blue)](https://www.framer.com/motion/)

<p align="center">
  <strong>A high-performance, dark glassmorphic customer support workspace featuring live AI triage animations, interactive command palette navigation (⌘K), and optimistic state management.</strong>
</p>

</div>

---

## 🎨 Design System & Highlights

- **Custom Dark Glassmorphic Design System**: Built with modern CSS custom properties (design tokens), backdrop blurs, luminous borders, and subtle glow effects without external heavyweight UI libraries.
- **Global Command Palette (`⌘K` / `Ctrl+K`)**: Instant keyboard navigation across tickets, knowledge base articles, team rosters, and real-time role-switching.
- **Live AI Triage Simulation**: Animated multi-stage scanning visualization (Analyzing Intent $\rightarrow$ Querying `pgvector` $\rightarrow$ LLM Synthesis $\rightarrow$ Confidence Derivation).
- **Optimistic State Machine Transitions**: Immediate UI feedback for ticket progressions with automatic rollback handling.
- **Modular Feature Architecture**: 18 specialized components partitioned cleanly into `components/features/`, `components/layout/`, and `components/ui/`.

---

## 📂 Component & Directory Structure

```
nexus-frontend/src/
├── app/
│   ├── (dashboard)/
│   │   ├── dashboard/page.tsx       # Live KPI analytics, ticket distribution, recent activity
│   │   ├── tickets/page.tsx         # Filterable ticket queue with search, category & status pills
│   │   ├── tickets/new/page.tsx     # Ticket submission wizard
│   │   ├── tickets/[id]/page.tsx    # Interactive ticket detail & triage workspace
│   │   ├── knowledge/page.tsx       # Semantic vector search tester & KB article management
│   │   ├── notifications/page.tsx   # User notification inbox with read/unread toggles
│   │   ├── settings/page.tsx        # Canned response templates & preferences
│   │   ├── team/page.tsx            # Multi-tenant user roster & RBAC assignments
│   │   └── layout.tsx               # Global dashboard shell (Sidebar, Header, CommandPalette)
│   ├── globals.css                  # Design tokens, glassmorphism utilities & animations
│   └── page.tsx                     # Authentication portal & mock tenant selector
├── components/
│   ├── features/tickets/            # TicketTable, TriagePanel, Timeline, NotesSection, TicketFilters
│   ├── layout/                      # Sidebar, Header, CommandPalette (⌘K)
│   └── ui/                          # Badge, Button, Card, EmptyState, Pagination, Skeleton
├── context/
│   └── AuthContext.tsx              # JWT token lifecycle, tenant switching, role permissions
└── lib/
    ├── api.ts                       # Typed API client covering all backend endpoints
    └── auth.ts                      # JWT decoding & localStorage token persistence
```

---

## 🚀 Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Configure Environment
Create a `.env.local` file (optional if using default `http://localhost:8080`):
```properties
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. Run Development Server
```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 🛠️ Scripts

- `npm run dev` — Starts Next.js development server with hot-reload
- `npm run build` — Generates optimized production build
- `npm run start` — Boots production server
- `npm run lint` — Runs ESLint checks
