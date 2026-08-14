'use client';

import { useAuth } from '@/context/AuthContext';
import { api, KnowledgeArticle } from '@/lib/api';
import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Plus, BookOpen, X } from 'lucide-react';
import styles from './page.module.css';

export default function KnowledgePage() {
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<KnowledgeArticle[]>([]);
  const [searching, setSearching] = useState(false);
  const [searched, setSearched] = useState(false);
  const [showAdd, setShowAdd] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [adding, setAdding] = useState(false);

  const canManage = user?.role === 'OWNER' || user?.role === 'ADMIN';

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !query.trim()) return;
    setSearching(true);
    setSearched(true);
    try {
      const data = await api.searchKnowledge(user.tenantId, query.trim());
      setResults(data);
    } catch (err) {
      console.error('Search failed:', err);
    } finally {
      setSearching(false);
    }
  }

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !title.trim() || !content.trim()) return;
    setAdding(true);
    try {
      await api.addKnowledgeArticle(user.tenantId, {
        title: title.trim(),
        content: content.trim(),
      });
      setTitle('');
      setContent('');
      setShowAdd(false);
    } catch (err: any) {
      alert(err.message || 'Failed to add article');
    } finally {
      setAdding(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className="page-title">Knowledge Base</h1>
          <p className={styles.subtitle}>Search articles using AI-powered semantic retrieval</p>
        </div>
        {canManage && (
          <button className="btn btn--primary" onClick={() => setShowAdd(!showAdd)}>
            {showAdd ? <X size={16} /> : <Plus size={16} />}
            {showAdd ? 'Cancel' : 'Add Article'}
          </button>
        )}
      </div>

      {/* Add Article Form */}
      <AnimatePresence>
        {showAdd && (
          <motion.form
            className={`glass glass--static ${styles.addForm}`}
            onSubmit={handleAdd}
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
          >
            <input
              className="input"
              placeholder="Article title..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
            <textarea
              className="input textarea"
              placeholder="Article content (this will be embedded for semantic search)..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              required
              rows={6}
            />
            <button
              className="btn btn--primary"
              type="submit"
              disabled={adding || !title.trim() || !content.trim()}
            >
              {adding ? 'Publishing...' : 'Publish Article'}
            </button>
          </motion.form>
        )}
      </AnimatePresence>

      {/* Search */}
      <form className={styles.searchBar} onSubmit={handleSearch}>
        <div className={styles.searchInput}>
          <Search size={18} />
          <input
            type="text"
            placeholder="Search the knowledge base..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className={styles.searchField}
          />
        </div>
        <button className="btn btn--primary" type="submit" disabled={searching || !query.trim()}>
          {searching ? 'Searching...' : 'Search'}
        </button>
      </form>

      {/* Results */}
      {searched && (
        <div className={styles.results}>
          {results.length > 0 ? results.map((article, i) => (
            <motion.div
              key={article.id}
              className={`glass ${styles.articleCard}`}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
            >
              <div className={styles.articleIcon}>
                <BookOpen size={18} />
              </div>
              <h3 className={styles.articleTitle}>{article.title}</h3>
              <p className={styles.articleContent}>{article.content}</p>
            </motion.div>
          )) : (
            <div className={styles.emptyState}>
              <BookOpen size={40} strokeWidth={1} />
              <p>No articles found for &ldquo;{query}&rdquo;</p>
            </div>
          )}
        </div>
      )}

      {!searched && (
        <div className={styles.placeholder}>
          <BookOpen size={48} strokeWidth={1} />
          <p>Enter a search query to find relevant knowledge base articles.</p>
          <p className={styles.hint}>The search uses AI-powered semantic matching — try natural language queries!</p>
        </div>
      )}
    </div>
  );
}
