import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Brain, Sparkles, Loader2 } from 'lucide-react';
import { api, Ticket, TriageResult, ApiError } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { formatEnum } from '@/lib/utils';
import { PRIORITY_CONFIG } from '@/lib/constants';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface TriagePanelProps {
  ticket: Ticket;
  initialResult?: TriageResult | null;
  onTriageComplete: () => void;
  onError: (error: string) => void;
}

export function TriagePanel({ ticket, initialResult, onTriageComplete, onError }: TriagePanelProps) {
  const { user } = useAuth();
  const [triaging, setTriaging] = useState(false);
  const [triagePhase, setTriagePhase] = useState(0);
  const [localTriageResult, setLocalTriageResult] = useState<TriageResult | null>(null);

  const displayResult = localTriageResult || initialResult;

  const canTriage = ['NEW'].includes(ticket.status);

  async function handleTriage() {
    if (!user || !ticket) return;
    setTriaging(true);
    setTriagePhase(1);

    const phase2 = setTimeout(() => setTriagePhase(2), 800);
    const phase3 = setTimeout(() => setTriagePhase(3), 1800);

    try {
      const result = await api.triageTicket(user.tenantId, ticket.id);
      clearTimeout(phase2);
      clearTimeout(phase3);
      setTriagePhase(4);
      setLocalTriageResult(result);
      
      onTriageComplete();
    } catch (err: unknown) {
      onError((err as ApiError)?.message || 'Triage failed');
    } finally {
      setTimeout(() => {
        setTriaging(false);
        setTriagePhase(0);
      }, 500);
    }
  }

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
