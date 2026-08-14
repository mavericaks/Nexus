'use client';

import { useAuth } from '@/context/AuthContext';
import { motion } from 'framer-motion';
import { Users, Shield, Mail } from 'lucide-react';
import styles from './page.module.css';

export default function TeamPage() {
  const { user } = useAuth();

  return (
    <div className={styles.page}>
      <h1 className="page-title">Team</h1>
      <p className={styles.subtitle}>Manage your organization&apos;s team members</p>

      <motion.div
        className={`glass glass--static ${styles.profileCard}`}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div className={styles.avatar}>
          {user?.email.charAt(0).toUpperCase()}
        </div>
        <div className={styles.profileInfo}>
          <h2 className={styles.profileName}>{user?.email.split('@')[0]}</h2>
          <div className={styles.profileMeta}>
            <span className={styles.metaItem}>
              <Mail size={14} /> {user?.email}
            </span>
            <span className={styles.metaItem}>
              <Shield size={14} />
              <span className={`badge badge--role`}>{user?.role}</span>
            </span>
          </div>
        </div>
      </motion.div>

      <div className={`glass glass--static ${styles.section}`}>
        <div className={styles.sectionHeader}>
          <h3 className="section-title">Team Members</h3>
          <Users size={16} className={styles.sectionIcon} />
        </div>
        <div className={styles.memberList}>
          <div className={styles.memberItem}>
            <div className={styles.memberAvatar}>
              {user?.email.charAt(0).toUpperCase()}
            </div>
            <div className={styles.memberInfo}>
              <span className={styles.memberName}>{user?.email}</span>
              <span className={styles.memberRole}>{user?.role}</span>
            </div>
            <span className={styles.memberStatus}>Active</span>
          </div>
        </div>
        <p className={styles.hint}>
          Team member management is available through the API. Additional team members 
          can be registered via the OAuth2 login flow.
        </p>
      </div>
    </div>
  );
}
