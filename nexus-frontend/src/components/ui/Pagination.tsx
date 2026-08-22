import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export interface PaginationProps {
  page: number; // 0-indexed
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
  pageInfoClassName?: string;
}

export function Pagination({ page, totalPages, onPageChange, className = '', pageInfoClassName = '' }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className={`flex items-center justify-center gap-4 ${className}`.trim()}>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => onPageChange(Math.max(0, page - 1))}
        disabled={page === 0}
      >
        <ChevronLeft size={16} /> Previous
      </Button>
      <span className={pageInfoClassName}>
        Page {page + 1} of {totalPages}
      </span>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
      >
        Next <ChevronRight size={16} />
      </Button>
    </div>
  );
}
