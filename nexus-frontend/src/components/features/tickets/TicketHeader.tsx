import React from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Trash2, Edit2 } from 'lucide-react';
import { Ticket } from '@/lib/api';
import { formatEnum } from '@/lib/utils';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface TicketHeaderProps {
  ticket: Ticket;
  canDelete: boolean;
  onDelete: () => void;
  onEditClick: () => void;
}

export function TicketHeader({ ticket, canDelete, onDelete, onEditClick }: TicketHeaderProps) {
  const router = useRouter();

  return (
    <>
      <div className={styles.topBar}>
        <button className={styles.back} onClick={() => router.push('/tickets')} aria-label="Back to tickets">
          <ArrowLeft size={16} /> Back
        </button>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={onEditClick} aria-label="Edit ticket">
            <Edit2 size={14} /> Edit
          </Button>
          {canDelete && (
            <Button variant="danger" size="sm" onClick={onDelete} aria-label="Delete ticket">
              <Trash2 size={14} /> Delete
            </Button>
          )}
        </div>
      </div>

      <div className={styles.titleRow}>
        <h1 className="page-title">{ticket.subject}</h1>
        <div className={styles.badges}>
          <Badge status={ticket.status} />
          {ticket.priority && <Badge priority={ticket.priority} />}
          {ticket.category && (
            <span className={styles.categoryTag}>{formatEnum(ticket.category)}</span>
          )}
        </div>
      </div>
    </>
  );
}
