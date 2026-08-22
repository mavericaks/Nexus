import React from 'react';
import { motion } from 'framer-motion';
import { Search } from 'lucide-react';
import { Ticket } from '@/lib/api';
import { timeAgo } from '@/lib/utils';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/page.module.css';

export interface TicketTableProps {
  tickets: Ticket[];
  loading: boolean;
  hasFilters: boolean;
  onClearFilters: () => void;
  onTicketClick: (ticketId: string) => void;
}

export function TicketTable({ tickets, loading, hasFilters, onClearFilters, onTicketClick }: TicketTableProps) {
  return (
    <div className={`glass glass--static ${styles.tableWrap}`}>
      {loading ? (
        <div className={styles.loadingRows}>
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className={styles.skeletonRow}>
              <Skeleton className={styles.skLine} style={{ width: '60%' }} />
              <Skeleton className={styles.skBadge} />
              <Skeleton className={styles.skBadge} />
              <Skeleton className={styles.skDate} />
            </div>
          ))}
        </div>
      ) : tickets.length > 0 ? (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Subject</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Category</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {tickets.map((ticket, i) => (
              <motion.tr
                key={ticket.id}
                className={styles.row}
                onClick={() => onTicketClick(ticket.id)}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.03 }}
              >
                <td className={styles.subjectCell}>
                  <span className={styles.subject}>{ticket.subject}</span>
                </td>
                <td>
                  <Badge status={ticket.status} />
                </td>
                <td>
                  {ticket.priority ? (
                    <Badge priority={ticket.priority} />
                  ) : (
                    <span className={styles.empty}>—</span>
                  )}
                </td>
                <td>
                  {ticket.category ? (
                    <span className={styles.category}>{ticket.category}</span>
                  ) : (
                    <span className={styles.empty}>—</span>
                  )}
                </td>
                <td className={styles.timeCell}>{timeAgo(ticket.createdAt)}</td>
              </motion.tr>
            ))}
          </tbody>
        </table>
      ) : (
        <EmptyState 
          icon={<Search size={40} strokeWidth={1} />}
          title="No tickets found"
          className={styles.emptyState}
        >
          {hasFilters && (
            <Button variant="secondary" onClick={onClearFilters}>
              Clear filters
            </Button>
          )}
        </EmptyState>
      )}
    </div>
  );
}
