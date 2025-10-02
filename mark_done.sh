#!/bin/bash

# Mark completed tasks
TASKS_FILE="/mnt/d/Projects/KMP/KreeKt/specs/011-reference-https-threejs/tasks.md"

# Camera tasks - already implemented
sed -i 's/- \[ \] \*\*T056\*\*/- [X] **T056**/' "$TASKS_FILE"
sed -i 's/- \[ \] \*\*T057\*\*/- [X] **T057**/' "$TASKS_FILE"
sed -i 's/- \[ \] \*\*T058\*\*/- [X] **T058**/' "$TASKS_FILE"

# Fog - already implemented
sed -i 's/- \[ \] \*\*T061\*\*/- [X] **T061**/' "$TASKS_FILE"

# Raycaster - just implemented
sed -i 's/- \[ \] \*\*T062\*\*/- [X] **T062**/' "$TASKS_FILE"
sed -i 's/- \[ \] \*\*T063\*\*/- [X] **T063**/' "$TASKS_FILE"

echo "Marked T056-T058, T061-T063 as complete"
