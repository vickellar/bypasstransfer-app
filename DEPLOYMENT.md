# ByPass Transfers - Deployment Guide

## Overview
This guide will help you deploy the ByPass Transfers application for production use with 50-200 concurrent users.

## System Requirements

### Minimum Server Specifications
| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **RAM** | 2 GB | 4 GB |
| **CPU** | 2 cores | 4 cores |
| **Disk** | 20 GB SSD | 50 GB SSD |
| **OS** | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| **Java** | OpenJDK 17 | OpenJDK 17 |
| **Database** | PostgreSQL 14+ | PostgreSQL 15+ |

### Network Requirements
- **Ports**: 80 (HTTP), 443 (HTTPS), 22 (SSH)
- **Bandwidth**: 1 Mbps minimum, 10 Mbps recommended

## Pre-Deployment Checklist

### 1. Database Setup
```sql
-- Create production database
CREATE DATABASE bypass_records_prod;

-- Create dedicated user (recommended)
CREATE USER bypass_app WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE bypass_records_prod TO bypass_app;
```

### 2. Environment Variables
Set these environment variables on your server:
```bash
export DB_USERNAME=bypass_app
export DB_PASSWORD=your_secure_password
export SPRING_PROFILES_ACTIVE=prod
```

### 3. Server Configuration

#### Install Java 17
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

#### Install PostgreSQL
```bash
sudo apt install postgresql postgresql-contrib
sudo systemctl enable postgresql
sudo systemctl start postgresql
```

#### Create Application Directory
```bash
sudo mkdir -p /opt/bypasstransers
sudo mkdir -p /opt/bypasstransers/logs
sudo chown -R $USER:$USER /opt/bypasstransers
```

## Deployment Steps

### 1. Build the Application
On your local machine or build server:
```bash
cd bypasstransers
mvn clean package -DskipTests
```

### 2. Copy JAR to Server
```bash
scp target/bypasstransers-0.0.1-SNAPSHOT.jar user@your-server:/opt/bypasstransers/
```

### 3. Create Systemd Service
Create `/etc/systemd/system/bypasstransers.service`:
```ini
[Unit]
Description=ByPass Transfers Application
After=postgresql.service

[Service]
Type=simple
User=bypass
Group=bypass
WorkingDirectory=/opt/bypasstransers
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_USERNAME=bypass_app"
Environment="DB_PASSWORD=your_secure_password"
ExecStart=/usr/bin/java -Xms1g -Xmx2g -jar /opt/bypasstransers/bypasstransers-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 4. Configure Nginx (Reverse Proxy)
Install Nginx:
```bash
sudo apt install nginx
```

Create `/etc/nginx/sites-available/bypasstransers`:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/bypasstransers /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 5. SSL Certificate (Let's Encrypt)
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

### 6. Start the Application
```bash
sudo systemctl daemon-reload
sudo systemctl enable bypasstransers
sudo systemctl start bypasstransers
```

### 7. Verify Deployment
Check application health:
```bash
curl http://localhost:8080/actuator/health
```

View logs:
```bash
sudo tail -f /opt/bypasstransers/logs/bypasstransers.log
```

## Performance Tuning

### JVM Options
For 2-4 GB RAM server, use:
```bash
java -Xms1g -Xmx2g -XX:+UseG1GC -jar app.jar
```

### Database Tuning
Edit `/etc/postgresql/15/main/postgresql.conf`:
```conf
# Memory settings
shared_buffers = 512MB
effective_cache_size = 1536MB
work_mem = 16MB
maintenance_work_mem = 256MB

# Connection settings
max_connections = 100

# Logging
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
```

Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

## Monitoring

### Health Check Endpoint
```
http://your-domain.com/actuator/health
```

### Metrics Endpoint
```
http://your-domain.com/actuator/metrics
```

### Key Metrics to Monitor
- **Response Time**: Should be < 500ms (p95)
- **Throughput**: Target 100-200 requests/second
- **Memory Usage**: Should stay below 2 GB
- **Database Connections**: Monitor active connections
- **Error Rate**: Should be < 1%

## Backup Strategy

### Database Backup
```bash
# Daily backup
0 2 * * * pg_dump -U bypass_app bypass_records_prod > /backups/bypass_$(date +\%Y\%m\%d).sql

# Weekly full backup
0 3 * * 0 pg_dumpall -U postgres > /backups/full_$(date +\%Y\%m\%d).sql
```

### Application Backup
```bash
# Backup configuration
cp /opt/bypasstransers/application-prod.properties /backups/
```

## Troubleshooting

### Application Won't Start
```bash
# Check logs
sudo journalctl -u bypasstransers -f

# Check port availability
sudo netstat -tlnp | grep 8080

# Check database connection
psql -U bypass_app -h localhost -d bypass_records_prod
```

### High Memory Usage
```bash
# Check JVM memory usage
jps -lvm
jmap -heap <pid>

# Check for memory leaks
jmap -histo:live <pid> | head -20
```

### Slow Queries
```bash
# Check PostgreSQL slow query log
sudo tail -f /var/log/postgresql/postgresql-15-main.log
```

## Security Checklist

- [ ] Change default database password
- [ ] Enable firewall (ufw)
- [ ] Configure SSL certificate
- [ ] Disable root SSH login
- [ ] Set up fail2ban
- [ ] Regular security updates
- [ ] Database backups encrypted
- [ ] Application logs secured

## Support

For issues or questions:
1. Check application logs: `/opt/bypasstransers/logs/`
2. Check system logs: `sudo journalctl -u bypasstransers`
3. Review health endpoint: `/actuator/health`
4. Monitor metrics: `/actuator/metrics`

## Expected Performance

After deployment, you should see:
- **Response Time**: < 500ms (95th percentile)
- **Throughput**: 100-200 requests/second
- **Concurrent Users**: 50-200 without degradation
- **Uptime**: 99.9%+
- **Memory Usage**: 1-2 GB under normal load
