'use client';

import { useAuth } from '@/context/AuthContext';
import { api, Notification } from '@/lib/api';
import { timeAgo } from '@/lib/utils';
import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { Bell, CheckCheck, Ticket, Brain, AlertTriangle } from 'lucide-react';
import styles from './page.module.css';

const TYPE_ICONS: Record<string, React.ReactNode> = {
  TICKET_CREATED: <Ticket size={16} />,
  TICKET_RESOLVED: <Ticket size={16} />,
  TRIAGE_COMPLETE: <Brain size={16} />,
  ESCALATION: <AlertTriangle size={16} />,
};

export default function NotificationsPage() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  const loadNotifications = useCallback(async () => {
    try {
      const data = await api.getNotifications();
      setNotifications(data);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  async function markRead(id: string) {
    await api.markNotificationRead(id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  }

  async function markAllRead() {
    await api.markAllNotificationsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  }

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className="page-title">Notifications</h1>
        {notifications.some((n) => !n.read) && (
          <button className="btn btn--secondary btn--sm" onClick={markAllRead}>
            <CheckCheck size={14} /> Mark all read
          </button>
        )}
      </div>

      <div className={`glass glass--static ${styles.listWrap}`}>
        {loading ? (
          <div className={styles.loadingState}>
            {[1, 2, 3].map((i) => (
              <div key={i} className={styles.skeletonItem}>
                <div className={`skeleton`} style={{ width: 36, height: 36, borderRadius: '50%' }} />
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <div className={`skeleton`} style={{ width: '70%', height: 14 }} />
                  <div className={`skeleton`} style={{ width: '40%', height: 12 }} />
                </div>
              </div>
            ))}
          </div>
        ) : notifications.length > 0 ? (
          <div className={styles.list}>
            {notifications.map((n, i) => (
              <motion.div
                key={n.id}
                className={`${styles.item} ${!n.read ? styles.unread : ''}`}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.03 }}
                onClick={() => {
                  if (!n.read) markRead(n.id);
                  if (n.referenceId) router.push(`/tickets/${n.referenceId}`);
                }}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    if (!n.read) markRead(n.id);
                    if (n.referenceId) router.push(`/tickets/${n.referenceId}`);
                  }
                }}
                aria-label={`${n.read ? '' : 'Unread: '}${n.title}`}
              >
                <div className={styles.iconWrap}>
                  {TYPE_ICONS[n.type] || <Bell size={16} />}
                </div>
                <div className={styles.content}>
                  <span className={styles.title}>{n.title}</span>
                  {n.message && <p className={styles.message}>{n.message}</p>}
                  <span className={styles.time}>{timeAgo(n.createdAt)}</span>
                </div>
                {!n.read && <div className={styles.unreadDot} />}
              </motion.div>
            ))}
          </div>
        ) : (
          <div className={styles.emptyState}>
            <Bell size={40} strokeWidth={1} />
            <p>No notifications yet</p>
          </div>
        )}
      </div>
    </div>
  );
}
