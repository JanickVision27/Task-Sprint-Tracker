import { createContext, useContext, useState, useEffect } from 'react';

// * Creates the global container for authentication data.
const AuthContext = createContext(null);

// * Wrapper component that holds the auth logic and shares it with the whole app.
export function AuthProvider({ children }) {
  
  // * Initializes state by checking localStorage first. 
  // * This keeps the user logged in even if they close or refresh the browser.
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });

  // * Derives true/false login status based on if the token exists.
  const isAuthenticated = !!token;

  // * Saves the token and user data to localStorage (permanent) and state (for UI updates).
  function login(newToken, userData) {
    localStorage.setItem('token', newToken);
    localStorage.setItem('user', JSON.stringify(userData));
    setToken(newToken);
    setUser(userData);
  }

  // * Clears the token and user data from localStorage and state.
  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }

  return (
    // * Shares the token, user, login status, and functions with any component inside this provider.
    <AuthContext.Provider value={{ token, user, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// * Custom hook that gives any component easy access to the auth data without repeating boilerplate code.
export function useAuth() {
  return useContext(AuthContext);
}