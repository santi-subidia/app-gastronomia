# Skill Registry — api-app-gastronomia

Generated: 2026-06-11
Source: user skills (`~/.config/opencode/skills/`, `~/.agents/skills/`)

| Skill | Trigger | Source | Rules |
|-------|---------|--------|-------|
| branch-pr | Creating, opening, or preparing PRs for review | `~/.config/opencode/skills/branch-pr/SKILL.md` | Every PR MUST link an approved issue; exactly one `type:*` label; automated checks must pass; blank PRs blocked |
| chained-pr | PRs over 400 lines, stacked PRs, review slices | `~/.config/opencode/skills/chained-pr/SKILL.md` | Split PRs >400 lines; each PR reviewable ≤60min; keep tests/docs with unit; dependency diagram required; no strategy mixing |
| cognitive-doc-design | Writing guides, READMEs, RFCs, onboarding, architecture, or review-facing docs | `~/.config/opencode/skills/cognitive-doc-design/SKILL.md` | Lead with answer; progressive disclosure; chunking; signposting; recognition over recall; review empathy |
| comment-writer | PR feedback, issue replies, reviews, Slack messages, or GitHub comments | `~/.config/opencode/skills/comment-writer/SKILL.md` | Be useful fast; warm and direct; keep short; explain why; avoid pile-ons; match thread language; no em dashes |
| go-testing | Go tests, go test coverage, Bubbletea teatest, golden files | `~/.config/opencode/skills/go-testing/SKILL.md` | Prefer table-driven tests; test behavior not implementation; use `t.TempDir()`; integration skippable with `testing.Short()` |
| issue-creation | Creating GitHub issues, bug reports, or feature requests | `~/.config/opencode/skills/issue-creation/SKILL.md` | Blank issues disabled; MUST use template; every issue gets `status:needs-review`; maintainer MUST add `status:approved` before PR |
| judgment-day | Judgment day, dual review, adversarial review, juzgar | `~/.config/opencode/skills/judgment-day/SKILL.md` | Blind dual review in parallel; classify warnings as real/theoretical; terminal states only APPROVED/ESCALATED; max 2 fix iterations |
| skill-creator | New skills, agent instructions, documenting AI usage patterns | `~/.config/opencode/skills/skill-creator/SKILL.md` | Skill = runtime LLM contract; references must be local; target 180–450 tokens; max 1000 tokens body |
| work-unit-commits | Implementation, commit splitting, chained PRs, keeping tests and docs with code | `~/.config/opencode/skills/work-unit-commits/SKILL.md` | Commit by work unit; keep tests/docs with code; tell a story; each commit should be future PR-ready; SDD workload guard |
| find-skills | User asks "how do I do X", "find a skill for X", "is there a skill for X" | `~/.agents/skills/find-skills/SKILL.md` | Check skills.sh leaderboard first; use `npx skills find/add/check/update`; guide user through discovery |
| vercel-react-best-practices | Writing, reviewing, or refactoring React/Next.js code for performance | `~/.agents/skills/vercel-react-best-practices/SKILL.md` | 70 rules across 8 categories; CRITICAL: eliminate waterfalls + bundle size; HIGH: server perf; MEDIUM: re-render + rendering |

## Project Convention Files

| File | Status |
|------|--------|
| `AGENTS.md` | Not found in project |
| `.opencode/` | Not found in project |
| `.editorconfig` | Not found in project |
| `Directory.Build.props` | Not found in project |
