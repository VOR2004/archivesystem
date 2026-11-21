# The Yandex provider reads the administrator's IAM token from YC_TOKEN.
provider "yandex" {
  cloud_id  = var.cloud_id
  folder_id = var.folder_id
}

# This provider talks only to YDB's DynamoDB-compatible Document API, not to AWS.
# Terraform creates these credentials itself; no AWS profile or manual key is needed.
provider "aws" {
  region     = "ru-central1"
  access_key = yandex_iam_service_account_static_access_key.deployment.access_key
  secret_key = yandex_iam_service_account_static_access_key.deployment.secret_key

  # The first plan has no generated key yet. The SDK needs a known fallback even
  # with credential validation disabled; apply uses the explicit real key above.
  shared_credentials_files = ["${path.module}/aws-bootstrap-credentials"]
  shared_config_files      = []

  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_region_validation      = true
  skip_requesting_account_id  = true

  endpoints {
    dynamodb = yandex_ydb_database_serverless.archives.document_api_endpoint
  }
}
