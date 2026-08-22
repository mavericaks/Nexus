'use client';

import { useAuth } from '@/context/AuthContext';
import { api, Ticket } from '@/lib/api';
import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, Filter } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Pagination } from '@/components/ui/Pagination';
import { TicketFilters, FiltersState } from '@/components/features/tickets/TicketFilters';
import { TicketTable } from '@/components/features/tickets/TicketTable';
import styles from './page.module.css';

export default function TicketsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<FiltersState>({
    status: '',
    priority: '',
    category: '',
  });
  const [showFilters, setShowFilters] = useState(false);

  const loadTickets = useCallback(async () => {
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
  }, [user, page, filters]);

  useEffect(() => {
    if (!user) return;
    loadTickets();
  }, [user, page, filters, loadTickets]);

  const clearFilters = () => {
    setFilters({ status: '', priority: '', category: '' });
    setPage(0);
  };

  const hasFilters = Boolean(filters.status || filters.priority || filters.category);

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className="page-title">Tickets</h1>
          <p className={styles.subtitle}>{totalElements} total tickets</p>
        </div>
        <div className={styles.actions}>
          <Button
            variant="secondary"
            className={showFilters ? styles.filterActive : ''}
            onClick={() => setShowFilters(!showFilters)}
          >
            <Filter size={16} />
            Filters
            {hasFilters && <span className={styles.filterDot} />}
          </Button>
          <Button
            variant="primary"
            onClick={() => router.push('/tickets/new')}
          >
            <Plus size={16} />
            New Ticket
          </Button>
        </div>
      </div>

      {/* ─── Filters ────────────────────────────────────────────── */}
      {showFilters && (
        <TicketFilters 
          filters={filters}
          onFilterChange={(newFilters) => {
            setFilters(newFilters);
            setPage(0);
          }}
          onClear={clearFilters}
          hasFilters={hasFilters}
        />
      )}

      {/* ─── Ticket Table ───────────────────────────────────────── */}
      <TicketTable 
        tickets={tickets}
        loading={loading}
        hasFilters={hasFilters}
        onClearFilters={clearFilters}
        onTicketClick={(id) => router.push(`/tickets/${id}`)}
      />

      {/* ─── Pagination ─────────────────────────────────────────── */}
      <Pagination 
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        className={styles.pagination}
        pageInfoClassName={styles.pageInfo}
      />
    </div>
  );
}
