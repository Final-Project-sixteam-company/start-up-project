terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  common_tags = {
    Project   = "clueroom"
    Env       = "prod"
    ManagedBy = "terraform"
    Purpose   = "app-scaleout-poc"
  }

  app_node_user_data = <<-USERDATA
#!/bin/sh
set -eu

exec > /var/log/clueroom-app-node-bootstrap.log 2>&1

echo "==== ClueRoom app node bootstrap start ===="

timedatectl set-timezone Asia/Seoul || true

mkdir -p /home/ubuntu/.ssh
touch /home/ubuntu/.ssh/authorized_keys

grep -qxF '${var.prod_control_public_key}' /home/ubuntu/.ssh/authorized_keys || echo '${var.prod_control_public_key}' >> /home/ubuntu/.ssh/authorized_keys

EMERGENCY_PUBLIC_KEY='${var.emergency_public_key}'
if [ -n "$EMERGENCY_PUBLIC_KEY" ]; then
  grep -qxF "$EMERGENCY_PUBLIC_KEY" /home/ubuntu/.ssh/authorized_keys || echo "$EMERGENCY_PUBLIC_KEY" >> /home/ubuntu/.ssh/authorized_keys
fi

chown -R ubuntu:ubuntu /home/ubuntu/.ssh
chmod 700 /home/ubuntu/.ssh
chmod 600 /home/ubuntu/.ssh/authorized_keys

apt-get update
apt-get install -y ca-certificates curl gnupg git unzip htop jq nano

if ! swapon --show | grep -q '/swapfile'; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com -o /tmp/get-docker.sh
  sh /tmp/get-docker.sh
fi

usermod -aG docker ubuntu || true

mkdir -p /opt/clueroom/app
mkdir -p /opt/clueroom/secrets/env.d
mkdir -p /opt/clueroom/alloy/data
mkdir -p /opt/clueroom/bin
chown -R ubuntu:ubuntu /opt/clueroom

cat > /opt/clueroom/bootstrap-complete.txt << 'BOOTSTRAP'
clueroom app node bootstrap completed
BOOTSTRAP

chown ubuntu:ubuntu /opt/clueroom/bootstrap-complete.txt

echo "==== ClueRoom app node bootstrap success ===="
USERDATA
}

resource "aws_lightsail_key_pair" "prod_control" {
  name       = var.prod_control_key_pair_name
  public_key = var.prod_control_public_key
}

resource "aws_lightsail_instance" "app" {
  for_each = var.app_servers

  name              = each.value.name
  availability_zone = var.availability_zone
  blueprint_id      = var.blueprint_id
  bundle_id         = var.bundle_id
  key_pair_name     = aws_lightsail_key_pair.prod_control.name
  user_data         = local.app_node_user_data

  tags = merge(local.common_tags, {
    Name = each.value.name
    Role = "app-server"
  })
}

resource "aws_lightsail_instance_public_ports" "app" {
  for_each = aws_lightsail_instance.app

  instance_name = each.value.name

  port_info {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22
    cidrs     = var.ssh_allowed_cidrs
  }
}
