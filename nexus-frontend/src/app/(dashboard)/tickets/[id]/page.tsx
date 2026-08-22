'use client';

import { useAuth } from '@/context/AuthContext';
import { api, Ticket, TicketEvent, TicketNote, TriageResult, ApiError } from '@/lib/api';
import { useEffect, useState, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Activity, MessageSquare } from 'lucide-react';
import { TicketHeader } from '@/components/features/tickets/TicketHeader';
import { TicketDescription } from '@/components/features/tickets/TicketDescription';
import { TicketEditForm } from '@/components/features/tickets/TicketEditForm';
import { TriagePanel } from '@/components/features/tickets/TriagePanel';
import { TransitionPanel } from '@/components/features/tickets/TransitionPanel';
import { Timeline } from '@/components/features/tickets/Timeline';
import { NotesSection } from '@/components/features/tickets/NotesSection';
import styles from './page.module.css';

const TRANSITIONS: Record<string, string[]> = {
  NEW: ['CLASSIFIED'],
  CLASSIFIED: ['AI_DRAFTED', 'ESCALATED'],
  AI_DRAFTED: ['AUTO_RESOLVED', 'ESCALATED'],
  AUTO_RESOLVED: ['CLOSED'],
  ESCALATED: ['IN_PROGRESS'],
  IN_PROGRESS: ['RESOLVED', 'ESCALATED'],
  RESOLVED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
};

export default function TicketDetailPage() {
  const { user } = useAuth();
  const router = useRouter();
  const params = useParams();
  const ticketId = params.id as string;

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [events, setEvents] = useState<TicketEvent[]>([]);
  const [notes, setNotes] = useState<TicketNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'timeline' | 'notes'>('timeline');

  const [triageResult, setTriageResult] = useState<TriageResult | null>(null);
  const [addingNote, setAddingNote] = useState(false);
  const [transitioning, setTransitioning] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Edit State
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const loadTicket = useCallback(async () => {
    if (!user) return;
    try {
      const [t, e, n] = await Promise.all([
        api.getTicket(user.tenantId, ticketId),
        api.getTicketEvents(user.tenantId, ticketId).catch(() => []),
        api.getTicketNotes(user.tenantId, ticketId).catch(() => []),
      ]);
      setTicket(t);
      setEvents(e);
      setNotes(n);

      if (t.aiConfidenceScore !== null) {
        setTriageResult({
          category: t.category || '',
          priority: t.priority || '',
          confidenceScore: t.aiConfidenceScore,
          suggestedReply: t.aiSuggestedResponse || '',
          reasoning: '',
          autoResolvable: false,
        });
      }
    } catch (err) {
      console.error('Failed to load ticket:', err);
    } finally {
      setLoading(false);
    }
  }, [user, ticketId]);

  useEffect(() => {
    if (!user || !ticketId) return;
    loadTicket();
  }, [user, ticketId, loadTicket]);

  async function handleSaveEdit(subject: string, description: string) {
    if (!user || !ticket) return;
    setIsSaving(true);
    setActionError(null);
    try {
      const updated = await api.updateTicket(user.tenantId, ticket.id, {
        subject,
        description,
        version: ticket.version,
      });
      setTicket(updated);
      setIsEditing(false);
      // Reload events to show update event if any
      const newEvents = await api.getTicketEvents(user.tenantId, ticketId).catch(() => []);
      setEvents(newEvents);
    } catch (err: unknown) {
      setActionError((err as ApiError)?.message || 'Failed to update ticket');
    } finally {
      setIsSaving(false);
    }
  }

  async function handleTransition(targetStatus: string) {
    if (!user || !ticket) return;
    setTransitioning(true);
    setActionError(null);
    try {
      const updated = await api.transitionTicket(user.tenantId, ticket.id, {
        targetStatus,
        version: ticket.version,
      });
      setTicket(updated);
      const newEvents = await api.getTicketEvents(user.tenantId, ticketId).catch(() => []);
      setEvents(newEvents);
    } catch (err: unknown) {
      setActionError((err as ApiError)?.message || 'Transition failed');
    } finally {
      setTransitioning(false);
    }
  }

  async function handleAddNote(content: string) {
    if (!user || !ticket) return;
    setAddingNote(true);
    setActionError(null);
    try {
      const note = await api.addTicketNote(user.tenantId, ticket.id, content);
      setNotes((prev) => [...prev, note]);
    } catch (err: unknown) {
      setActionError((err as ApiError)?.message || 'Failed to add note');
    } finally {
      setAddingNote(false);
    }
  }

  async function handleDelete() {
    if (!user || !ticket) return;
    if (!confirm('Are you sure you want to delete this ticket?')) return;
    try {
      await api.deleteTicket(user.tenantId, ticket.id);
      router.push('/tickets');
    } catch (err: unknown) {
      setActionError((err as ApiError)?.message || 'Failed to delete ticket');
    }
  }

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={`skeleton ${styles.skTitle}`} />
        <div className={styles.grid}>
          <div className={`glass ${styles.mainPanel}`}>
            <div className={`skeleton`} style={{ height: 200 }} />
          </div>
          <div className={`glass ${styles.sidePanel}`}>
            <div className={`skeleton`} style={{ height: 300 }} />
          </div>
        </div>
      </div>
    );
  }

  if (!ticket) {
    return (
      <div className={styles.page}>
        <p>Ticket not found.</p>
        <button className="btn btn--secondary" onClick={() => router.push('/tickets')}>
          Back to Tickets
        </button>
      </div>
    );
  }

  const allowedTransitions = TRANSITIONS[ticket.status] || [];
  const canDelete = user?.role === 'OWNER' || user?.role === 'ADMIN';

  return (
    <div className={styles.page}>
      {isEditing ? (
        <TicketEditForm
          ticket={ticket}
          onSave={handleSaveEdit}
          onCancel={() => setIsEditing(false)}
          isSaving={isSaving}
        />
      ) : (
        <>
          <TicketHeader
            ticket={ticket}
            canDelete={canDelete}
            onDelete={handleDelete}
            onEditClick={() => setIsEditing(true)}
          />
        </>
      )}

      {actionError && (
        <div className={styles.actionError} role="alert">
          <p>{actionError}</p>
          <button className="btn btn--ghost btn--sm" onClick={() => setActionError(null)} aria-label="Dismiss error">✕</button>
        </div>
      )}

      <div className={styles.grid}>
        {/* ─── Main Panel ──────────────────────────────────────── */}
        <div className={styles.mainCol}>
          {!isEditing && <TicketDescription ticket={ticket} />}
          
          <TriagePanel
            ticket={ticket}
            initialResult={triageResult}
            onTriageComplete={loadTicket}
            onError={setActionError}
          />
          
          <TransitionPanel
            allowedTransitions={allowedTransitions}
            isTransitioning={transitioning}
            onTransition={handleTransition}
          />
        </div>

        {/* ─── Side Panel ──────────────────────────────────────── */}
        <div className={styles.sideCol}>
          <div className={`glass glass--static ${styles.sidePanel}`}>
            <div className={styles.tabs}>
              <button
                className={`${styles.tab} ${activeTab === 'timeline' ? styles.tabActive : ''}`}
                onClick={() => setActiveTab('timeline')}
              >
                <Activity size={14} /> Timeline
              </button>
              <button
                className={`${styles.tab} ${activeTab === 'notes' ? styles.tabActive : ''}`}
                onClick={() => setActiveTab('notes')}
              >
                <MessageSquare size={14} /> Notes ({notes.length})
              </button>
            </div>

            {activeTab === 'timeline' && (
              <Timeline events={events} />
            )}

            {activeTab === 'notes' && (
              <NotesSection
                notes={notes}
                isAdding={addingNote}
                onAddNote={handleAddNote}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
