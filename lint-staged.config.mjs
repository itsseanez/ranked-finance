// Runs on `git commit` (pre-commit hook). Staged frontend files use frontend/package.json's config instead,
// because lint-staged picks the closest config to each file.
export default {
  // Spotless formats the whole backend; one run per commit, whatever Java files are staged.
  "backend/**/*.java": () =>
    "./backend/mvnw -q -f backend/pom.xml spotless:apply",
};
