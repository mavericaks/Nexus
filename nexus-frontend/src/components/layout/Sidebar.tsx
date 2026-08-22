import React from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import { LayoutDashboard, Ticket, BookOpen, Users, Settings, LogOut } from 'lucide-react';
import styles from '@/app/(dashboard)/layout.module.css';

export const NAV_ITEMS = [
  { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', roles: ['OWNER', 'ADMIN', 'AGENT'] },
  { path: '/tickets', icon: Ticket, label: 'Tickets', roles: ['OWNER', 'ADMIN', 'AGENT'] },
  { path: '/knowledge', icon: BookOpen, label: 'Knowledge', roles: ['OWNER', 'ADMIN', 'AGENT'] },
  { path: '/team', icon: Users, label: 'Team', roles: ['OWNER', 'ADMIN'] },
  { path: '/settings', icon: Settings, label: 'Settings', roles: ['OWNER', 'ADMIN'] },
];

export interface SidebarProps {
  expanded: boolean;
  onExpandChange: (expanded: boolean) => void;
  unreadCount: number;
}

export function Sidebar({ expanded, onExpandChange, unreadCount }: SidebarProps) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  if (!user) return null;

  const filteredNav = NAV_ITEMS.filter((item) => item.roles.includes(user.role));
  const userInitial = user.email.charAt(0).toUpperCase();

  return (
    <aside
      className={`${styles.sidebar} ${expanded ? styles.sidebarExpanded : ''}`}
      onMouseEnter={() => onExpandChange(true)}
      onMouseLeave={() => onExpandChange(false)}
    >
      <div className={styles.sidebarTop}>
        <div className={styles.sidebarLogo}>
          <span className={styles.logoN}>N</span>
          <AnimatePresence>
            {expanded && (
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
                  {expanded && (
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
                {item.path === '/tickets' && unreadCount > 0 && !expanded && (
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
            {expanded && (
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
  );
}
