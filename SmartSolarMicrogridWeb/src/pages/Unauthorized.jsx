import { Link } from "react-router-dom";

export default function Unauthorized() {
  return (
    <main className="login">
      <div className="loginbox">
        <h1>Access denied</h1>
        <p>Your account does not have permission to view this page.</p>
        <Link to="/login">Return to sign in</Link>
      </div>
    </main>
  );
}