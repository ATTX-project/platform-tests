Feature: All the custom plugins and installed and working

    Scenario: Check that ATTX attx-l-replaceds plugin is available
        Given that platform is up and running
        Then UV should contain attx-l-replaceds plugin
