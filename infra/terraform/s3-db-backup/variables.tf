variable "aws_region" {
  description = "AWS region for ClueRoom backup resources"
  type        = string
  default     = "ap-northeast-2"
}

variable "bucket_name_prefix" {
  description = "S3 bucket name prefix for ClueRoom prod MySQL backups"
  type        = string
  default     = "clueroom-prod-db-backups-apne2"
}

variable "iam_user_name" {
  description = "IAM user for uploading ClueRoom MySQL backups"
  type        = string
  default     = "clueroom-prod-db-backup-uploader"
}

variable "iam_policy_name" {
  description = "IAM policy name for ClueRoom MySQL backup uploads"
  type        = string
  default     = "clueroom-prod-db-backup-s3-policy"
}
