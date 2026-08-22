import React from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { Search, Bell } from 'lucide-react';
import styles from '@/app/(dashboard)/layout.module.css';

export interface HeaderProps {
  onSearchClick: () => void;
  unreadCount: number;
}

export function Header({ onSearchClick, unreadCount }: HeaderProps) {
  const { user } = useAuth();
  const router = useRouter();

  if (!user) return null;

  return (
    <header className={styles.header}>
      <button
        className={styles.searchTrigger}
        onClick={onSearchClick}
        aria-label="Open command palette"
      >
        <Search size={16} />
        <span>Search or command...</span>
        <kbd className={styles.kbd}>⌘K</kbd>
      </button>

      <div className={styles.headerRight}>
        <button 
          className={styles.headerBtn} 
          onClick={() => router.push('/notifications')} 
          aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
        >
          <Bell size={18} />
          {unreadCount > 0 && (
            <span className={styles.notifBadge}>{unreadCount > 9 ? '9+' : unreadCount}</span>
          )}
        </button>
        <div className={styles.headerDivider} />
        <div className={styles.headerUser}>
          <span className={styles.headerEmail}>{user.email}</span>
          <span className={`badge badge--role ${styles.headerRole}`}>
            {user.role}
          </span>
        </div>
      </div>
    </header>
  );
}
