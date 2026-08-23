'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { getToken, setToken, removeToken, getUserFromToken, isTokenExpired } from '@/lib/auth';
import { API_BASE_URL } from '@/lib/constants';

interface User {
  email: string;
  userId: string;
  tenantId: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  authError: string | null;
  login: () => void;
  demoLogin: () => Promise<void>;
  isDemoLoading: boolean;
  logout: () => void;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  // Read OAuth redirect params during initialization (not in effect)
  const [user, setUser] = useState<User | null>(() => {
    if (typeof window === 'undefined') return null;
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');
    if (tokenFromUrl) {
      setToken(tokenFromUrl);
      window.history.replaceState({}, '', window.location.pathname);
    }
    return getUserFromToken();
  });

  const isLoading = false;

  const [authError, setAuthError] = useState<string | null>(() => {
    if (typeof window === 'undefined') return null;
    const urlParams = new URLSearchParams(window.location.search);
    const errorFromUrl = urlParams.get('error');
    if (errorFromUrl) {
      window.history.replaceState({}, '', window.location.pathname);
      return decodeURIComponent(errorFromUrl);
    }
    return null;
  });

  // Periodically check token expiry
  useEffect(() => {
    const interval = setInterval(() => {
      const token = getToken();
      if (token && isTokenExpired(token)) {
        removeToken();
        setUser(null);
      }
    }, 60000); // Check every minute

    return () => clearInterval(interval);
  }, []);

  const [isDemoLoading, setIsDemoLoading] = useState(false);

  const login = useCallback(() => {
    // Redirect to Spring Boot OAuth2 login endpoint
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
  }, []);

  const demoLogin = useCallback(async () => {
    setIsDemoLoading(true);
    setAuthError(null);
    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/auth/demo-login`, {
        method: 'POST',
      });
      if (!res.ok) {
        throw new Error('Demo login unavailable. The server may not have demo mode enabled.');
      }
      const data = await res.json();
      setToken(data.token);
      setUser(getUserFromToken());
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Demo login failed');
    } finally {
      setIsDemoLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    removeToken();
    setUser(null);
  }, []);

  const clearError = useCallback(() => {
    setAuthError(null);
  }, []);

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated: !!user,
    authError,
    login,
    demoLogin,
    isDemoLoading,
    logout,
    clearError,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
