package io.kestra.plugin.opentofu.cli;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class OpenTofuCLITest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var runner = OpenTofuCLI.builder()
            .id(IdUtils.create())
            .type(OpenTofuCLI.class.getName())
            .commands(Property.ofValue(List.of("tofu version")))
            .build();

        var runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of());

        ScriptOutput output = runner.run(runContext);
        assertThat(output.getExitCode(), is(0));
    }

    @Test
    void envRendering() throws Exception {
        var environmentKey = "MY_KEY";
        var environmentValue = "MY_VALUE";

        var runner = OpenTofuCLI.builder()
            .id(IdUtils.create())
            .type(OpenTofuCLI.class.getName())
            .env(Property.ofValue(Map.of("{{ inputs.environmentKey }}", "{{ inputs.environmentValue }}")))
            .commands(
                Property.ofValue(
                    List.of(
                        "echo \"::{\\\"outputs\\\":{" +
                            "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                            "}}::\""
                    )
                )
            )
            .build();

        var runContext = TestsUtils.mockRunContext(
            runContextFactory,
            runner,
            Map.of("environmentKey", environmentKey, "environmentValue", environmentValue)
        );

        ScriptOutput output = runner.run(runContext);
        assertThat(output.getExitCode(), is(0));
        assertThat(output.getVars().get("customEnv"), is(environmentValue));
    }
}
