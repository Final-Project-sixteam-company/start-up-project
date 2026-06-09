terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "random_id" "bucket_suffix" {
  byte_length = 3
}

locals {
  bucket_name   = "${var.bucket_name_prefix}-${random_id.bucket_suffix.hex}"
  backup_prefix = "mysql/prod"

  common_tags = {
    Project   = "clueroom"
    Env       = "prod"
    ManagedBy = "terraform"
    Purpose   = "mysql-db-backup"
  }
}

resource "aws_s3_bucket" "db_backup" {
  bucket = local.bucket_name

  tags = local.common_tags
}

resource "aws_s3_bucket_public_access_block" "db_backup" {
  bucket = aws_s3_bucket.db_backup.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "db_backup" {
  bucket = aws_s3_bucket.db_backup.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "db_backup" {
  bucket = aws_s3_bucket.db_backup.id

  rule {
    id     = "expire-mysql-backups-after-30-days"
    status = "Enabled"

    filter {
      prefix = "${local.backup_prefix}/"
    }

    expiration {
      days = 30
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

resource "aws_iam_user" "db_backup_uploader" {
  name = var.iam_user_name

  tags = merge(local.common_tags, {
    Purpose = "upload-mysql-backups"
  })
}

data "aws_iam_policy_document" "db_backup_uploader" {
  statement {
    sid    = "ListBackupPrefix"
    effect = "Allow"

    actions = [
      "s3:ListBucket",
    ]

    resources = [
      aws_s3_bucket.db_backup.arn,
    ]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values = [
        local.backup_prefix,
        "${local.backup_prefix}/*",
      ]
    }
  }

  statement {
    sid    = "ReadWriteBackupObjects"
    effect = "Allow"

    actions = [
      "s3:AbortMultipartUpload",
      "s3:GetObject",
      "s3:PutObject",
    ]

    resources = [
      "${aws_s3_bucket.db_backup.arn}/${local.backup_prefix}/*",
    ]
  }
}

resource "aws_iam_policy" "db_backup_uploader" {
  name        = var.iam_policy_name
  description = "Allow ClueRoom data server to upload and verify MySQL backups"
  policy      = data.aws_iam_policy_document.db_backup_uploader.json

  tags = local.common_tags
}

resource "aws_iam_user_policy_attachment" "db_backup_uploader" {
  user       = aws_iam_user.db_backup_uploader.name
  policy_arn = aws_iam_policy.db_backup_uploader.arn
}
