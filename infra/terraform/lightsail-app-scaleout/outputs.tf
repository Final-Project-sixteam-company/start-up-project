output "prod_control_key_pair_name" {
  description = "Lightsail key pair name used for prod server control access"
  value       = aws_lightsail_key_pair.prod_control.name
}

output "app_private_ips" {
  description = "Private IPs of app servers for Nginx upstream"
  value = {
    for k, v in aws_lightsail_instance.app : k => v.private_ip_address
  }
}

output "app_public_ips" {
  description = "Public IPs of app servers for emergency SSH access"
  value = {
    for k, v in aws_lightsail_instance.app : k => v.public_ip_address
  }
}

output "nginx_upstream_candidates" {
  description = "Nginx upstream server lines for manual load balancing"
  value = [
    for k, v in aws_lightsail_instance.app :
    "server ${v.private_ip_address}:8080 max_fails=3 fail_timeout=10s weight=1;"
  ]
}

output "prod_control_ssh_check_commands" {
  description = "Commands to check prod-to-app-node SSH access"
  value = [
    for k, v in aws_lightsail_instance.app :
    "ssh -i /opt/clueroom/scaleout/keys/app-node-control ubuntu@${v.private_ip_address} 'hostname && cat /opt/clueroom/bootstrap-complete.txt'"
  ]
}
