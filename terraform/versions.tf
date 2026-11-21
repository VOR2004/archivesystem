terraform {
  required_version = ">= 1.7.0, < 2.0.0"

  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = "0.168.0"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "5.100.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "2.7.1"
    }
  }
}
