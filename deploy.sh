#!/bin/bash
# ============================================
# Oracle Cloud Deploy Script
# Multi-Threaded Bank System
# ============================================
# Run this on your Oracle Cloud VM after SSH:
#   chmod +x deploy.sh && ./deploy.sh
# ============================================

set -e

echo "======================================"
echo " Multi-Threaded Bank System - Deploy"
echo "======================================"

# 1. Install Java 17
echo "[1/5] Installing Java 17..."
if ! command -v java &> /dev/null; then
    sudo apt-get update -qq
    sudo apt-get install -y -qq openjdk-17-jdk-headless git
    echo "  Java installed: $(java -version 2>&1 | head -1)"
else
    echo "  Java already installed: $(java -version 2>&1 | head -1)"
fi

# 2. Clone repo
APP_DIR="$HOME/BankSystem"
echo "[2/5] Setting up project..."
if [ -d "$APP_DIR" ]; then
    cd "$APP_DIR"
    git pull origin main
    echo "  Updated existing repo"
else
    git clone https://github.com/baalaganeshr/Multi-Thread-bank-system-.git "$APP_DIR"
    cd "$APP_DIR"
    echo "  Cloned fresh repo"
fi

# 3. Compile
echo "[3/5] Compiling Java sources..."
mkdir -p out
javac -d out \
    src/com/banksystem/util/*.java \
    src/com/banksystem/model/*.java \
    src/com/banksystem/threading/*.java \
    src/com/banksystem/service/*.java \
    src/com/banksystem/web/*.java \
    src/com/banksystem/WebMain.java
echo "  Compilation successful"

# 4. Install systemd service
echo "[4/5] Setting up systemd service..."
sudo tee /etc/systemd/system/banksystem.service > /dev/null <<EOF
[Unit]
Description=Multi-Threaded Bank System
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$APP_DIR
Environment=PORT=8080
ExecStart=/usr/bin/java -cp out com.banksystem.WebMain
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable banksystem
echo "  Service installed and enabled"

# 5. Open firewall port
echo "[5/5] Configuring firewall..."
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 8080 -j ACCEPT 2>/dev/null || true
sudo netfilter-persistent save 2>/dev/null || true
echo "  Port 8080 opened"

# Start the service
sudo systemctl restart banksystem
sleep 2

# Get public IP
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com 2>/dev/null || echo "YOUR_VM_IP")

echo ""
echo "======================================"
echo " DEPLOYMENT COMPLETE!"
echo "======================================"
echo ""
echo " Your app is live at:"
echo "   http://$PUBLIC_IP:8080"
echo ""
echo " Useful commands:"
echo "   sudo systemctl status banksystem   # Check status"
echo "   sudo systemctl restart banksystem  # Restart"
echo "   sudo journalctl -u banksystem -f   # View logs"
echo "   cd $APP_DIR && git pull && ./deploy.sh  # Update"
echo ""
