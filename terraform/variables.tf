variable "cloud_id" {
  description = "Идентификатор облака Yandex Cloud."
  type        = string
  nullable    = false

  validation {
    condition     = length(trimspace(var.cloud_id)) > 0
    error_message = "cloud_id не должен быть пустым."
  }
}

variable "folder_id" {
  description = "Идентификатор каталога для всех ресурсов системы."
  type        = string
  nullable    = false

  validation {
    condition     = length(trimspace(var.folder_id)) > 0
    error_message = "folder_id не должен быть пустым."
  }
}

variable "bucket_name" {
  description = "Глобально уникальное имя приватного бакета с префиксом vvotNN-, где NN — две цифры."
  type        = string
  nullable    = false

  validation {
    condition = (
      length(var.bucket_name) >= 8 && length(var.bucket_name) <= 63 &&
      can(regex("^vvot[0-9]{2}-[a-z0-9]([a-z0-9-]*[a-z0-9])?$", var.bucket_name))
    )
    error_message = "Имя бакета должно начинаться с vvotNN- (NN — две цифры), содержать 8–63 символа: строчные латинские буквы, цифры и дефисы, и заканчиваться буквой или цифрой."
  }
}
