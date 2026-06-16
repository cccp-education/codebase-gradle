#!/bin/bash
# Script pour démarrer le daemon vibecoding en arrière-plan

DAEMON_PID_FILE="/tmp/vibecoding-daemon.pid"
LOG_FILE="/tmp/vibecoding-daemon.log"

echo "Démarrage du daemon vibecoding..."
cd /home/cheroliv/workspace/foundry/public/codebase-gradle

# Démarrer le daemon en arrière-plan
nohup ./gradlew sessionProtocolDaemon > "$LOG_FILE" 2>&1 &
DAEMON_PID=$!

# Sauvegarder le PID
echo $DAEMON_PID > "$DAEMON_PID_FILE"

echo "Daemon démarré avec PID: $DAEMON_PID"
echo "Logs disponibles dans: $LOG_FILE"