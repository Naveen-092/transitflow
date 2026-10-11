
import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '../api/axios';
import { clearSession, getToken, getUser, saveSession, saveUser } from '../api/session';
import { AuthContext } from './authContextObject';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (getToken() ? getUser() : null));
  const [loading, setLoading] = useState(() => Boolean(getToken()));

  // On page load, confirm the stored token is still valid and refresh the user.
  useEffect(() => {
    if (!getToken()) return;
    api.get('/auth/me')
      .then((res) => {
        setUser(res.data.data);
        saveUser(res.data.data);
      })
      .catch((err) => {
        if (err.response?.status === 401) {
          clearSession();
          setUser(null);
        } // other errors (server down): keep the cached user
      })
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email, password) => {
    const res = await api.post('/auth/login', { email, password });
    saveSession(res.data.data);
    setUser(res.data.data.user);
    return res.data.data.user;
  }, []);

  const register = useCallback(async (form) => {
    await api.post('/auth/register', form);
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}