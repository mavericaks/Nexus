'use client';

import { useAuth } from '@/context/AuthContext';
import { api, ApiError } from '@/lib/api';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { ArrowLeft, Send } from 'lucide-react';
import styles from './page.module.css';

export default function NewTicketPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !subject.trim() || !description.trim()) return;

    setSubmitting(true);
    setError('');

    try {
      const ticket = await api.createTicket(user.tenantId, {
        subject: subject.trim(),
        description: description.trim(),
      });
      router.push(`/tickets/${ticket.id}`);
    } catch (err: unknown) {
      setError((err as ApiError)?.message || 'Failed to create ticket');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.page}>
      <button className={styles.back} onClick={() => router.push('/tickets')}>
        <ArrowLeft size={16} />
        Back to Tickets
      </button>

      <h1 className="page-title">Create New Ticket</h1>

      <motion.form
        className={`glass glass--static ${styles.form}`}
        onSubmit={handleSubmit}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
      >
        <div className={styles.field}>
          <label className="input-label" htmlFor="subject">Subject</label>
          <input
            id="subject"
            className="input"
            type="text"
            placeholder="Brief summary of the issue..."
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            required
            maxLength={255}
            autoFocus
          />
        </div>

        <div className={styles.field}>
          <label className="input-label" htmlFor="description">Description</label>
          <textarea
            id="description"
            className="input textarea"
            placeholder="Describe the issue in detail. Our AI will analyze this to suggest a category, priority, and response..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            maxLength={10000}
            rows={8}
          />
        </div>

        {error && <p className="input-error">{error}</p>}

        <div className={styles.formActions}>
          <button
            type="button"
            className="btn btn--secondary"
            onClick={() => router.push('/tickets')}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn--primary"
            disabled={submitting || !subject.trim() || !description.trim()}
          >
            <Send size={16} />
            {submitting ? 'Creating...' : 'Create Ticket'}
          </button>
        </div>
      </motion.form>
    </div>
  );
}
