const TOKEN = 'tf_token';
const USER = 'tf_user';
const EXPIRES = 'tf_expires';

export function saveSession({ token, expiresInMs, user }) {
  localStorage.setItem(TOKEN, token);
  localStorage.setItem(EXPIRES, String(Date.now() + expiresInMs));
  localStorage.setItem(USER, JSON.stringify(user));
}

export function saveUser(user) {
  localStorage.setItem(USER, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN);
  localStorage.removeItem(EXPIRES);
  localStorage.removeItem(USER);
}

/** Returns the token, or null if there is none or it has expired. */
export function getToken() {
  const token = localStorage.getItem(TOKEN);
  const expires = Number(localStorage.getItem(EXPIRES));
  if (token && expires && Date.now() > expires) {
    clearSession();
    return null;
  }
  return token;
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER));
  } catch {
    return null;
  }
}