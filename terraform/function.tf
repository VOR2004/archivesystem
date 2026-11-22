locals {
  java_source_directory = "${path.module}/../src/main/java"
  gson_version = trimspace(regex(
    "(?m)^gsonVersion=([^\\r\\n]+)",
    file("${path.module}/../gradle.properties")
  )[0])
}

# Package sources locally with Terraform; Cloud Functions compiles them using Maven.
# This works from a fresh checkout without a JDK, Gradle, Maven, or a prebuilt JAR.
data "archive_file" "function" {
  type             = "zip"
  output_path      = "${path.module}/.terraform/function-source.zip"
  output_file_mode = "0644"

  source {
    filename = "pom.xml"
    content = templatefile("${path.module}/function-pom.xml.tftpl", {
      gson_version = local.gson_version
    })
  }

  dynamic "source" {
    for_each = fileset(local.java_source_directory, "**/*.java")
    content {
      filename = "src/main/java/${source.value}"
      content  = file("${local.java_source_directory}/${source.value}")
    }
  }
}
