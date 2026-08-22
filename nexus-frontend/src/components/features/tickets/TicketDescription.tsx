import React from 'react';
import { Clock } from 'lucide-react';
import { Ticket } from '@/lib/api';
import { formatDate, timeAgo } from '@/lib/utils';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface TicketDescriptionProps {
  ticket: Ticket;
}

export function TicketDescription({ ticket }: TicketDescriptionProps) {
  return (
    <div className={`glass glass--static ${styles.section}`}>
      <h3 className="section-title">Description</h3>
      <p className={styles.description}>{ticket.description}</p>
      <div className={styles.meta}>
        <span><Clock size={14} /> Created {formatDate(ticket.createdAt)}</span>
        <span>Updated {timeAgo(ticket.updatedAt)}</span>
      </div>
    </div>
  );
}
