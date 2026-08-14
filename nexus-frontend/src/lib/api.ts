import { API_BASE_URL } from './constants';

// ─── Types ──────────────────────────────────────────────────────────

export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface Ticket {
  id: string;
  tenantId: string;
  subject: string;
  description: string;
  status: string;
  priority: string | null;
  category: string | null;
  aiConfidenceScore: number | null;
  aiSuggestedResponse: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface TicketEvent {
  id: string;
  ticketId: string;
  eventType: string;
  actorId: string | null;
  actorName: string | null;
  details: Record<string, unknown>;
  createdAt: string;
}

export interface TicketNote {
  id: string;
  ticketId: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface Notification {
  id: string;
  type: string;
  title: string;
  message: string | null;
  referenceId: string | null;
  read: boolean;
  createdAt: string;
}

export interface TriageResult {
  ticketId: string;
  suggestedCategory: string;
  suggestedPriority: string;
  confidenceScore: number;
  suggestedResponse: string;
}

export interface KnowledgeArticle {
  id: string;
  title: string;
  content: string;
  tenantId: string;
  createdAt: string;
}

export interface ResponseTemplate {
  id: string;
  title: string;
  content: string;
  category: string | null;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SatisfactionRating {
  id: string;
  ticketId: string;
  score: number;
  feedback: string | null;
  createdAt: string;
}

// ─── Core API Client ────────────────────────────────────────────────

class ApiClient {
  private getToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('nexus_jwt');
  }

  private async request<T>(
    path: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      let error: ApiError;
      try {
        error = await response.json();
      } catch {
        error = {
          status: response.status,
          error: response.statusText,
          message: 'An unexpected error occurred',
          timestamp: new Date().toISOString(),
        };
      }
      throw error;
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  // ─── Tickets ────────────────────────────────────────────────────

  async getTickets(
    tenantId: string,
    params: {
      page?: number;
      size?: number;
      status?: string;
      priority?: string;
      category?: string;
      sort?: string;
    } = {}
  ): Promise<PageResponse<Ticket>> {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    if (params.status) query.set('status', params.status);
    if (params.priority) query.set('priority', params.priority);
    if (params.category) query.set('category', params.category);
    if (params.sort) query.set('sort', params.sort);

    const qs = query.toString();
    return this.request<PageResponse<Ticket>>(
      `/api/v1/tenants/${tenantId}/tickets${qs ? '?' + qs : ''}`
    );
  }

  async getTicket(tenantId: string, ticketId: string): Promise<Ticket> {
    return this.request<Ticket>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}`
    );
  }

  async createTicket(
    tenantId: string,
    data: { subject: string; description: string }
  ): Promise<Ticket> {
    return this.request<Ticket>(
      `/api/v1/tenants/${tenantId}/tickets`,
      { method: 'POST', body: JSON.stringify(data) }
    );
  }

  async updateTicket(
    tenantId: string,
    ticketId: string,
    data: { subject?: string; description?: string; version: number }
  ): Promise<Ticket> {
    return this.request<Ticket>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}`,
      { method: 'PUT', body: JSON.stringify(data) }
    );
  }

  async transitionTicket(
    tenantId: string,
    ticketId: string,
    data: { targetStatus: string; version: number }
  ): Promise<Ticket> {
    return this.request<Ticket>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}/transition`,
      { method: 'POST', body: JSON.stringify(data) }
    );
  }

  async deleteTicket(tenantId: string, ticketId: string): Promise<void> {
    return this.request<void>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}`,
      { method: 'DELETE' }
    );
  }

  // ─── Triage ─────────────────────────────────────────────────────

  async triageTicket(tenantId: string, ticketId: string): Promise<TriageResult> {
    return this.request<TriageResult>(
      `/api/v1/tenants/${tenantId}/triage/${ticketId}`,
      { method: 'POST' }
    );
  }

  // ─── Events ─────────────────────────────────────────────────────

  async getTicketEvents(tenantId: string, ticketId: string): Promise<TicketEvent[]> {
    return this.request<TicketEvent[]>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}/events`
    );
  }

  // ─── Notes ──────────────────────────────────────────────────────

  async getTicketNotes(tenantId: string, ticketId: string): Promise<TicketNote[]> {
    return this.request<TicketNote[]>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}/notes`
    );
  }

  async addTicketNote(
    tenantId: string,
    ticketId: string,
    content: string
  ): Promise<TicketNote> {
    return this.request<TicketNote>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}/notes`,
      { method: 'POST', body: JSON.stringify({ content }) }
    );
  }

  // ─── Satisfaction ───────────────────────────────────────────────

  async rateTicket(
    tenantId: string,
    ticketId: string,
    score: number,
    feedback?: string
  ): Promise<SatisfactionRating> {
    return this.request<SatisfactionRating>(
      `/api/v1/tenants/${tenantId}/tickets/${ticketId}/satisfaction`,
      { method: 'POST', body: JSON.stringify({ score, feedback }) }
    );
  }

  async getTicketRating(tenantId: string, ticketId: string): Promise<SatisfactionRating | null> {
    try {
      return await this.request<SatisfactionRating>(
        `/api/v1/tenants/${tenantId}/tickets/${ticketId}/satisfaction`
      );
    } catch {
      return null;
    }
  }

  // ─── Notifications ──────────────────────────────────────────────

  async getNotifications(): Promise<Notification[]> {
    return this.request<Notification[]>('/api/v1/notifications');
  }

  async getUnreadCount(): Promise<{ unreadCount: number }> {
    return this.request<{ unreadCount: number }>('/api/v1/notifications/count');
  }

  async markNotificationRead(notificationId: string): Promise<void> {
    return this.request<void>(
      `/api/v1/notifications/${notificationId}/read`,
      { method: 'PATCH' }
    );
  }

  async markAllNotificationsRead(): Promise<void> {
    return this.request<void>('/api/v1/notifications/read-all', { method: 'POST' });
  }

  // ─── Knowledge Base ─────────────────────────────────────────────

  async searchKnowledge(tenantId: string, query: string): Promise<KnowledgeArticle[]> {
    return this.request<KnowledgeArticle[]>(
      `/api/v1/tenants/${tenantId}/triage/knowledge/search?query=${encodeURIComponent(query)}`
    );
  }

  async addKnowledgeArticle(
    tenantId: string,
    data: { title: string; content: string }
  ): Promise<KnowledgeArticle> {
    return this.request<KnowledgeArticle>(
      `/api/v1/tenants/${tenantId}/triage/knowledge`,
      { method: 'POST', body: JSON.stringify(data) }
    );
  }

  // ─── Templates ──────────────────────────────────────────────────

  async getTemplates(tenantId: string, category?: string): Promise<ResponseTemplate[]> {
    const qs = category ? `?category=${category}` : '';
    return this.request<ResponseTemplate[]>(
      `/api/v1/tenants/${tenantId}/templates${qs}`
    );
  }

  async createTemplate(
    tenantId: string,
    data: { title: string; content: string; category?: string }
  ): Promise<ResponseTemplate> {
    return this.request<ResponseTemplate>(
      `/api/v1/tenants/${tenantId}/templates`,
      { method: 'POST', body: JSON.stringify(data) }
    );
  }

  async deleteTemplate(tenantId: string, templateId: string): Promise<void> {
    return this.request<void>(
      `/api/v1/tenants/${tenantId}/templates/${templateId}`,
      { method: 'DELETE' }
    );
  }

  // ─── Analytics ──────────────────────────────────────────────────

  async getMetrics(tenantId: string): Promise<Record<string, unknown>> {
    return this.request<Record<string, unknown>>(
      `/api/v1/tenants/${tenantId}/analytics/metrics`
    );
  }
}

export const api = new ApiClient();
