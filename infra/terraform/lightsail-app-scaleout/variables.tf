variable "aws_region" {
  description = "AWS region for ClueRoom Lightsail app scale-out PoC"
  type        = string
  default     = "ap-northeast-2"
}

variable "availability_zone" {
  description = "Lightsail availability zone"
  type        = string
  default     = "ap-northeast-2a"
}

variable "blueprint_id" {
  description = "Lightsail blueprint ID"
  type        = string
  default     = "ubuntu_24_04"
}

variable "bundle_id" {
  description = "Lightsail bundle ID"
  type        = string
  default     = "small_3_0"
}

variable "prod_control_key_pair_name" {
  description = "Lightsail key pair name for prod server control access"
  type        = string
  default     = "clueroom-scaleout-prod-control-key"
}

variable "prod_control_public_key" {
  description = "Public SSH key used by prod server to control app nodes"
  type        = string
}

variable "emergency_public_key" {
  description = "Optional local emergency public SSH key. It is added by user_data when bootstrap succeeds."
  type        = string
  default     = ""
}

variable "ssh_allowed_cidrs" {
  description = "Explicit SSH CIDRs for emergency/team access. Do not use 0.0.0.0/0 for the committed PoC path."
  type        = list(string)

  validation {
    condition = (
      length(var.ssh_allowed_cidrs) > 0
      && alltrue([
        for cidr in var.ssh_allowed_cidrs :
        cidr != "0.0.0.0/0" && cidr != "::/0"
      ])
    )
    error_message = "ssh_allowed_cidrs must be explicit CIDR ranges and must not include 0.0.0.0/0 or ::/0."
  }
}

variable "app_servers" {
  description = "App servers to create for scale-out PoC"
  type = map(object({
    name = string
  }))

  default = {
    app01 = {
      name = "clueroom-app-01"
    }
  }
}
