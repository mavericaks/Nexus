'use client';

import { useAuth } from '@/context/AuthContext';
import { motion } from 'framer-motion';
import { Globe, Key, Bell, Palette } from 'lucide-react';
import styles from './page.module.css';

export default function SettingsPage() {
  const { user } = useAuth();

  return (
    <div className={styles.page}>
      <h1 className="page-title">Settings</h1>
      <p className={styles.subtitle}>Manage your workspace and preferences</p>

      <div className={styles.grid}>
        <motion.div
          className={`glass glass--static ${styles.card}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0 }}
        >
          <div className={styles.cardIcon}><Globe size={20} /></div>
          <h3 className={styles.cardTitle}>Workspace</h3>
          <div className={styles.settingRow}>
            <span className={styles.settingLabel}>Tenant ID</span>
            <code className={styles.settingValue}>{user?.tenantId}</code>
          </div>
          <div className={styles.settingRow}>
            <span className={styles.settingLabel}>Your Role</span>
            <span className={`badge badge--role`}>{user?.role}</span>
          </div>
        </motion.div>

        <motion.div
          className={`glass glass--static ${styles.card}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <div className={styles.cardIcon}><Key size={20} /></div>
          <h3 className={styles.cardTitle}>API Access</h3>
          <p className={styles.cardDesc}>
            Use your JWT token to access the Nexus API programmatically.
          </p>
          <div className={styles.settingRow}>
            <span className={styles.settingLabel}>API Base URL</span>
            <code className={styles.settingValue}>https://nexus-tep5.onrender.com</code>
          </div>
        </motion.div>

        <motion.div
          className={`glass glass--static ${styles.card}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <div className={styles.cardIcon}><Bell size={20} /></div>
          <h3 className={styles.cardTitle}>Notifications <span className={styles.comingSoon}>Coming soon</span></h3>
          <div className={styles.settingRow}>
            <span className={styles.settingLabel}>Email Digest</span>
            <select className="input" style={{ maxWidth: 160 }} disabled>
              <option value="OFF">Off</option>
              <option value="DAILY">Daily</option>
              <option value="WEEKLY">Weekly</option>
            </select>
          </div>
          <div className={styles.settingRow}>
            <span className={styles.settingLabel}>In-App Sounds</span>
            <label className={styles.toggle}>
              <input type="checkbox" defaultChecked disabled />
              <span className={styles.toggleSlider} />
            </label>
          </div>
        </motion.div>

        <motion.div
          className={`glass glass--static ${styles.card}`}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <div className={styles.cardIcon}><Palette size={20} /></div>
          <h3 className={styles.cardTitle}>Appearance <span className={styles.comingSoon}>Coming soon</span></h3>
          <div className={styles.settingRow}>
            <span className={styles.settingLabel}>Theme</span>
            <select className="input" style={{ maxWidth: 160 }} disabled>
              <option value="DARK">Dark (Obsidian)</option>
              <option value="SYSTEM">System</option>
            </select>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
