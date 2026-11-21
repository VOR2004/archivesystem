locals {
  prefix = "vvot11-"

  service_accounts = {
    deployment = ["ymq.admin", "ydb.editor"]
    gateway    = ["ymq.writer", "ydb.viewer", "storage.viewer"]
    function   = ["ydb.editor", "storage.uploader"]
    trigger    = ["ymq.reader", "functions.functionInvoker"]
  }

  role_assignments = merge([
    for account, roles in local.service_accounts : {
      for role in roles : "${account}:${role}" => {
        account = account
        role    = role
      }
    }
  ]...)
}

resource "yandex_iam_service_account" "system" {
  for_each = local.service_accounts

  name      = "${local.prefix}${each.key}"
  folder_id = var.folder_id
}

# Individual grants preserve any roles already assigned to the folder's other users.
resource "yandex_resourcemanager_folder_iam_member" "system" {
  for_each = local.role_assignments

  folder_id = var.folder_id
  role      = each.value.role
  member    = "serviceAccount:${yandex_iam_service_account.system[each.value.account].id}"
}

resource "yandex_iam_service_account_static_access_key" "deployment" {
  service_account_id = yandex_iam_service_account.system["deployment"].id
  description        = "${local.prefix}Terraform access to Message Queue and YDB Document API"

  depends_on = [yandex_resourcemanager_folder_iam_member.system]
}
