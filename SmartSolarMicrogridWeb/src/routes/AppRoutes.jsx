import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "../components/common/ProtectedRoute";
import NotFound from "../pages/NotFound";
import Unauthorized from "../pages/Unauthorized";

export default function AppRoutes({ LoginPage, BackofficePage, OperatorPage }) {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route
        path="/backoffice/*"
        element={
          <ProtectedRoute roles={["Backoffice"]}>
            <BackofficePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/operator/*"
        element={
          <ProtectedRoute roles={["GridOperator"]}>
            <OperatorPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}