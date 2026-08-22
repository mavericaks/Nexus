import React, { useState } from 'react';
import { Send } from 'lucide-react';
import { TicketNote } from '@/lib/api';
import { timeAgo } from '@/lib/utils';
import { Button } from '@/components/ui/Button';
import styles from '@/app/(dashboard)/tickets/[id]/page.module.css';

export interface NotesSectionProps {
  notes: TicketNote[];
  isAdding: boolean;
  onAddNote: (content: string) => void;
}

export function NotesSection({ notes, isAdding, onAddNote }: NotesSectionProps) {
  const [noteContent, setNoteContent] = useState('');

  const handleAdd = () => {
    if (!noteContent.trim()) return;
    onAddNote(noteContent.trim());
    setNoteContent('');
  };

  return (
    <div className={styles.notesSection}>
      <div className={styles.notesList}>
        {notes.map((note) => (
          <div key={note.id} className={styles.noteItem}>
            <div className={styles.noteHeader}>
              <div className={styles.noteAvatar}>
                {note.authorName.charAt(0).toUpperCase()}
              </div>
              <span className={styles.noteName}>{note.authorName}</span>
              <span className={styles.noteTime}>{timeAgo(note.createdAt)}</span>
            </div>
            <p className={styles.noteBody}>{note.content}</p>
          </div>
        ))}
        {notes.length === 0 && (
          <p className={styles.emptyTimeline}>No internal notes yet.</p>
        )}
      </div>
      <div className={styles.noteInput}>
        <textarea
          className="input textarea"
          placeholder="Add an internal note..."
          value={noteContent}
          onChange={(e) => setNoteContent(e.target.value)}
          rows={3}
          disabled={isAdding}
        />
        <Button
          variant="primary"
          size="sm"
          onClick={handleAdd}
          disabled={isAdding || !noteContent.trim()}
        >
          <Send size={14} />
          {isAdding ? 'Sending...' : 'Add Note'}
        </Button>
      </div>
    </div>
  );
}
