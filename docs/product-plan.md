# Screenshot Cleaner Product Plan

## Product Brief

Build an Android app that helps users clean old screenshots. The app detects screenshots older than 30 days, notifies the user, and allows the user to review them one by one. The user swipes right to keep a screenshot or swipes left to delete it. The app is simple, private, and works locally without requiring an account.

## Release Order

1. Create Kotlin Android project with Jetpack Compose.
2. Add onboarding and permission explanation screen.
3. Request image/media and notification permissions.
4. Query screenshots from MediaStore.
5. Filter screenshots older than 30 days.
6. Store kept screenshot decisions locally.
7. Add review screen.
8. Add swipe right to keep.
9. Add swipe left to delete with confirmation.
10. Add WorkManager scan.
11. Send notification when old screenshots exist.
12. Open the review screen from notification.
13. Add basic settings screen.
14. Add CI pipeline.
15. Add release/deployment pipeline.

## Automation Policy

Every 2 days, Codex should inspect this project, choose the next ready requirement, implement it on a branch, run available tests, push the branch, and open or update a pull request for user review. Codex should not merge automatically. When a PR is ready, Codex should report the PR link and status; email notification depends on the user's Codex/GitHub notification settings.

