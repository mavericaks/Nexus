import React from 'react';
import { TicketEvent } from '@/lib/api';
import { formatEnum, timeAgo } from '@/lib/utils';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface TimelineProps {
  events: TicketEvent[];
}

export function Timeline({ events }: TimelineProps) {
  if (events.length === 0) {
    return (
      <div className={styles.timeline}>
        <p className={styles.emptyTimeline}>No events recorded yet.</p>
      </div>
    );
  }

  return (
    <div className={styles.timeline}>
      {events.map((event, i) => (
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
      ))}
    </div>
  );
}
