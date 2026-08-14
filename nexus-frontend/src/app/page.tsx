'use client';

import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Zap, Shield, Brain, ChevronDown } from 'lucide-react';
import styles from './page.module.css';

export default function LandingPage() {
  const { isAuthenticated, isLoading, login } = useAuth();
  const router = useRouter();
  const [showFeatures, setShowFeatures] = useState(false);

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isLoading, isAuthenticated, router]);

  useEffect(() => {
    const timer = setTimeout(() => setShowFeatures(true), 1200);
    return () => clearTimeout(timer);
  }, []);

  if (isLoading) {
    return (
      <div className={styles.loaderWrap}>
        <div className={styles.loader}>
          <div className={styles.loaderRing} />
          <span className={styles.loaderText}>Initializing Nexus</span>
        </div>
      </div>
    );
  }

  if (isAuthenticated) return null;

  const headlineWords = ['Support,', 'Supercharged', 'by', 'AI.'];
  const features = [
    {
      icon: <Brain size={28} />,
      title: 'AI-Powered Triage',
      description: 'Automatic classification, priority assignment, and intelligent response drafting.',
    },
    {
      icon: <Zap size={28} />,
      title: 'Instant Resolution',
      description: 'Knowledge base RAG retrieval resolves common issues without human intervention.',
    },
    {
      icon: <Shield size={28} />,
      title: 'Enterprise Security',
      description: 'Multi-tenant isolation with Row-Level Security. Your data never crosses boundaries.',
    },
  ];

  return (
    <main className={styles.landing}>
      {/* ─── Hero ──────────────────────────────────────────────── */}
      <section className={styles.hero}>
        <motion.div
          className={styles.logoMark}
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        >
          <div className={styles.logoGlow} />
          <span className={styles.logoLetter}>N</span>
        </motion.div>

        <h1 className={styles.headline}>
          {headlineWords.map((word, i) => (
            <motion.span
              key={word}
              className={styles.headlineWord}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{
                delay: 0.3 + i * 0.15,
                duration: 0.5,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              {word}{' '}
            </motion.span>
          ))}
        </h1>

        <motion.p
          className={styles.subheadline}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.0, duration: 0.8 }}
        >
          Intelligent ticket triage. Instant knowledge retrieval. Enterprise-grade security.
        </motion.p>

        <motion.button
          className={styles.loginBtn}
          onClick={login}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.3, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          whileHover={{ scale: 1.03 }}
          whileTap={{ scale: 0.97 }}
        >
          <svg className={styles.googleIcon} viewBox="0 0 24 24" width="20" height="20">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
          </svg>
          Sign in with Google
        </motion.button>

        <motion.div
          className={styles.scrollHint}
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.5 }}
          transition={{ delay: 2, duration: 0.5 }}
        >
          <ChevronDown size={20} />
        </motion.div>
      </section>

      {/* ─── Features ──────────────────────────────────────────── */}
      <AnimatePresence>
        {showFeatures && (
          <section className={styles.features}>
            {features.map((feature, i) => (
              <motion.div
                key={feature.title}
                className={`glass ${styles.featureCard}`}
                initial={{ opacity: 0, y: 40 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  delay: i * 0.15,
                  duration: 0.6,
                  ease: [0.22, 1, 0.36, 1],
                }}
              >
                <div className={styles.featureIcon}>{feature.icon}</div>
                <h3 className={styles.featureTitle}>{feature.title}</h3>
                <p className={styles.featureDesc}>{feature.description}</p>
              </motion.div>
            ))}
          </section>
        )}
      </AnimatePresence>

      <footer className={styles.footer}>
        <p>Nexus &copy; {new Date().getFullYear()} &middot; Built with care.</p>
      </footer>
    </main>
  );
}
