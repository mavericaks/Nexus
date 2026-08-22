import React from 'react';
import { motion } from 'framer-motion';
import { STATUS_OPTIONS, PRIORITY_OPTIONS, CATEGORY_OPTIONS, STATUS_CONFIG, PRIORITY_CONFIG } from '@/lib/constants';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/page.module.css';

export interface FiltersState {
  status: string;
  priority: string;
  category: string;
}

export interface TicketFiltersProps {
  filters: FiltersState;
  onFilterChange: (filters: FiltersState) => void;
  onClear: () => void;
  hasFilters: boolean;
}

export function TicketFilters({ filters, onFilterChange, onClear, hasFilters }: TicketFiltersProps) {
  return (
    <motion.div
      className={`glass glass--static ${styles.filterBar}`}
      initial={{ opacity: 0, height: 0 }}
      animate={{ opacity: 1, height: 'auto' }}
      exit={{ opacity: 0, height: 0 }}
    >
      <select
        className="input"
        value={filters.status}
        onChange={(e) => onFilterChange({ ...filters, status: e.target.value })}
      >
        <option value="">All Statuses</option>
        {STATUS_OPTIONS.map((s) => (
          <option key={s} value={s}>{STATUS_CONFIG[s]?.label || s}</option>
        ))}
      </select>
      <select
        className="input"
        value={filters.priority}
        onChange={(e) => onFilterChange({ ...filters, priority: e.target.value })}
      >
        <option value="">All Priorities</option>
        {PRIORITY_OPTIONS.map((p) => (
          <option key={p} value={p}>{PRIORITY_CONFIG[p]?.label || p}</option>
        ))}
      </select>
      <select
        className="input"
        value={filters.category}
        onChange={(e) => onFilterChange({ ...filters, category: e.target.value })}
      >
        <option value="">All Categories</option>
        {CATEGORY_OPTIONS.map((c) => (
          <option key={c} value={c}>{c}</option>
        ))}
      </select>
      {hasFilters && (
        <Button variant="ghost" onClick={onClear}>Clear</Button>
      )}
    </motion.div>
  );
}
