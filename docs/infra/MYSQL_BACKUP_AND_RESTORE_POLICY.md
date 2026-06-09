# ClueRoom MySQL Backup And Restore Policy

> Status: INFRA-20/21 policy, Terraform resource baseline, and rehearsal plan.
> This document does not create AWS resources, change cron, or restore production data.

## 1. Purpose

ClueRoom already has a local MySQL backup script on the production server.

The next step is to make backups recoverable even when the production server is lost or corrupted.

This document defines:

```text
- current local backup baseline
- S3 backup storage policy
- IAM and bucket separation principles
- Terraform-managed backup bucket / IAM resources
- access key handling rules
- restore rehearsal procedure
- forbidden operations and secret handling rules
```

## 2. Current Baseline

Production paths:

```text
/opt/clueroom/backup-mysql.sh
→ MySQL backup script installed on the server

/opt/clueroom/backups/mysql
→ local MySQL backup output directory

/opt/clueroom/logs/mysql-backup.log
→ cron backup log target
```

Repository source:

```text
scripts/backup-mysql.sh
```

Current script behavior:

```text
1. read DB_NAME and DB_PASSWORD from /opt/clueroom/app/.env
2. verify MySQL container connectivity
3. run mysqldump with --single-transaction, --quick, --routines, --triggers
4. gzip the dump
5. write file to /opt/clueroom/backups/mysql
6. chmod backup file to 600
7. delete local backups older than 7 days
```

Current limitation:

```text
Local backups are still on the same production server.
If the server volume is lost, the local backups can be lost with it.
```

## 3. S3 Backup Principles

### Use Private Backup Storage

Preferred:

```text
separate private backup bucket
```

Example:

```text
s3://clueroom-prod-db-backups-apne2-<random_suffix>/mysql/prod/daily/YYYY/MM/DD/{db}_{timestamp}.sql.gz
```

Acceptable only as a temporary fallback:

```text
private backup prefix in a non-public bucket
```

Avoid:

```text
public S3 asset bucket or public official image prefixes
```

Reason:

```text
Database backups must not share public-read policy, CDN assumptions, or image asset access paths.
```

### Bucket Policy

The backup bucket should have:

```text
- Block Public Access enabled
- no public bucket policy
- no public object ACLs
- server-side encryption enabled
- lifecycle expiration policy
- access logs or CloudTrail visibility if available
```

Current Terraform target:

```text
Terraform directory:
infra/terraform/s3-db-backup

Bucket name:
clueroom-prod-db-backups-apne2-<random_suffix>

Backup prefix:
mysql/prod

Lifecycle:
objects under mysql/prod/ expire after 30 days
```

### IAM Policy

Use a dedicated IAM principal for backup upload.

Current IAM principal:

```text
clueroom-prod-db-backup-uploader
```

Minimum permissions:

```text
s3:PutObject
s3:GetObject
s3:ListBucket on backup bucket/prefix
s3:AbortMultipartUpload
```

Do not reuse the public asset upload IAM user if it has public asset management scope.

Do not create `aws_iam_access_key` in Terraform. Terraform state can contain the generated secret access key. Create the access key manually in AWS Console after `terraform apply`, then store it only on the data server.

Data server secret env path:

```text
/opt/clueroom-data/secrets/aws-backup.env
```

Expected env keys:

```env
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_DEFAULT_REGION=ap-northeast-2
S3_BACKUP_BUCKET=clueroom-prod-db-backups-apne2-<random_suffix>
S3_BACKUP_PREFIX=mysql/prod
```

Required file mode:

```bash
chmod 600 /opt/clueroom-data/secrets/aws-backup.env
```

### Encryption

Minimum:

```text
SSE-S3
```

Future stronger option:

```text
SSE-KMS with restricted key policy
```

## 4. Candidate S3 Object Layout

Recommended:

```text
s3://clueroom-prod-db-backups-apne2-<random_suffix>/mysql/prod/daily/2026/06/05/startup_20260605_030000.sql.gz
```

Alternative with environment prefix:

```text
s3://<private-backup-bucket>/mysql/prod/daily/2026/06/05/startup_20260605_030000.sql.gz
```

Do not use:

```text
s3://clueroom-assets-pudding-20260520/official/...
```

The `official/` prefix is for public scenario/image runtime assets, not database backups.

## 5. Backup Script Enhancement Candidate

The current local script should remain the source of truth until the S3 flow is tested.

Potential enhancement sequence:

```text
1. create local dump
2. gzip local dump
3. calculate sha256sum
4. upload .sql.gz to private S3 backup bucket/prefix
5. upload checksum sidecar file
6. verify S3 object exists and has nonzero size
7. keep local retention
8. rely on S3 lifecycle for remote retention
```

Candidate commands for a future script:

```bash
aws s3 cp "$BACKUP_FILE" "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/$DATE_PATH/$(basename "$BACKUP_FILE")" \
  --only-show-errors \
  --server-side-encryption AES256
```

```bash
sha256sum "$BACKUP_FILE" > "$BACKUP_FILE.sha256"
aws s3 cp "$BACKUP_FILE.sha256" "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/$DATE_PATH/$(basename "$BACKUP_FILE").sha256" \
  --only-show-errors \
  --server-side-encryption AES256
```

The command above is documentation-only. Do not run it until the private bucket/IAM/lifecycle decisions are complete.

## 6. Retention Policy Candidate

Local:

```text
7 days
```

S3:

```text
daily backups: 14~30 days
weekly backups: 8~12 weeks if needed
monthly backups: optional after MVP
```

MVP recommendation:

```text
keep it simple: daily backups retained for 30 days in S3
```

Lifecycle should be implemented at the S3 bucket/prefix level, not by committing backup files or manual cleanup lists to git.

## 7. Restore Rehearsal Principles

Restores must be rehearsed before trusting backups.

Do not test restore by overwriting the production DB.

Use one of:

```text
- temporary local MySQL container
- staging/test MySQL container
- disposable rehearsal database name
```

Production restore should be reserved for a real incident and must require human approval.

## 8. Restore Rehearsal Procedure

### 8-1. Select Backup

On the production server:

```bash
ls -lh /opt/clueroom/backups/mysql
```

Or, after S3 upload is implemented:

```bash
aws s3 ls "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/" --recursive | tail -n 20
```

### 8-2. Copy To Rehearsal Host

Prefer testing on a non-production host or disposable container.

If downloading from S3:

```bash
aws s3 cp "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/YYYY/MM/DD/startup_YYYYMMDD_HHMMSS.sql.gz" /tmp/
```

### 8-3. Start Temporary MySQL Container

Example only:

```bash
docker run --name clueroom-restore-rehearsal \
  -e MYSQL_ROOT_PASSWORD=rehearsal_password \
  -d mysql:8.4
```

Wait until MySQL is ready:

```bash
docker exec clueroom-restore-rehearsal mysqladmin ping -uroot -prehearsal_password --silent
```

### 8-4. Restore Into Temporary Container

```bash
gunzip -c /tmp/startup_YYYYMMDD_HHMMSS.sql.gz | \
  docker exec -i clueroom-restore-rehearsal mysql -uroot -prehearsal_password
```

### 8-5. Verify Restored Data

```bash
docker exec -i clueroom-restore-rehearsal mysql -uroot -prehearsal_password <<'SQL'
SHOW DATABASES;
USE startup;
SHOW TABLES;
SELECT COUNT(*) AS scenarios_count FROM scenarios;
SELECT COUNT(*) AS evidences_count FROM evidences;
SELECT COUNT(*) AS play_sessions_count FROM play_sessions;
SQL
```

Scenario-specific smoke candidates:

```sql
SELECT id, code, title, status, visibility
FROM scenarios
WHERE code IN ('SCENARIO_SEOWOLCHAE_LAST_PRESCRIPTION', 'SCENARIO_STUDIO9');
```

### 8-6. Cleanup

```bash
docker rm -f clueroom-restore-rehearsal
rm -f /tmp/startup_YYYYMMDD_HHMMSS.sql.gz
```

## 9. Production Restore Guardrails

Before any production restore:

```text
1. identify incident reason
2. take one more current DB backup if MySQL is reachable
3. verify selected backup timestamp and checksum
4. announce restore window to team
5. stop or gate write traffic if needed
6. restore using documented command
7. run health checks
8. run scenario/session smoke
9. keep the pre-restore backup
```

Production restore is destructive and must not be done from an agent suggestion without human approval.

## 11. Data Server Split Readiness

DB/Redis externalization is a separate cutover from normal app deployment.

Before moving MySQL or Redis to a shared data server:

```text
1. complete at least one non-production restore rehearsal
2. verify backup checksum and restore table counts
3. prepare data server private networking and firewall rules
4. set APP_DB_HOST / APP_REDIS_HOST to private IP or internal DNS through the compose interpolation source
5. use docker-compose.external-data.yml or an equivalent app-only override to remove local mysql/redis depends_on
6. run health, scenario, play-session, interrogation, and final-deduction smoke checks
```

Do not treat Blue-Green app deployment as sufficient rollback protection for DB/Redis migration. If every app slot points to a broken external DB or Redis, both blue and green can fail.

## 12. Security Rules

```text
- Do not commit .sql or .sql.gz backup files.
- Do not commit DB password.
- Do not commit AWS access keys.
- Do not commit terraform.tfstate, terraform.tfvars, tfplan, or local provider cache directories.
- Do not create aws_iam_access_key in Terraform for the backup uploader.
- Do not paste backup contents into PRs, Slack, or AI prompts.
- Do not store database backups in public-read S3 paths.
- Do not use public asset bucket policies for backups.
- Do not run restore against production DB during rehearsal.
```

## 13. Terraform Provisioning

Run Terraform from a local operator machine, not from the production or data server.

```bash
cd infra/terraform/s3-db-backup

terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
```

After apply:

```bash
terraform output -raw s3_backup_bucket
terraform output -raw s3_backup_prefix
terraform output -raw iam_user_name
```

Create the IAM access key manually in AWS Console for `clueroom-prod-db-backup-uploader`. Do not commit or paste the key. Store it only in `/opt/clueroom-data/secrets/aws-backup.env`.

## 14. Open Decisions

```text
- decide whether backup uploads run from cron or a separate controlled script
- decide restore rehearsal cadence
- decide whether SSE-KMS is needed after MVP
```

## 15. Completion Criteria

Policy is complete when:

```text
- private S3 backup bucket is provisioned by Terraform
- backup IAM user and policy are provisioned by Terraform
- IAM access key is manually created and stored only on the data server
- local + S3 retention policy is selected
- restore rehearsal command is tested on non-production MySQL
- production restore guardrails are documented
- no backup files or secrets are committed
```
