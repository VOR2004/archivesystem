output "api_url" {
  description = "Точка входа системы: служебный HTTPS-домен API Gateway."
  value       = "https://${yandex_api_gateway.archives.domain}"
}

output "function_id" {
  description = "Идентификатор функции архивирования."
  value       = yandex_function.archive.id
}

output "tasks_queue_url" {
  description = "URL очереди заданий."
  value       = yandex_message_queue.tasks.id
}

output "dead_letter_queue_url" {
  description = "URL очереди необработанных заданий."
  value       = yandex_message_queue.dead_letter.id
}

output "database_id" {
  description = "Идентификатор Serverless-базы YDB."
  value       = yandex_ydb_database_serverless.archives.id
}

output "bucket_name" {
  description = "Имя приватного бакета архивов."
  value       = yandex_storage_bucket.archives.bucket
}
