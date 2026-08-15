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
  logout: () => void;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);

  // Check for JWT on mount and when URL changes (OAuth callback)
  useEffect(() => {
    // Check URL for token or error from OAuth redirect
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');
    const errorFromUrl = urlParams.get('error');

    if (tokenFromUrl) {
      setToken(tokenFromUrl);
      // Clean URL
      window.history.replaceState({}, '', window.location.pathname);
    }

    if (errorFromUrl) {
      setAuthError(decodeURIComponent(errorFromUrl));
      window.history.replaceState({}, '', window.location.pathname);
    }

    // Try to get user from stored token
    const storedUser = getUserFromToken();
    if (storedUser) {
      setUser(storedUser);
    }

    setIsLoading(false);
  }, []);

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

  const login = useCallback(() => {
    // Redirect to Spring Boot OAuth2 login endpoint
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
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
