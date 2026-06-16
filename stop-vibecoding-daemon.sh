#!/bin/bash
# Script pour arrêter le daemon vibecoding

DAEMON_PID_FILE="/tmp/vibecoding-daemon.pid"

if [ -f "$DAEMON_PID_FILE" ]; then
    DAEMON_PID=$(cat "$DAEMON_PID_FILE")
    echo "Arrêt du daemon avec PID: $DAEMON_PID"
    kill $DAEMON_PID
    rm "$DAEMON_PID_FILE"
    echo "Daemon arrêté"
else
    echo "Aucun daemon en cours d'exécution"
fi