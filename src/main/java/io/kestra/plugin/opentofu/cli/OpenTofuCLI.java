package io.kestra.plugin.opentofu.cli;

import java.util.List;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run OpenTofu CLI commands in Docker",
    description = "Executes OpenTofu commands inside the task runner container. Defaults to the `ghcr.io/opentofu/opentofu` image. For production use, configure a remote state backend such as S3, GCS, or Terraform Cloud."
)
@Plugin(
    examples = {
        @Example(
            title = "Initialize OpenTofu, then create and apply the plan",
            full = true,
            code = """
                id: git-opentofu
                namespace: company.team

                tasks:
                  - id: working_dir
                    type: io.kestra.plugin.core.flow.WorkingDirectory
                    tasks:
                      - id: clone_repository
                        type: io.kestra.plugin.git.Clone
                        url: https://github.com/your-org/your-repo
                        branch: main

                      - id: opentofu
                        type: io.kestra.plugin.opentofu.cli.OpenTofuCLI
                        beforeCommands:
                          - tofu init
                        inputFiles:
                          terraform.tfvars: |
                            username = "cicd"
                            password = "{{ secret('CI_CD_PASSWORD') }}"
                            hostname = "https://demo.kestra.io"
                        outputFiles:
                          - "*.txt"
                        commands:
                          - tofu plan 2>&1 | tee plan_output.txt
                          - tofu apply -auto-approve 2>&1 | tee apply_output.txt
                        env:
                          AWS_ACCESS_KEY_ID: "{{ secret('AWS_ACCESS_KEY_ID') }}"
                          AWS_SECRET_ACCESS_KEY: "{{ secret('AWS_SECRET_ACCESS_KEY') }}"
                          AWS_DEFAULT_REGION: "{{ secret('AWS_DEFAULT_REGION') }}"
                """
        ),
        @Example(
            title = "Pin OpenTofu version and run validate then plan",
            full = true,
            code = """
                id: opentofu-plan-only
                namespace: company.team

                tasks:
                  - id: opentofu
                    type: io.kestra.plugin.opentofu.cli.OpenTofuCLI
                    containerImage: ghcr.io/opentofu/opentofu:1.9.0
                    beforeCommands:
                      - tofu init -input=false
                    inputFiles:
                      main.tf: |
                        terraform {
                          required_providers {
                            local = { source = "hashicorp/local" }
                          }
                        }
                        resource "local_file" "example" {
                          content  = "hello"
                          filename = "output.txt"
                        }
                    commands:
                      - tofu validate -no-color
                      - tofu plan -input=false -no-color -out=tfplan
                    env:
                      TF_VAR_region: us-east-1
                    outputFiles:
                      - tfplan
                """
        )
    }
)
public class OpenTofuCLI extends AbstractExecScript implements RunnableTask<ScriptOutput> {
    private static final String DEFAULT_IMAGE = "ghcr.io/opentofu/opentofu";

    @Schema(
        title = "OpenTofu Docker image",
        description = "Defaults to `ghcr.io/opentofu/opentofu`. Pin a specific version tag for reproducible builds, e.g. `ghcr.io/opentofu/opentofu:1.9.0`."
    )
    @Builder.Default
    protected Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @Schema(
        title = "Primary OpenTofu CLI commands",
        description = "Main commands run with `/bin/sh -c`, e.g., `tofu plan` or `tofu apply -auto-approve`."
    )
    @NotNull
    protected Property<List<String>> commands;

    @Override
    protected DockerOptions injectDefaults(RunContext runContext, DockerOptions original) throws IllegalVariableEvaluationException {
        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(runContext.render(this.getContainerImage()).as(String.class).orElse(null));
        }
        return builder.build();
    }

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return this.commands(runContext)
            .withInterpreter(this.interpreter)
            .withBeforeCommands(beforeCommands)
            .withBeforeCommandsWithOptions(true)
            .withCommands(commands)
            .run();
    }
}
