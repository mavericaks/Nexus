# Nexus Git Workflow Guide (Full Fledged & Literal)

This is the exact, literal, step-by-step manual you will follow to manage the Git workflow for this repository. Do not skip any steps. This workflow guarantees we never accidentally merge broken code or bypass the CI/CD pipeline.

## 1. Starting a New Phase (Branch Creation)
Always branch off a fresh, updated `main`. Never build a new phase directly on `main`.

### Step 1a: Open your shell and navigate to the project
1. Open your terminal (e.g., PowerShell or Command Prompt).
2. Change your directory to the exact location of the Nexus repository:
```powershell
# Type this and press Enter
cd a:\Nexus
```

### Step 1b: Ensure you are on the main branch
```powershell
# Switch to the main branch
git checkout main
```

### Step 1c: Pull the absolute latest code from GitHub
```powershell
# Download the latest accepted code
git pull origin main
```

### Step 1d: Create your new isolated branch
*Replace `6-resilience` with whatever phase you are working on.*
```powershell
# Create and immediately switch to a new branch
git checkout -b phase-6-resilience
```
You are now safe to start writing code for the phase!

---

## 2. During the Phase: Completing a Unit (Local Commits)
When I (the AI) tell you that a specific unit of work is done, you will do the following:

### Step 2a: Open your shell and navigate to the project (if closed)
```powershell
cd a:\Nexus
```

### Step 2b: Run all tests locally
Never commit code that breaks the build on your own machine.
```powershell
# Run the maven tests (use the exact path to your maven executable or mvn if it's in your PATH)
mvn test
```
*Wait for it to say `BUILD SUCCESS`. If it says `BUILD FAILURE`, tell me to fix it before proceeding.*

### Step 2c: Stage all your changes
```powershell
# Tell Git to prepare all modified, deleted, and new files for the commit
git add -A
```

### Step 2d: Save the commit locally
Write a clear message describing exactly what unit we just finished.
```powershell
# Save the commit to your local branch
git commit -m "Phase 6 Unit 1: Add Redis caching for RAG lookup"
```
> [!TIP]
> We will repeat Section 2 multiple times per phase (Unit 1, Unit 2, Unit 3, etc.). Do not proceed to Section 3 until I explicitly tell you "The Phase is completely finished."

---

## 3. End of the Phase: Merging to Main (The PR Workflow)
When all units for the phase are complete, we use GitHub Pull Requests (PRs) to merge. **Never run `git merge` locally.**

### Step 3a: Push your branch to GitHub
```powershell
# Push your entire local branch up to the remote repository
git push -u origin phase-6-resilience
```

### Step 3b: Open a Pull Request in your Browser
1. Open your web browser (Chrome/Edge/Firefox).
2. Navigate to your repository: `https://github.com/mavericaks/Nexus`
3. At the top of the page, GitHub will show a yellow banner saying `phase-6-resilience had recent pushes`. Click the green button that says **Compare & pull request**.
4. In the title box, type a clear title (e.g., `Phase 6 Complete: Resilience & Rate Limiting`).
5. Leave a brief description of what we built.
6. Click the green **Create pull request** button.

### Step 3c: Wait for the Gate (CI Pipeline)
> [!IMPORTANT]
> **Do NOT click the Merge button yet.** 
> Scroll down to the bottom of the Pull Request page. You will see a yellow circle spinning next to a check called `CI Pipeline / build-and-test`. This means GitHub is running our automated tests.
> 
> * **If it fails (Red ❌):** Do NOT use your admin privileges to bypass it. Come back to the AI IDE, tell me exactly what failed, let me fix it, and repeat **Section 2** to commit the fix. Then run `git push`. The PR will automatically update and test again.
> * **If it passes (Green ✅):** You are officially cleared to merge!

### Step 3d: Merge the Pull Request
1. Once the checks are Green ✅, click the green **Squash and merge** (or **Merge pull request**) button on the GitHub PR page.
2. Click **Confirm merge**.
3. Right after merging, click the **Delete branch** button that appears. This keeps GitHub clean.

---

## 4. Post-Merge Cleanup (Local)
Now that the code is safely merged on GitHub, you need to pull it back down to your laptop so your local `main` is up to date.

### Step 4a: Open your shell and navigate
```powershell
cd a:\Nexus
```

### Step 4b: Switch back to main
```powershell
git checkout main
```

### Step 4b: Download the newly merged code
```powershell
# This pulls down the merge commit you just created on GitHub
git pull origin main
```

### Step 4c: Delete the old local branch
Since it's merged, we no longer need the local phase branch.
```powershell
# Delete the local branch
git branch -D phase-6-resilience
```

You are now back at Step 1, with a perfectly clean, tested, and updated `main` branch, ready to start the next phase!
