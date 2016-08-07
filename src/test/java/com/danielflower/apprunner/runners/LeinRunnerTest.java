package com.danielflower.apprunner.runners;

import com.danielflower.apprunner.io.OutputToWriterBridge;
import org.apache.commons.io.output.StringBuilderWriter;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;
import scaffolding.Photocopier;
import scaffolding.TestConfig;

import java.io.File;

import static com.danielflower.apprunner.runners.SbtRunnerTest.clearlyShowError;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static scaffolding.RestClient.httpClient;

public class LeinRunnerTest {

    private StringBuilderWriter buildLog = new StringBuilderWriter();
    private StringBuilderWriter consoleLog = new StringBuilderWriter();

    @Test
    public void canStartAndStopLeinProjects() throws Exception {
        // doing it twice proves the port was cleaned up
        canStartALeinProject(1);
        canStartALeinProject(2);
    }

    @Test
    public void theVersionIsReported() {
        LeinRunner runner = new LeinRunner(new File("target"), HomeProvider.default_java_home, CommandLineProvider.lein_on_path);
        assertThat(runner.getVersionInfo(), anyOf(containsString("Lein"), equalTo("Not available")));
    }

    public void canStartALeinProject(int attempt) throws Exception {
        String appName = "lein";
        LeinRunner runner = new LeinRunner(
            Photocopier.copySampleAppToTempDir(appName),
            HomeProvider.default_java_home,
            CommandLineProvider.lein_on_path);

        int port = 45678;
        try {
            try (Waiter startupWaiter = Waiter.waitForApp(appName, port)) {
                runner.start(
                    new OutputToWriterBridge(buildLog),
                    new OutputToWriterBridge(consoleLog),
                    TestConfig.testEnvVars(port, appName),
                    startupWaiter);
            }
            try {
                ContentResponse resp = httpClient.GET("http://localhost:" + port + "/" + appName + "/");
                assertThat(resp.getStatus(), is(200));
                assertThat(resp.getContentAsString(), containsString("Hello from lein"));
                assertThat(buildLog.toString(), containsString("Ran 1 tests containing 1 assertions"));
            } finally {
                runner.shutdown();
            }
        } catch (Exception e) {
            clearlyShowError(attempt, e, buildLog, consoleLog);
        }
    }
}
