resource "yandex_storage_bucket" "archives" {
  bucket    = var.bucket_name
  folder_id = var.folder_id
  max_size  = 10 * 1024 * 1024 * 1024

  anonymous_access_flags {
    read        = false
    list        = false
    config_read = false
  }
}

resource "yandex_ydb_database_serverless" "archives" {
  name      = "${local.prefix}database"
  folder_id = var.folder_id

  # Serverless databases use OLTP; storage_size_limit is in GiB in this provider.
  serverless_database {
    enable_throttling_rcu_limit = true
    throttling_rcu_limit        = 10
    storage_size_limit          = 1
  }
}

# The table's exact name is explicitly required by the assignment.
# name and url are document attributes; only the primary key is declared here.
resource "aws_dynamodb_table" "archives" {
  name         = "archives"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "key"

  attribute {
    name = "key"
    type = "S"
  }

  depends_on = [yandex_resourcemanager_folder_iam_member.system]
}

resource "yandex_message_queue" "dead_letter" {
  name                      = "${local.prefix}dead-letter"
  message_retention_seconds = 14 * 24 * 60 * 60
  access_key                = yandex_iam_service_account_static_access_key.deployment.access_key
  secret_key                = yandex_iam_service_account_static_access_key.deployment.secret_key
}

resource "yandex_message_queue" "tasks" {
  name                       = "${local.prefix}tasks"
  visibility_timeout_seconds = 900
  receive_wait_time_seconds  = 20
  message_retention_seconds  = 24 * 60 * 60
  access_key                 = yandex_iam_service_account_static_access_key.deployment.access_key
  secret_key                 = yandex_iam_service_account_static_access_key.deployment.secret_key

  redrive_policy = jsonencode({
    deadLetterTargetArn = yandex_message_queue.dead_letter.arn
    maxReceiveCount     = 1
  })
}

resource "yandex_function" "archive" {
  name               = "${local.prefix}archive"
  folder_id          = var.folder_id
  runtime            = "java21"
  entrypoint         = "ru.itis.handler.ArchiveHandler"
  memory             = 256
  execution_timeout  = "600"
  service_account_id = yandex_iam_service_account.system["function"].id
  user_hash          = data.archive_file.function.output_sha256

  environment = {
    BUCKET_NAME               = yandex_storage_bucket.archives.bucket
    YDB_DOCUMENT_API_ENDPOINT = yandex_ydb_database_serverless.archives.document_api_endpoint
  }

  content {
    zip_filename = data.archive_file.function.output_path
  }

  depends_on = [
    aws_dynamodb_table.archives,
    yandex_resourcemanager_folder_iam_member.system,
  ]
}

resource "yandex_function_trigger" "tasks" {
  name      = "${local.prefix}tasks-trigger"
  folder_id = var.folder_id

  message_queue {
    queue_id           = yandex_message_queue.tasks.arn
    service_account_id = yandex_iam_service_account.system["trigger"].id
    batch_size         = "1"
    batch_cutoff       = "0"
    visibility_timeout = "900"
  }

  function {
    id                 = yandex_function.archive.id
    service_account_id = yandex_iam_service_account.system["trigger"].id
  }

  depends_on = [yandex_resourcemanager_folder_iam_member.system]
}

resource "yandex_api_gateway" "archives" {
  name      = "${local.prefix}api"
  folder_id = var.folder_id

  spec = templatefile("${path.module}/../openapi.yaml", {
    folder_id                  = var.folder_id
    bucket_name                = yandex_storage_bucket.archives.bucket
    queue_url                  = yandex_message_queue.tasks.id
    database_path              = yandex_ydb_database_serverless.archives.database_path
    gateway_service_account_id = yandex_iam_service_account.system["gateway"].id
  })

  depends_on = [
    aws_dynamodb_table.archives,
    yandex_function_trigger.tasks,
    yandex_resourcemanager_folder_iam_member.system,
  ]
}
