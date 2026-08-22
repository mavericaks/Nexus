'use client';

import { useAuth } from '@/context/AuthContext';
import { api, Ticket } from '@/lib/api';
import { timeAgo, animateValue } from '@/lib/utils';
import { STATUS_CONFIG, PRIORITY_CONFIG } from '@/lib/constants';
import { useEffect, useState, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import {
  Ticket as TicketIcon,
  Brain, CheckCircle2, Clock, ArrowUpRight, Activity
} from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import styles from './page.module.css';

interface DashboardMetrics {
  totalTickets: number;
  openTickets: number;
  resolvedTickets: number;
  escalatedTickets: number;
  autoResolvedRate: number;
  avgSatisfaction: number;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [recentTickets, setRecentTickets] = useState<Ticket[]>([]);
  const [statusData, setStatusData] = useState<{ name: string; value: number; color: string }[]>([]);
  const [loading, setLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    if (!user) return;
    try {
      // Fetch tickets for stats
      const [page1] = await Promise.all([
        api.getTickets(user.tenantId, { size: 100 }),
      ]);

      const tickets = page1.content;

      // Compute metrics
      const open = tickets.filter((t) => !['RESOLVED', 'CLOSED', 'AUTO_RESOLVED'].includes(t.status)).length;
      const resolved = tickets.filter((t) => ['RESOLVED', 'CLOSED', 'AUTO_RESOLVED'].includes(t.status)).length;
      const autoResolved = tickets.filter((t) => t.status === 'AUTO_RESOLVED').length;
      const escalated = tickets.filter((t) => t.status === 'ESCALATED').length;

      setMetrics({
        totalTickets: tickets.length,
        openTickets: open,
        resolvedTickets: resolved,
        escalatedTickets: escalated,
        autoResolvedRate: tickets.length > 0 ? Math.round((autoResolved / tickets.length) * 100) : 0,
        avgSatisfaction: 0,
      });

      // Status distribution for pie chart
      const statusCounts: Record<string, number> = {};
      tickets.forEach((t) => {
        statusCounts[t.status] = (statusCounts[t.status] || 0) + 1;
      });

      setStatusData(
        Object.entries(statusCounts).map(([status, count]) => ({
          name: STATUS_CONFIG[status]?.label || status,
          value: count,
          color: STATUS_CONFIG[status]?.color || '#64748b',
        }))
      );

      // Recent tickets
      setRecentTickets(tickets.slice(0, 5));
    } catch (err) {
      console.error('Failed to load dashboard:', err instanceof Error ? err : JSON.stringify(err));
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (!user) return;
    loadDashboard();
  }, [user, loadDashboard]);

  if (loading) {
    return (
      <div className={styles.page}>
        <h1 className="page-title">Dashboard</h1>
        <div className={styles.metricsGrid}>
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className={`glass ${styles.metricCard}`}>
              <div className={`skeleton ${styles.skeletonTitle}`} />
              <div className={`skeleton ${styles.skeletonNumber}`} />
            </div>
          ))}
        </div>
      </div>
    );
  }

  const metricCards = [
    {
      icon: <TicketIcon size={20} />,
      label: 'Total Tickets',
      value: metrics?.totalTickets ?? 0,
      color: 'var(--accent-primary)',
      bgColor: 'rgba(99, 102, 241, 0.1)',
    },
    {
      icon: <Clock size={20} />,
      label: 'Open',
      value: metrics?.openTickets ?? 0,
      color: 'var(--color-warning)',
      bgColor: 'var(--color-warning-soft)',
    },
    {
      icon: <CheckCircle2 size={20} />,
      label: 'Resolved',
      value: metrics?.resolvedTickets ?? 0,
      color: 'var(--color-success)',
      bgColor: 'var(--color-success-soft)',
    },
    {
      icon: <Brain size={20} />,
      label: 'AI Auto-Resolve Rate',
      value: metrics?.autoResolvedRate ?? 0,
      suffix: '%',
      color: 'var(--accent-secondary)',
      bgColor: 'rgba(139, 92, 246, 0.1)',
    },
  ];

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className="page-title">Dashboard</h1>
        <p className={styles.greeting}>Welcome back, {user?.email.split('@')[0]}</p>
      </div>

      {/* ─── Metric Cards ──────────────────────────────────────── */}
      <div className={styles.metricsGrid}>
        {metricCards.map((card, i) => (
          <motion.div
            key={card.label}
            className={`glass ${styles.metricCard}`}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1, duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className={styles.metricTop}>
              <div
                className={styles.metricIcon}
                style={{ background: card.bgColor, color: card.color }}
              >
                {card.icon}
              </div>
              <span className={styles.metricLabel}>{card.label}</span>
            </div>
            <AnimatedNumber value={card.value} suffix={card.suffix} />
          </motion.div>
        ))}
      </div>

      {/* ─── Charts + Recent Tickets ────────────────────────────── */}
      <div className={styles.chartsRow}>
        {/* Status Distribution */}
        <motion.div
          className={`glass glass--static ${styles.chartCard}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.4 }}
        >
          <h3 className="section-title">Status Distribution</h3>
          {statusData.length > 0 ? (
            <div className={styles.pieWrap}>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={statusData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={85}
                    paddingAngle={3}
                    dataKey="value"
                    stroke="none"
                  >
                    {statusData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: 'rgba(15,15,25,0.95)',
                      border: '1px solid rgba(255,255,255,0.1)',
                      borderRadius: '8px',
                      color: '#f1f5f9',
                      fontSize: '13px',
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className={styles.legend}>
                {statusData.map((item) => (
                  <div key={item.name} className={styles.legendItem}>
                    <span className={styles.legendDot} style={{ background: item.color }} />
                    <span className={styles.legendLabel}>{item.name}</span>
                    <span className={styles.legendValue}>{item.value}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className={styles.emptyChart}>No tickets yet</p>
          )}
        </motion.div>

        {/* Recent Tickets */}
        <motion.div
          className={`glass glass--static ${styles.recentCard}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.4 }}
        >
          <div className={styles.recentHeader}>
            <h3 className="section-title">Recent Tickets</h3>
            <Activity size={16} className={styles.recentIcon} />
          </div>
          {recentTickets.length > 0 ? (
            <div className={styles.recentList}>
              {recentTickets.map((ticket) => (
                <a
                  key={ticket.id}
                  href={`/tickets/${ticket.id}`}
                  className={styles.recentItem}
                >
                  <div className={styles.recentItemTop}>
                    <span className={styles.recentSubject}>{ticket.subject}</span>
                    <ArrowUpRight size={14} className={styles.recentArrow} />
                  </div>
                  <div className={styles.recentMeta}>
                    <span className={`badge ${STATUS_CONFIG[ticket.status]?.badgeClass || ''}`}>
                      {STATUS_CONFIG[ticket.status]?.label || ticket.status}
                    </span>
                    {ticket.priority && (
                      <span className={`badge ${PRIORITY_CONFIG[ticket.priority]?.badgeClass || ''}`}>
                        {ticket.priority}
                      </span>
                    )}
                    <span className={styles.recentTime}>{timeAgo(ticket.createdAt)}</span>
                  </div>
                </a>
              ))}
            </div>
          ) : (
            <p className={styles.emptyChart}>No tickets yet. Create your first one!</p>
          )}
        </motion.div>
      </div>
    </div>
  );
}

// ─── Animated Number Component ──────────────────────────────────────
function AnimatedNumber({ value, suffix }: { value: number; suffix?: string }) {
  const [display, setDisplay] = useState(0);
  const ref = useRef(false);

  useEffect(() => {
    if (ref.current) return;
    ref.current = true;
    animateValue(0, value, 1500, setDisplay);
  }, [value]);

  return (
    <span className="metric-number">
      {display}
      {suffix}
    </span>
  );
}
