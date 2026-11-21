# Cloud providers are mocked: these checks never contact Yandex Cloud or AWS.
# The archive provider stays real to check source packaging from the repository.
mock_provider "yandex" {
  mock_resource "yandex_ydb_database_serverless" {
    defaults = {
      document_api_endpoint = "https://docapi.serverless.yandexcloud.net/ru-central1/test/test"
      database_path         = "/ru-central1/test/test"
    }
  }
}

mock_provider "aws" {}

variables {
  cloud_id    = "test-cloud"
  folder_id   = "test-folder"
  bucket_name = "vvot11-test-archive-bucket"
}

run "connects_resources_and_packages_sources" {
  command = apply

  # The checker can supply another two-digit prefix for the bucket.
  variables {
    bucket_name = "vvot02-test-archive-bucket"
  }

  assert {
    condition = (
      yandex_function_trigger.tasks.function[0].retry_attempts == null
      && yandex_function_trigger.tasks.function[0].retry_interval == null
      && jsondecode(yandex_message_queue.tasks.redrive_policy).maxReceiveCount == 1
    )
    error_message = "Message Queue triggers do not support function retry settings; retries must use the queue redrive policy."
  }

  assert {
    condition = (
      yamldecode(yandex_api_gateway.archives.spec).paths["/tasks"].post["x-yc-apigateway-integration"].queue_url
      == yandex_message_queue.tasks.id
    )
    error_message = "POST /tasks must target the queue created by this configuration."
  }

  assert {
    condition = (
      yandex_storage_bucket.archives.bucket == "vvot02-test-archive-bucket"
      && yamldecode(yandex_api_gateway.archives.spec).paths["/download/{key}"].get["x-yc-apigateway-integration"].bucket
      == yandex_storage_bucket.archives.bucket
    )
    error_message = "The gateway must use the exact bucket name supplied by the checker."
  }

  assert {
    condition = (
      yamldecode(yandex_api_gateway.archives.spec).paths["/archives"].get["x-yc-apigateway-integration"].database
      == yandex_ydb_database_serverless.archives.database_path
      && yandex_function.archive.environment["YDB_DOCUMENT_API_ENDPOINT"]
      == yandex_ydb_database_serverless.archives.document_api_endpoint
    )
    error_message = "The gateway and function must use the same created YDB database."
  }

  assert {
    condition = (
      contains([for entry in data.archive_file.function.source : entry.filename], "pom.xml")
      && contains([for entry in data.archive_file.function.source : entry.filename], "src/main/java/ru/itis/handler/ArchiveHandler.java")
      && data.archive_file.function.output_size > 0
    )
    error_message = "The deployment package must contain Maven configuration and Java sources."
  }
}

run "rejects_bucket_without_student_prefix" {
  command = plan

  variables {
    bucket_name = "another-archive-bucket"
  }

  expect_failures = [var.bucket_name]
}

run "rejects_empty_folder" {
  command = plan

  variables {
    folder_id = ""
  }

  expect_failures = [var.folder_id]
}
