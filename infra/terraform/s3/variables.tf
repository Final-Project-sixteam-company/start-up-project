variable "aws_region" {
  description = "AWS region for ClueRoom infrastructure"
  type        = string
  default     = "ap-northeast-2"
}

variable "env" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "bucket_name" {
  description = "Globally unique S3 bucket name for ClueRoom assets"
  type        = string
}

variable "allowed_origins" {
  description = "CORS allowed origins for browser-based uploads/downloads"
  type        = list(string)
  default = [
    "https://api.clueroom.xyz",
    "https://clueroom.xyz",
    "http://localhost:3000",
    "http://localhost:5173"
  ]
}
