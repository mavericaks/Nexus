# Phase 10: Frontend Component Structure

This document details the new frontend architecture established in Phase 10 to break down the monolithic Nexus application.

## Directory Structure

```
nexus-frontend/src/
├── app/                  # Next.js App Router Pages
├── components/           # Reusable Component Layer
│   ├── features/         # Feature-specific components
│   │   └── tickets/      # Ticket management features
│   ├── layout/           # Shared application layouts
│   └── ui/               # Core design primitives
├── context/              # React Context Providers
└── lib/                  # Utilities and API clients
```

## Shared UI Components (`src/components/ui/`)
These are pure presentation components that enforce the "glassmorphism" design system.
*   **Badge.tsx**: Consumes `STATUS_CONFIG` and `PRIORITY_CONFIG` to render consistent pill badges.
*   **Button.tsx**: Standardizes variants (`primary`, `secondary`, `ghost`, `danger`) and sizes.
*   **Card.tsx**: A reusable wrapper for the `.glass` panels.
*   **Skeleton.tsx**: A standard shimmer loading block.
*   **EmptyState.tsx**: Standardized placeholder for empty datasets or searches.
*   **Pagination.tsx**: Reusable pagination controls.

## Shared Layout Components (`src/components/layout/`)
These components manage the global shell and consume `useAuth()` directly.
*   **Sidebar.tsx**: Left navigation.
*   **Header.tsx**: Top bar with user profile and notifications.
*   **CommandPalette.tsx**: The global `⌘K` search palette.

*Note: These are currently consumed by `src/app/(dashboard)/layout.tsx`.*

## Monolithic Pages (Untouched)
The following pages remain monolithic and will be addressed in future phases:
*   `/dashboard/page.tsx`
*   `/knowledge/page.tsx`
*   `/notifications/page.tsx`
*   `/settings/page.tsx`
*   `/team/page.tsx`
*   `/tickets/new/page.tsx`

## Phase 10 Refactored Features
### `/tickets/page.tsx`
This route was successfully refactored and is now assembled from components in `src/components/features/tickets/`:
*   `TicketFilters.tsx`
*   `TicketTable.tsx`

## Proposed Future Refactoring
### `/tickets/[id]/page.tsx` (Ticket Detail)
This file remains a ~474-line monolith. In the next phase, it should be broken down into:
*   `TicketHeader.tsx` (Title, Badges, Delete action)
*   `TicketDescription.tsx` (Main body and metadata)
*   `TriagePanel.tsx` (AI triage logic and animation)
*   `TransitionPanel.tsx` (Status transition buttons)
*   `Timeline.tsx` (Events feed)
*   `NotesSection.tsx` (Internal notes feed and input)

## Architectural Decisions & Constraints
*   **No New Frameworks**: We deliberately avoided introducing Tailwind or shadcn/ui to preserve the exact visual identity and minimize rewrite risk.
*   **Preserved CSS Modules**: The existing `.module.css` and `globals.css` structure was maintained.
*   **Lint Errors Preserved**: The `react-hooks/set-state-in-effect` linting errors were deliberately left intact. This is a symptom of the data-fetching architecture (calling async functions that update state inside `useEffect`). Fixing this requires introducing a server-state library (like React Query), which is strictly out of scope for Phase 10.
