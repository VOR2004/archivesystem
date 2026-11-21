terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.100.0"
    }
  }
}

# Simulate keys that will only be known after creating a service account key.
resource "terraform_data" "credentials" {
  input = {
    key    = "offline-generated-key"
    secret = "offline-generated-secret"
  }
}

provider "aws" {
  region                      = "ru-central1"
  access_key                  = terraform_data.credentials.output.key
  secret_key                  = terraform_data.credentials.output.secret
  shared_credentials_files    = ["${path.module}/../../../aws-bootstrap-credentials"]
  shared_config_files         = []
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_region_validation      = true
  skip_requesting_account_id  = true

  # A real provider, but any DynamoDB request can only reach this local address.
  endpoints {
    dynamodb = "http://127.0.0.1:1/"
  }
}

resource "aws_dynamodb_table" "check" {
  name         = "archives"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "key"

  attribute {
    name = "key"
    type = "S"
  }
}
