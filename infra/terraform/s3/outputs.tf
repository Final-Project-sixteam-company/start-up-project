output "bucket_name" {
  description = "S3 bucket name"
  value       = aws_s3_bucket.assets.bucket
}

output "bucket_region" {
  description = "S3 bucket region"
  value       = var.aws_region
}

output "bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.assets.arn
}
