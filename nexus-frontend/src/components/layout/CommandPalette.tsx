import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { LayoutDashboard, Ticket, BookOpen, Users, Settings, Command } from 'lucide-react';
import styles from '@/app/(dashboard)/layout.module.css';

export interface CommandPaletteProps {
  onClose: () => void;
  onNavigate: (path: string) => void;
}

export function CommandPalette({ onClose, onNavigate }: CommandPaletteProps) {
  const [query, setQuery] = useState('');

  const commands = [
    { label: 'Go to Dashboard', path: '/dashboard', icon: <LayoutDashboard size={16} /> },
    { label: 'Go to Tickets', path: '/tickets', icon: <Ticket size={16} /> },
    { label: 'Create New Ticket', path: '/tickets/new', icon: <Ticket size={16} /> },
    { label: 'Go to Knowledge Base', path: '/knowledge', icon: <BookOpen size={16} /> },
    { label: 'Go to Team', path: '/team', icon: <Users size={16} /> },
    { label: 'Go to Settings', path: '/settings', icon: <Settings size={16} /> },
  ];

  const filtered = commands.filter((cmd) =>
    cmd.label.toLowerCase().includes(query.toLowerCase())
  );

  // Close on Escape
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  return (
    <>
      <motion.div
        className={styles.paletteBackdrop}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
      />
      <motion.div
        className={styles.palette}
        initial={{ opacity: 0, scale: 0.96, y: -10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: -10 }}
        transition={{ duration: 0.2 }}
      >
        <div className={styles.paletteInput}>
          <Command size={16} />
          <input
            type="text"
            placeholder="Search tickets, run commands..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoFocus
          />
        </div>
        <div className={styles.paletteResults}>
          {filtered.map((cmd) => (
            <button
              key={cmd.path}
              className={styles.paletteItem}
              onClick={() => onNavigate(cmd.path)}
            >
              {cmd.icon}
              <span>{cmd.label}</span>
            </button>
          ))}
          {filtered.length === 0 && (
            <p className={styles.paletteEmpty}>No results found</p>
          )}
        </div>
      </motion.div>
    </>
  );
}
