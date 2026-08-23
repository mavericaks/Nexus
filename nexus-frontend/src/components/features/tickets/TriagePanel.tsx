import React, { useState, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Brain, Sparkles, Loader2, Check, Search, Cpu, BarChart3, AlertCircle, BookOpen } from 'lucide-react';
import { Ticket, TriageResult, ApiError } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { formatEnum } from '@/lib/utils';
import { PRIORITY_CONFIG } from '@/lib/constants';
import { API_BASE_URL } from '@/lib/constants';
import { getToken } from '@/lib/auth';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

interface KBArticle {
  title: string;
  similarity: string;
}

interface StageInfo {
  id: string;
  label: string;
  icon: React.ReactNode;
  status: 'pending' | 'active' | 'done' | 'error';
  message?: string;
  data?: KBArticle[] | Record<string, unknown> | null;
}

const PIPELINE_STAGES: { id: string; label: string; icon: React.ReactNode }[] = [
  { id: 'KB_SEARCH', label: 'Knowledge Base Search', icon: <Search size={14} /> },
  { id: 'KB_RESULTS', label: 'Articles Retrieved', icon: <BookOpen size={14} /> },
  { id: 'LLM_CALL', label: 'AI Analysis', icon: <Brain size={14} /> },
  { id: 'LLM_RESPONSE', label: 'Response Received', icon: <Cpu size={14} /> },
  { id: 'CONFIDENCE', label: 'Confidence Scoring', icon: <BarChart3 size={14} /> },
];

export interface TriagePanelProps {
  ticket: Ticket;
  initialResult?: TriageResult | null;
  onTriageComplete: () => void;
  onError: (error: string) => void;
}

export function TriagePanel({ ticket, initialResult, onTriageComplete, onError }: TriagePanelProps) {
  const { user } = useAuth();
  const [triaging, setTriaging] = useState(false);
  const [stages, setStages] = useState<StageInfo[]>([]);
  const [localTriageResult, setLocalTriageResult] = useState<TriageResult | null>(null);
  const [streamComplete, setStreamComplete] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  const displayResult = localTriageResult || initialResult;
  const canTriage = ['NEW'].includes(ticket.status);

  const updateStage = useCallback((stageId: string, status: StageInfo['status'], message?: string, data?: StageInfo['data']) => {
    setStages(prev => {
      const existing = prev.find(s => s.id === stageId);
      if (existing) {
        return prev.map(s => s.id === stageId ? { ...s, status, message, data } : s);
      }
      const template = PIPELINE_STAGES.find(p => p.id === stageId);
      if (!template) return prev;
      return [...prev, { ...template, status, message, data }];
    });
  }, []);

  function handleTriage() {
    if (!user || !ticket) return;

    setTriaging(true);
    setStreamComplete(false);
    setLocalTriageResult(null);

    // Initialize all stages as pending
    setStages(PIPELINE_STAGES.map(s => ({ ...s, status: 'pending' as const })));

    const token = getToken();
    const url = `${API_BASE_URL}/api/v1/tenants/${user.tenantId}/tickets/${ticket.id}/triage/stream`;

    // SSE doesn't support custom headers natively, so we use EventSource with URL token
    // For JWT auth with SSE, we use a fetch-based approach
    const controller = new AbortController();

    fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream',
      },
      signal: controller.signal,
    }).then(async response => {
      if (!response.ok) {
        throw new Error(`Triage stream failed: ${response.status}`);
      }

      const reader = response.body?.getReader();
      if (!reader) throw new Error('No response body');

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        let currentEventName = '';
        let currentData = '';

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEventName = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            currentData = line.slice(5).trim();
          } else if (line === '' && currentData) {
            // End of event — process it
            try {
              const event = JSON.parse(currentData);
              const stage = event.stage as string;
              const message = event.message as string;
              const data = event.data as StageInfo['data'];

              if (stage === 'ERROR') {
                // Mark current active stage as error
                setStages(prev => prev.map(s =>
                  s.status === 'active' ? { ...s, status: 'error' as const } : s
                ));
                onError(message);
              } else if (stage === 'COMPLETE') {
                // Mark all stages as done
                setStages(prev => prev.map(s =>
                  s.status !== 'error' ? { ...s, status: 'done' as const } : s
                ));
                if (data && !Array.isArray(data)) {
                  setLocalTriageResult({
                    category: data.category as string,
                    priority: data.priority as string,
                    confidenceScore: data.confidenceScore as number,
                    suggestedReply: data.suggestedReply as string,
                    reasoning: data.reasoning as string,
                    autoResolvable: data.autoResolvable as boolean,
                  });
                }
                setStreamComplete(true);
                onTriageComplete();
              } else {
                // Mark previous active stages as done, set current as active
                setStages(prev => prev.map(s => {
                  if (s.id === stage) return { ...s, status: 'active' as const, message, data };
                  if (s.status === 'active') return { ...s, status: 'done' as const };
                  return s;
                }));

                // If stage not in template list, add it
                updateStage(stage, 'active', message, data);
              }
            } catch {
              // Skip unparseable events
            }
            currentEventName = '';
            currentData = '';
          }
        }
      }
    }).catch(err => {
      if (err.name !== 'AbortError') {
        console.error('SSE stream error:', err);
        onError(err.message || 'Triage stream failed');
      }
    }).finally(() => {
      setTimeout(() => {
        setTriaging(false);
      }, 800);
    });
  }

  const showPipeline = triaging || (streamComplete && stages.length > 0 && !displayResult);

  return (
    <div className={`glass glass--static ${styles.section} ${styles.triageSection} ${triaging ? styles.triageActive : ''}`}>
      <div className={styles.triageHeader}>
        <h3 className="section-title">
          <Sparkles size={16} /> AI Triage
        </h3>
        {canTriage && (
          <Button
            variant="primary"
            size="sm"
            onClick={handleTriage}
            disabled={triaging}
          >
            {triaging ? <Loader2 size={14} className={styles.spinning} /> : <Brain size={14} />}
            {triaging ? 'Analyzing...' : 'Run Triage'}
          </Button>
        )}
      </div>

      <AnimatePresence mode="wait">
        {showPipeline && (
          <motion.div
            className={styles.triagePipeline}
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.3 }}
          >
            {stages.map((stage, i) => (
              <motion.div
                key={stage.id}
                className={`${styles.pipelineStage} ${styles[`stage--${stage.status}`]}`}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05, duration: 0.3 }}
              >
                <div className={styles.stageIndicator}>
                  {stage.status === 'done' && <Check size={12} />}
                  {stage.status === 'active' && <Loader2 size={12} className={styles.spinning} />}
                  {stage.status === 'error' && <AlertCircle size={12} />}
                  {stage.status === 'pending' && <span className={styles.stageDot} />}
                </div>
                <div className={styles.stageContent}>
                  <div className={styles.stageLabel}>
                    {stage.icon}
                    <span>{stage.label}</span>
                  </div>
                  {stage.message && stage.status !== 'pending' && (
                    <motion.span
                      className={styles.stageMessage}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                    >
                      {stage.message}
                    </motion.span>
                  )}
                  {stage.id === 'KB_RESULTS' && stage.data && Array.isArray(stage.data) && (
                    <motion.div
                      className={styles.stageArticles}
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                    >
                      {(stage.data as KBArticle[]).map((article: KBArticle, idx: number) => (
                        <span key={idx} className={styles.articleTag}>
                          {article.title} ({article.similarity})
                        </span>
                      ))}
                    </motion.div>
                  )}
                </div>
              </motion.div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {displayResult && !triaging && (
        <motion.div
          className={styles.triageResults}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className={styles.triageGrid}>
            <div className={styles.triageCard}>
              <span className="label">Category</span>
              <span className={styles.triageValue}>{formatEnum(displayResult.category)}</span>
            </div>
            <div className={styles.triageCard}>
              <span className="label">Priority</span>
              <span className={styles.triageValue} style={{
                color: PRIORITY_CONFIG[displayResult.priority]?.color
              }}>
                {displayResult.priority}
              </span>
            </div>
            <div className={styles.triageCard}>
              <span className="label">Confidence</span>
              <ConfidenceGauge score={displayResult.confidenceScore} />
            </div>
          </div>

          {displayResult.suggestedReply && (
            <div className={styles.aiResponse}>
              <span className="label">AI Suggested Response</span>
              <div className={`glass--inset ${styles.responseBody}`}>
                <p className="ai-text">{displayResult.suggestedReply}</p>
              </div>
            </div>
          )}
        </motion.div>
      )}

      {!displayResult && !triaging && (
        <p className={styles.triagePlaceholder}>
          Run AI Triage to automatically classify this ticket and generate a response.
        </p>
      )}
    </div>
  );
}

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
