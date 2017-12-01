# Platform Tests

ATTX Linked Data Broker tests repository. Images for platform available https://github.com/ATTX-project/platform-deployment

User guide for running the tests: https://attx-project.github.io/Containerized-testing.html

## Testing with Gradle

There are two tasks for running the tests:
* `runIntegTests` -  for running the test in the local environment with all the ports exposed, thus allowing for rapid development; It will also start the containers needed, by default they will expose ports; Adding a `-PrunEnv=console` parameter will allow for continuous testing without removing any of the started containers;
* `runContainerTests` - for running tests in the CI environment or a closed test setup, and for this one needs the Gradle property `-PtestEnv=CI`. This task will build and run the tests inside a container on the same network as the other containers without the need of exposing all the ports.

Running the tests inside the container:
* `gradle -PregistryURL=attx-dev:5000 -PsnapshotRepoURL=http://attx-dev:8081 -PtestEnv=CI clean runContainerTests`

Run the test locally and exposing the ports. At the end of the tests the containers are removed:
* `gradle -PregistryURL=attx-dev:5000 -PsnapshotRepoURL=http://attx-dev:8081 -PtestEnv=dev clean runIntegTests`

Run the test locally from console and exposing the container ports:
* `gradle -PregistryURL=attx-dev:5000 -PsnapshotRepoURL=http://attx-dev:8081 -PtestEnv=dev -PrunEnv=console clean runIntegTests`

Running a specific test class:
* `gradle -PregistryURL=attx-dev:5000 -PartifactRepoURL=http://attx-dev:8081 -PtestEnv=dev -PrunEnv=console runIntegTests --tests org.uh.attx.platform.test.GraphManagerIntegrationTest`

Test reports are exported to the folder configured in `copyReportFiles` task (default `$buildDir/reports/`).
