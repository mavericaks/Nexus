'use client';

import { useAuth } from '@/context/AuthContext';
import { api, Ticket, TicketEvent, TicketNote, TriageResult } from '@/lib/api';
import { timeAgo, formatDate, formatEnum } from '@/lib/utils';
import { STATUS_CONFIG, PRIORITY_CONFIG } from '@/lib/constants';
import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft, Brain, Send, MessageSquare, Clock, Activity,
  ChevronRight, Sparkles, AlertTriangle, Trash2, Loader2
} from 'lucide-react';
import styles from './page.module.css';

// Allowed transitions per status (mirrors TicketStateMachine)
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

  // Triage state
  const [triaging, setTriaging] = useState(false);
  const [triageResult, setTriageResult] = useState<TriageResult | null>(null);
  const [triagePhase, setTriagePhase] = useState(0);

  // Notes
  const [noteContent, setNoteContent] = useState('');
  const [addingNote, setAddingNote] = useState(false);

  // Transition
  const [transitioning, setTransitioning] = useState(false);

  useEffect(() => {
    if (!user || !ticketId) return;
    loadTicket();
  }, [user, ticketId]);

  async function loadTicket() {
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
          ticketId: t.id,
          suggestedCategory: t.category || '',
          suggestedPriority: t.priority || '',
          confidenceScore: t.aiConfidenceScore,
          suggestedResponse: t.aiSuggestedResponse || '',
        });
      }
    } catch (err) {
      console.error('Failed to load ticket:', err);
    } finally {
      setLoading(false);
    }
  }

  async function handleTriage() {
    if (!user || !ticket) return;
    setTriaging(true);
    setTriagePhase(1);

    // Simulate phased animation
    const phase2 = setTimeout(() => setTriagePhase(2), 800);
    const phase3 = setTimeout(() => setTriagePhase(3), 1800);

    try {
      const result = await api.triageTicket(user.tenantId, ticket.id);
      clearTimeout(phase2);
      clearTimeout(phase3);
      setTriagePhase(4);
      setTriageResult(result);

      // Reload ticket to get updated fields
      const updated = await api.getTicket(user.tenantId, ticket.id);
      setTicket(updated);

      // Reload events
      const newEvents = await api.getTicketEvents(user.tenantId, ticketId).catch(() => []);
      setEvents(newEvents);
    } catch (err: any) {
      alert(err.message || 'Triage failed');
    } finally {
      setTimeout(() => {
        setTriaging(false);
        setTriagePhase(0);
      }, 500);
    }
  }

  async function handleTransition(targetStatus: string) {
    if (!user || !ticket) return;
    setTransitioning(true);
    try {
      const updated = await api.transitionTicket(user.tenantId, ticket.id, {
        targetStatus,
        version: ticket.version,
      });
      setTicket(updated);
      const newEvents = await api.getTicketEvents(user.tenantId, ticketId).catch(() => []);
      setEvents(newEvents);
    } catch (err: any) {
      alert(err.message || 'Transition failed');
    } finally {
      setTransitioning(false);
    }
  }

  async function handleAddNote() {
    if (!user || !ticket || !noteContent.trim()) return;
    setAddingNote(true);
    try {
      const note = await api.addTicketNote(user.tenantId, ticket.id, noteContent.trim());
      setNotes((prev) => [...prev, note]);
      setNoteContent('');
    } catch (err: any) {
      alert(err.message || 'Failed to add note');
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
    } catch (err: any) {
      alert(err.message || 'Failed to delete ticket');
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
  const canTriage = ['NEW'].includes(ticket.status);
  const canDelete = user?.role === 'OWNER' || user?.role === 'ADMIN';

  return (
    <div className={styles.page}>
      <div className={styles.topBar}>
        <button className={styles.back} onClick={() => router.push('/tickets')}>
          <ArrowLeft size={16} /> Back
        </button>
        {canDelete && (
          <button className="btn btn--danger btn--sm" onClick={handleDelete}>
            <Trash2 size={14} /> Delete
          </button>
        )}
      </div>

      <div className={styles.titleRow}>
        <h1 className="page-title">{ticket.subject}</h1>
        <div className={styles.badges}>
          <span className={`badge ${STATUS_CONFIG[ticket.status]?.badgeClass || ''}`}>
            {STATUS_CONFIG[ticket.status]?.label || ticket.status}
          </span>
          {ticket.priority && (
            <span className={`badge ${PRIORITY_CONFIG[ticket.priority]?.badgeClass || ''}`}>
              {ticket.priority}
            </span>
          )}
          {ticket.category && (
            <span className={styles.categoryTag}>{formatEnum(ticket.category)}</span>
          )}
        </div>
      </div>

      <div className={styles.grid}>
        {/* ─── Main Panel ──────────────────────────────────────── */}
        <div className={styles.mainCol}>
          {/* Description */}
          <div className={`glass glass--static ${styles.section}`}>
            <h3 className="section-title">Description</h3>
            <p className={styles.description}>{ticket.description}</p>
            <div className={styles.meta}>
              <span><Clock size={14} /> Created {formatDate(ticket.createdAt)}</span>
              <span>Updated {timeAgo(ticket.updatedAt)}</span>
            </div>
          </div>

          {/* AI Triage */}
          <div className={`glass glass--static ${styles.section} ${styles.triageSection} ${triaging ? styles.triageActive : ''}`}>
            <div className={styles.triageHeader}>
              <h3 className="section-title">
                <Sparkles size={16} /> AI Triage
              </h3>
              {canTriage && (
                <button
                  className="btn btn--primary btn--sm"
                  onClick={handleTriage}
                  disabled={triaging}
                >
                  {triaging ? <Loader2 size={14} className={styles.spinning} /> : <Brain size={14} />}
                  {triaging ? 'Analyzing...' : 'Run Triage'}
                </button>
              )}
            </div>

            {/* Triage Animation Phases */}
            <AnimatePresence mode="wait">
              {triaging && (
                <motion.div
                  className={styles.triageAnim}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                >
                  <div className={styles.triagePulse} />
                  <p className={styles.triageText}>
                    {triagePhase === 1 && 'Analyzing ticket context...'}
                    {triagePhase === 2 && 'Searching knowledge base...'}
                    {triagePhase === 3 && 'Generating classification...'}
                  </p>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Triage Results */}
            {triageResult && !triaging && (
              <motion.div
                className={styles.triageResults}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
              >
                <div className={styles.triageGrid}>
                  <div className={styles.triageCard}>
                    <span className="label">Category</span>
                    <span className={styles.triageValue}>{formatEnum(triageResult.suggestedCategory)}</span>
                  </div>
                  <div className={styles.triageCard}>
                    <span className="label">Priority</span>
                    <span className={styles.triageValue} style={{
                      color: PRIORITY_CONFIG[triageResult.suggestedPriority]?.color
                    }}>
                      {triageResult.suggestedPriority}
                    </span>
                  </div>
                  <div className={styles.triageCard}>
                    <span className="label">Confidence</span>
                    <ConfidenceGauge score={triageResult.confidenceScore} />
                  </div>
                </div>

                {triageResult.suggestedResponse && (
                  <div className={styles.aiResponse}>
                    <span className="label">AI Suggested Response</span>
                    <div className={`glass--inset ${styles.responseBody}`}>
                      <p className="ai-text">{triageResult.suggestedResponse}</p>
                    </div>
                  </div>
                )}
              </motion.div>
            )}

            {!triageResult && !triaging && (
              <p className={styles.triagePlaceholder}>
                Run AI Triage to automatically classify this ticket and generate a response.
              </p>
            )}
          </div>

          {/* Transitions */}
          {allowedTransitions.length > 0 && (
            <div className={`glass glass--static ${styles.section}`}>
              <h3 className="section-title">Transition</h3>
              <div className={styles.transitionBtns}>
                {allowedTransitions.map((status) => (
                  <button
                    key={status}
                    className="btn btn--secondary"
                    onClick={() => handleTransition(status)}
                    disabled={transitioning}
                  >
                    <ChevronRight size={14} />
                    Move to {STATUS_CONFIG[status]?.label || status}
                  </button>
                ))}
              </div>
            </div>
          )}
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

            {/* Timeline */}
            {activeTab === 'timeline' && (
              <div className={styles.timeline}>
                {events.length > 0 ? events.map((event, i) => (
                  <div key={event.id} className={styles.timelineItem}>
                    <div className={styles.timelineDot} />
                    {i < events.length - 1 && <div className={styles.timelineLine} />}
                    <div className={styles.timelineContent}>
                      <div className={styles.timelineHead}>
                        <span className={styles.timelineType}>{formatEnum(event.eventType)}</span>
                        <span className={styles.timelineTime}>{timeAgo(event.createdAt)}</span>
                      </div>
                      {event.actorName && (
                        <span className={styles.timelineActor}>by {event.actorName}</span>
                      )}
                      {event.details && Object.keys(event.details).length > 0 && (
                        <div className={styles.timelineDetails}>
                          {Object.entries(event.details).map(([key, val]) => (
                            <span key={key}>{key}: {String(val)}</span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                )) : (
                  <p className={styles.emptyTimeline}>No events recorded yet.</p>
                )}
              </div>
            )}

            {/* Notes */}
            {activeTab === 'notes' && (
              <div className={styles.notesSection}>
                <div className={styles.notesList}>
                  {notes.map((note) => (
                    <div key={note.id} className={styles.noteItem}>
                      <div className={styles.noteHeader}>
                        <div className={styles.noteAvatar}>
                          {note.authorName.charAt(0).toUpperCase()}
                        </div>
                        <span className={styles.noteName}>{note.authorName}</span>
                        <span className={styles.noteTime}>{timeAgo(note.createdAt)}</span>
                      </div>
                      <p className={styles.noteBody}>{note.content}</p>
                    </div>
                  ))}
                  {notes.length === 0 && (
                    <p className={styles.emptyTimeline}>No internal notes yet.</p>
                  )}
                </div>
                <div className={styles.noteInput}>
                  <textarea
                    className="input textarea"
                    placeholder="Add an internal note..."
                    value={noteContent}
                    onChange={(e) => setNoteContent(e.target.value)}
                    rows={3}
                  />
                  <button
                    className="btn btn--primary btn--sm"
                    onClick={handleAddNote}
                    disabled={addingNote || !noteContent.trim()}
                  >
                    <Send size={14} />
                    {addingNote ? 'Sending...' : 'Add Note'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Confidence Gauge ───────────────────────────────────────────────
function ConfidenceGauge({ score }: { score: number }) {
  const percentage = Math.round(score * 100);
  const color = percentage >= 70 ? 'var(--color-success)' :
                percentage >= 40 ? 'var(--color-warning)' : 'var(--color-danger)';

  return (
    <div className={styles.gauge}>
      <svg viewBox="0 0 36 36" className={styles.gaugeSvg}>
        <path
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke="rgba(255,255,255,0.06)"
          strokeWidth="3"
        />
        <motion.path
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke={color}
          strokeWidth="3"
          strokeLinecap="round"
          initial={{ strokeDasharray: '0 100' }}
          animate={{ strokeDasharray: `${percentage} ${100 - percentage}` }}
          transition={{ duration: 1.2, ease: [0.22, 1, 0.36, 1] }}
        />
      </svg>
      <span className={styles.gaugeText} style={{ color }}>{percentage}%</span>
    </div>
  );
}
