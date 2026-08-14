'use client';

import { useAuth } from '@/context/AuthContext';
import { useRouter, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  LayoutDashboard, Ticket, BookOpen, Users, Settings,
  LogOut, Bell, Search, ChevronLeft, Command
} from 'lucide-react';
import { api } from '@/lib/api';
import styles from './layout.module.css';

const NAV_ITEMS = [
  { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', roles: ['OWNER', 'ADMIN', 'AGENT'] },
  { path: '/tickets', icon: Ticket, label: 'Tickets', roles: ['OWNER', 'ADMIN', 'AGENT'] },
  { path: '/knowledge', icon: BookOpen, label: 'Knowledge', roles: ['OWNER', 'ADMIN', 'AGENT'] },
  { path: '/team', icon: Users, label: 'Team', roles: ['OWNER', 'ADMIN'] },
  { path: '/settings', icon: Settings, label: 'Settings', roles: ['OWNER', 'ADMIN'] },
];

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading, isAuthenticated, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/');
    }
  }, [isLoading, isAuthenticated, router]);

  // Fetch unread notifications
  useEffect(() => {
    if (!isAuthenticated) return;
    const fetchCount = async () => {
      try {
        const data = await api.getUnreadCount();
        setUnreadCount(data.unreadCount);
      } catch {
        // Silently fail
      }
    };
    fetchCount();
    const interval = setInterval(fetchCount, 30000);
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  // Keyboard shortcut: Ctrl+K for command palette
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setCommandPaletteOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  if (isLoading || !isAuthenticated || !user) {
    return (
      <div className={styles.loadingScreen}>
        <div className={styles.loadingRing} />
      </div>
    );
  }

  const filteredNav = NAV_ITEMS.filter((item) => item.roles.includes(user.role));
  const userInitial = user.email.charAt(0).toUpperCase();

  return (
    <div className={styles.appShell}>
      {/* ─── Sidebar ────────────────────────────────────────────── */}
      <aside
        className={`${styles.sidebar} ${sidebarExpanded ? styles.sidebarExpanded : ''}`}
        onMouseEnter={() => setSidebarExpanded(true)}
        onMouseLeave={() => setSidebarExpanded(false)}
      >
        <div className={styles.sidebarTop}>
          <div className={styles.sidebarLogo}>
            <span className={styles.logoN}>N</span>
            <AnimatePresence>
              {sidebarExpanded && (
                <motion.span
                  className={styles.logoText}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -10 }}
                  transition={{ duration: 0.15 }}
                >
                  Nexus
                </motion.span>
              )}
            </AnimatePresence>
          </div>

          <nav className={styles.sidebarNav}>
            {filteredNav.map((item) => {
              const isActive = pathname.startsWith(item.path);
              return (
                <button
                  key={item.path}
                  className={`${styles.navItem} ${isActive ? styles.navItemActive : ''}`}
                  onClick={() => router.push(item.path)}
                  title={item.label}
                >
                  <item.icon size={20} />
                  <AnimatePresence>
                    {sidebarExpanded && (
                      <motion.span
                        className={styles.navLabel}
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -10 }}
                        transition={{ duration: 0.15 }}
                      >
                        {item.label}
                      </motion.span>
                    )}
                  </AnimatePresence>
                  {item.path === '/tickets' && unreadCount > 0 && !sidebarExpanded && (
                    <span className={styles.navBadge} />
                  )}
                </button>
              );
            })}
          </nav>
        </div>

        <div className={styles.sidebarBottom}>
          <button
            className={styles.navItem}
            onClick={logout}
            title="Sign out"
          >
            <LogOut size={20} />
            <AnimatePresence>
              {sidebarExpanded && (
                <motion.span
                  className={styles.navLabel}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -10 }}
                  transition={{ duration: 0.15 }}
                >
                  Sign Out
                </motion.span>
              )}
            </AnimatePresence>
          </button>

          <div className={styles.userAvatar} title={user.email}>
            {userInitial}
          </div>
        </div>
      </aside>

      {/* ─── Main Content ───────────────────────────────────────── */}
      <div className={styles.mainArea}>
        {/* ─── Header ─────────────────────────────────────────── */}
        <header className={styles.header}>
          <button
            className={styles.searchTrigger}
            onClick={() => setCommandPaletteOpen(true)}
          >
            <Search size={16} />
            <span>Search or command...</span>
            <kbd className={styles.kbd}>⌘K</kbd>
          </button>

          <div className={styles.headerRight}>
            <button className={styles.headerBtn} onClick={() => router.push('/notifications')}>
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

        {/* ─── Page Content ───────────────────────────────────── */}
        <main className={styles.content}>
          <motion.div
            key={pathname}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            {children}
          </motion.div>
        </main>
      </div>

      {/* ─── Command Palette ────────────────────────────────────── */}
      <AnimatePresence>
        {commandPaletteOpen && (
          <CommandPalette
            onClose={() => setCommandPaletteOpen(false)}
            onNavigate={(path) => {
              router.push(path);
              setCommandPaletteOpen(false);
            }}
          />
        )}
      </AnimatePresence>

      {/* ─── Mobile Bottom Nav ──────────────────────────────────── */}
      <nav className={styles.mobileNav}>
        {filteredNav.slice(0, 5).map((item) => {
          const isActive = pathname.startsWith(item.path);
          return (
            <button
              key={item.path}
              className={`${styles.mobileNavItem} ${isActive ? styles.mobileNavActive : ''}`}
              onClick={() => router.push(item.path)}
            >
              <item.icon size={20} />
              <span className={styles.mobileNavLabel}>{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
}

// ─── Command Palette Component ──────────────────────────────────────
function CommandPalette({
  onClose,
  onNavigate,
}: {
  onClose: () => void;
  onNavigate: (path: string) => void;
}) {
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
