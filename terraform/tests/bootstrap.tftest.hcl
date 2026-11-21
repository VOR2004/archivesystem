# Plan only; the fixture points the actual AWS provider at localhost and disables
# all AWS identity/metadata lookups. No cloud account, token, or live API is used.
run "plans_before_service_account_key_exists" {
  command = plan

  module {
    source = "./tests/fixtures/aws-bootstrap"
  }
}
