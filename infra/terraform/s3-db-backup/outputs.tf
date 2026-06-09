output "s3_backup_bucket" {
  description = "S3 bucket name for ClueRoom MySQL backups"
  value       = aws_s3_bucket.db_backup.bucket
}

output "s3_backup_prefix" {
  description = "S3 prefix for ClueRoom MySQL backups"
  value       = local.backup_prefix
}

output "iam_user_name" {
  description = "IAM user for uploading ClueRoom MySQL backups"
  value       = aws_iam_user.db_backup_uploader.name
}

output "iam_policy_arn" {
  description = "IAM policy ARN for ClueRoom MySQL backup uploads"
  value       = aws_iam_policy.db_backup_uploader.arn
}
