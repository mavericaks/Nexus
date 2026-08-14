/**
 * JWT helper functions for client-side token management.
 */

interface JwtPayload {
  sub: string;
  userId: string;
  tenantId: string;
  role: string;
  exp: number;
  iat: number;
  iss: string;
}

const TOKEN_KEY = 'nexus_jwt';

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function decodeToken(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1];
    const decoded = atob(payload);
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeToken(token);
  if (!payload) return true;
  // Add 30 second buffer
  return Date.now() >= (payload.exp * 1000) - 30000;
}

export function getUserFromToken(): {
  email: string;
  userId: string;
  tenantId: string;
  role: string;
} | null {
  const token = getToken();
  if (!token) return null;

  if (isTokenExpired(token)) {
    removeToken();
    return null;
  }

  const payload = decodeToken(token);
  if (!payload) return null;

  return {
    email: payload.sub,
    userId: payload.userId,
    tenantId: payload.tenantId,
    role: payload.role,
  };
}
