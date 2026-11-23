// Conventional Commits, checked by the commit-msg hook and in CI. See CLAUDE.md → Commits.
export default {
  extends: ["@commitlint/config-conventional"],
  // Dependabot writes its own titles and bodies ("build(deps-dev): bump X from 1 to 2 in /frontend in the
  // npm group across 1 directory") and often goes past the 100-character limit. Its messages already
  // follow the type(scope) format via dependabot.yml, so skip linting them.
  ignores: [(message) => /^(build|ci)\(deps(-dev)?\): bump /.test(message)],
};
