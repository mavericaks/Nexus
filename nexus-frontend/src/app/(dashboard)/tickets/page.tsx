'use client';

import { useAuth } from '@/context/AuthContext';
import { api, Ticket } from '@/lib/api';
import { timeAgo } from '@/lib/utils';
import { STATUS_CONFIG, PRIORITY_CONFIG, STATUS_OPTIONS, PRIORITY_OPTIONS, CATEGORY_OPTIONS } from '@/lib/constants';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { Plus, Filter, ChevronLeft, ChevronRight, Search } from 'lucide-react';
import styles from './page.module.css';

export default function TicketsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    status: '',
    priority: '',
    category: '',
  });
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    if (!user) return;
    loadTickets();
  }, [user, page, filters]);

  async function loadTickets() {
    if (!user) return;
    setLoading(true);
    try {
      const data = await api.getTickets(user.tenantId, {
        page,
        size: 15,
        status: filters.status || undefined,
        priority: filters.priority || undefined,
        category: filters.category || undefined,
        sort: 'createdAt,desc',
      });
      setTickets(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      console.error('Failed to load tickets:', err);
    } finally {
      setLoading(false);
    }
  }

  const clearFilters = () => {
    setFilters({ status: '', priority: '', category: '' });
    setPage(0);
  };

  const hasFilters = filters.status || filters.priority || filters.category;

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className="page-title">Tickets</h1>
          <p className={styles.subtitle}>{totalElements} total tickets</p>
        </div>
        <div className={styles.actions}>
          <button
            className={`btn btn--secondary ${showFilters ? styles.filterActive : ''}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <Filter size={16} />
            Filters
            {hasFilters && <span className={styles.filterDot} />}
          </button>
          <button
            className="btn btn--primary"
            onClick={() => router.push('/tickets/new')}
          >
            <Plus size={16} />
            New Ticket
          </button>
        </div>
      </div>

      {/* ─── Filters ────────────────────────────────────────────── */}
      {showFilters && (
        <motion.div
          className={`glass glass--static ${styles.filterBar}`}
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          exit={{ opacity: 0, height: 0 }}
        >
          <select
            className="input"
            value={filters.status}
            onChange={(e) => { setFilters(f => ({ ...f, status: e.target.value })); setPage(0); }}
          >
            <option value="">All Statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{STATUS_CONFIG[s]?.label || s}</option>
            ))}
          </select>
          <select
            className="input"
            value={filters.priority}
            onChange={(e) => { setFilters(f => ({ ...f, priority: e.target.value })); setPage(0); }}
          >
            <option value="">All Priorities</option>
            {PRIORITY_OPTIONS.map((p) => (
              <option key={p} value={p}>{PRIORITY_CONFIG[p]?.label || p}</option>
            ))}
          </select>
          <select
            className="input"
            value={filters.category}
            onChange={(e) => { setFilters(f => ({ ...f, category: e.target.value })); setPage(0); }}
          >
            <option value="">All Categories</option>
            {CATEGORY_OPTIONS.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          {hasFilters && (
            <button className="btn btn--ghost" onClick={clearFilters}>Clear</button>
          )}
        </motion.div>
      )}

      {/* ─── Ticket Table ───────────────────────────────────────── */}
      <div className={`glass glass--static ${styles.tableWrap}`}>
        {loading ? (
          <div className={styles.loadingRows}>
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className={styles.skeletonRow}>
                <div className={`skeleton ${styles.skLine}`} style={{ width: '60%' }} />
                <div className={`skeleton ${styles.skBadge}`} />
                <div className={`skeleton ${styles.skBadge}`} />
                <div className={`skeleton ${styles.skDate}`} />
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
                  onClick={() => router.push(`/tickets/${ticket.id}`)}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: i * 0.03 }}
                >
                  <td className={styles.subjectCell}>
                    <span className={styles.subject}>{ticket.subject}</span>
                  </td>
                  <td>
                    <span className={`badge ${STATUS_CONFIG[ticket.status]?.badgeClass || ''}`}>
                      {STATUS_CONFIG[ticket.status]?.label || ticket.status}
                    </span>
                  </td>
                  <td>
                    {ticket.priority ? (
                      <span className={`badge ${PRIORITY_CONFIG[ticket.priority]?.badgeClass || ''}`}>
                        {ticket.priority}
                      </span>
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
          <div className={styles.emptyState}>
            <Search size={40} strokeWidth={1} />
            <p>No tickets found</p>
            {hasFilters && (
              <button className="btn btn--secondary" onClick={clearFilters}>
                Clear filters
              </button>
            )}
          </div>
        )}
      </div>

      {/* ─── Pagination ─────────────────────────────────────────── */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button
            className="btn btn--ghost btn--sm"
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
          >
            <ChevronLeft size={16} /> Previous
          </button>
          <span className={styles.pageInfo}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            className="btn btn--ghost btn--sm"
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
          >
            Next <ChevronRight size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
