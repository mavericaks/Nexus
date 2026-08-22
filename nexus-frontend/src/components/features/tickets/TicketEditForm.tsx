import React, { useState } from 'react';
import { Ticket } from '@/lib/api';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface TicketEditFormProps {
  ticket: Ticket;
  onSave: (subject: string, description: string) => void;
  onCancel: () => void;
  isSaving: boolean;
}

export function TicketEditForm({ ticket, onSave, onCancel, isSaving }: TicketEditFormProps) {
  const [subject, setSubject] = useState(ticket.subject);
  const [description, setDescription] = useState(ticket.description);

  const handleSave = () => {
    if (!subject.trim()) return;
    onSave(subject.trim(), description.trim());
  };

  return (
    <div className={`glass glass--static ${styles.section}`} style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
      <h3 className="section-title">Edit Ticket</h3>
      
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <label style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Subject</label>
        <input
          type="text"
          className="input"
          style={{ width: '100%' }}
          value={subject}
          onChange={(e) => setSubject(e.target.value)}
          disabled={isSaving}
        />
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
        <label style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Description</label>
        <textarea
          className="input textarea"
          style={{ width: '100%', minHeight: '120px' }}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          disabled={isSaving}
        />
      </div>

      <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end', marginTop: 'var(--space-2)' }}>
        <Button variant="ghost" onClick={onCancel} disabled={isSaving}>
          Cancel
        </Button>
        <Button variant="primary" onClick={handleSave} disabled={isSaving || !subject.trim()}>
          {isSaving ? 'Saving...' : 'Save Changes'}
        </Button>
      </div>
    </div>
  );
}
