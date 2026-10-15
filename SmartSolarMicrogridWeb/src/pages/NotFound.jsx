import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <main className="login">
      <div className="loginbox">
        <h1>Page not found</h1>
        <p>The address may be incorrect or the page may have moved.</p>
        <Link to="/login">Return to sign in</Link>
      </div>
    </main>
  );
}