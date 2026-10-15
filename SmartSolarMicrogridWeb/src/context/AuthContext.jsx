import { createContext, useState } from "react";
import * as authService from "../services/authService";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(authService.getStoredUser);

  async function signIn(credentials) {
    const authenticatedUser = await authService.login(credentials);
    setCurrentUser(authenticatedUser);
    return authenticatedUser;
  }

  function signOut() {
    authService.logout();
    setCurrentUser(null);
  }

  return (
    <AuthContext.Provider value={{ currentUser, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}