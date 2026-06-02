# How to use the OpenTofu plugin

Run OpenTofu CLI commands — init, plan, apply, destroy, and more — from Kestra flows inside a container.

## Common properties

`containerImage` defaults to `ghcr.io/opentofu/opentofu`. Pin a specific version by appending a tag (e.g. `ghcr.io/opentofu/opentofu:1.9.0`). `taskRunner` controls where the container runs — defaults to Docker.

## Tasks

`cli.OpenTofuCLI` runs one or more OpenTofu CLI commands set in `commands` (e.g. `tofu init`, `tofu plan`, `tofu apply -auto-approve`). Use `beforeCommands` for setup steps. Pass Terraform variable files, backend configs, or provider credentials via `inputFiles` or pull them from [namespace files](https://kestra.io/docs/concepts/namespace-files). Pass credentials and secrets as environment variables via `env` — store sensitive values in [secrets](https://kestra.io/docs/concepts/secret). Apply connection or runner properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).
