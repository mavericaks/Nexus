import React from 'react';
import { ChevronRight } from 'lucide-react';
import { STATUS_CONFIG } from '@/lib/constants';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface TransitionPanelProps {
  allowedTransitions: string[];
  isTransitioning: boolean;
  onTransition: (targetStatus: string) => void;
}

export function TransitionPanel({ allowedTransitions, isTransitioning, onTransition }: TransitionPanelProps) {
  if (allowedTransitions.length === 0) return null;

  return (
    <div className={`glass glass--static ${styles.section}`}>
      <h3 className="section-title">Transition</h3>
      <div className={styles.transitionBtns}>
        {allowedTransitions.map((status) => (
          <Button
            key={status}
            variant="secondary"
            onClick={() => onTransition(status)}
            disabled={isTransitioning}
          >
            <ChevronRight size={14} />
            Move to {STATUS_CONFIG[status]?.label || status}
          </Button>
        ))}
      </div>
    </div>
  );
}
