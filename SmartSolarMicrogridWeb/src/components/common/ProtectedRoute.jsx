import { Navigate, useLocation } from "react-router-dom";
import useAuth from "../../hooks/useAuth";

export default function ProtectedRoute({ roles, children }) {
  const { currentUser } = useAuth();
  const location = useLocation();

  if (!localStorage.getItem("token") || !currentUser) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles && !roles.includes(currentUser.role)) {
    return <Navigate to={currentUser.role === "Backoffice" ? "/backoffice" : "/operator"} replace />;
  }

  return children;
}