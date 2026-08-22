'use client';

import { useAuth } from '@/context/AuthContext';
import { useRouter, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { api } from '@/lib/api';
import styles from './layout.module.css';

import { Sidebar, NAV_ITEMS } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import { CommandPalette } from '@/components/layout/CommandPalette';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading, isAuthenticated } = useAuth();
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

  return (
    <div className={styles.appShell}>
      {/* ─── Sidebar ────────────────────────────────────────────── */}
      <Sidebar 
        expanded={sidebarExpanded} 
        onExpandChange={setSidebarExpanded} 
        unreadCount={unreadCount} 
      />

      {/* ─── Main Content ───────────────────────────────────────── */}
      <div className={styles.mainArea}>
        {/* ─── Header ─────────────────────────────────────────── */}
        <Header 
          onSearchClick={() => setCommandPaletteOpen(true)} 
          unreadCount={unreadCount} 
        />

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
