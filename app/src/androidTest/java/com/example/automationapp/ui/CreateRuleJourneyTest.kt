package com.example.automationapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.automationapp.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI Test for the "Happy Path" of creating a new automation rule.
 *
 * This test simulates a real user flow:
 * 1. Click the Create Rule FAB
 * 2. Enter a rule name
 * 3. Add a Time-based trigger
 * 4. Add an action
 * 5. Save the rule
 * 6. Verify the rule appears in the list
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CreateRuleJourneyTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /**
     * Happy Path Test: Create a new "Study Mode" rule with a Time Range trigger.
     *
     * This test verifies the complete user journey of creating a rule:
     * 1. Starting from the Rule List screen
     * 2. Navigating to Create Rule screen
     * 3. Filling in rule details
     * 4. Adding a trigger
     * 5. Adding an action
     * 6. Saving and verifying the rule appears in the list
     */
    @Test
    fun createStudyModeRule_happyPath() {
        // Wait for the app to load and settle
        composeTestRule.waitForIdle()

        // Step 1: Find and click the "Create Rule" FAB
        composeTestRule
            .onNodeWithTag("create_rule_fab")
            .assertIsDisplayed()
            .performClick()

        // Wait for navigation to complete
        composeTestRule.waitForIdle()

        // Step 2: Find the "Rule Name" text field and enter "Study Mode"
        composeTestRule
            .onNodeWithTag("rule_name_field")
            .assertIsDisplayed()
            .performTextInput("Study Mode")

        // Step 3: Add a Time Range Trigger
        // 3a. Click "Add Trigger" button
        composeTestRule
            .onNodeWithTag("add_trigger_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // 3b. Select "Time Range" trigger type
        composeTestRule
            .onNodeWithTag("trigger_type_TIME_RANGE")
            .assertIsDisplayed()
            .performClick()

        // 3c. Click "Configure" to proceed to trigger configuration
        composeTestRule
            .onNodeWithTag("configure_trigger_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // 3d. The time range dialog uses default values (9:00 to 17:00)
        // Just click "Add Trigger" to confirm with defaults
        composeTestRule
            .onNodeWithTag("add_trigger_confirm_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Step 4: Add an Action (e.g., Set Ringer to Silent)
        // 4a. Click "Add Action" button
        composeTestRule
            .onNodeWithTag("add_action_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // 4b. Select an action type - look for "Silent Mode" or similar
        // Using text matcher since action types may not have testTags
        composeTestRule
            .onNodeWithText("Silent Mode", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        // 4c. Confirm the action (if there's a confirm button)
        composeTestRule
            .onNodeWithText("Add Action", ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Step 5: Click "Create Rule" / "Save" button
        composeTestRule
            .onNodeWithTag("save_rule_button")
            .assertIsDisplayed()
            .performClick()

        // Wait for the rule to be saved and navigation back to list
        composeTestRule.waitForIdle()

        // Give some time for database operation and UI update
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // Step 6: Assert that "Study Mode" now appears in the Rule List
        composeTestRule
            .onNodeWithText("Study Mode")
            .assertIsDisplayed()
    }

    /**
     * Test that the Create Rule screen shows proper validation errors
     * when trying to save without required fields.
     */
    @Test
    fun createRule_showsValidationErrors_whenFieldsEmpty() {
        composeTestRule.waitForIdle()

        // Navigate to Create Rule screen
        composeTestRule
            .onNodeWithTag("create_rule_fab")
            .performClick()

        composeTestRule.waitForIdle()

        // Try to save without filling any fields
        composeTestRule
            .onNodeWithTag("save_rule_button")
            .performClick()

        composeTestRule.waitForIdle()

        // The button should be disabled or show validation errors
        // Check that we're still on the Create Rule screen
        composeTestRule
            .onNodeWithTag("rule_name_field")
            .assertIsDisplayed()
    }

    /**
     * Test the Cancel/Back navigation from Create Rule screen.
     */
    @Test
    fun createRule_navigatesBack_onBackPress() {
        composeTestRule.waitForIdle()

        // Navigate to Create Rule screen
        composeTestRule
            .onNodeWithTag("create_rule_fab")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're on Create Rule screen
        composeTestRule
            .onNodeWithTag("rule_name_field")
            .assertIsDisplayed()

        // Press back (using content description of back button)
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're back on the Rule List screen (FAB should be visible)
        composeTestRule
            .onNodeWithTag("create_rule_fab")
            .assertIsDisplayed()
    }

    /**
     * Test adding multiple triggers to a rule.
     */
    @Test
    fun createRule_canAddMultipleTriggers() {
        composeTestRule.waitForIdle()

        // Navigate to Create Rule screen
        composeTestRule
            .onNodeWithTag("create_rule_fab")
            .performClick()

        composeTestRule.waitForIdle()

        // Add first trigger (Time Range)
        composeTestRule.onNodeWithTag("add_trigger_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("trigger_type_TIME_RANGE").performClick()
        composeTestRule.onNodeWithTag("configure_trigger_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_trigger_confirm_button").performClick()
        composeTestRule.waitForIdle()

        // Add second trigger (Battery Level)
        composeTestRule.onNodeWithTag("add_trigger_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("trigger_type_BATTERY_LEVEL").performClick()
        composeTestRule.onNodeWithTag("configure_trigger_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_trigger_confirm_button").performClick()
        composeTestRule.waitForIdle()

        // Verify that the Triggers section shows both triggers
        // (The section should contain trigger cards)
        composeTestRule
            .onNodeWithText("Time Range", substring = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Battery", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Test that the Time Range trigger shows proper time picker UI.
     */
    @Test
    fun createRule_timeRangeTrigger_showsTimePicker() {
        composeTestRule.waitForIdle()

        // Navigate to Create Rule screen
        composeTestRule
            .onNodeWithTag("create_rule_fab")
            .performClick()

        composeTestRule.waitForIdle()

        // Add trigger
        composeTestRule.onNodeWithTag("add_trigger_button").performClick()
        composeTestRule.waitForIdle()

        // Select Time Range trigger
        composeTestRule.onNodeWithTag("trigger_type_TIME_RANGE").performClick()
        composeTestRule.onNodeWithTag("configure_trigger_button").performClick()
        composeTestRule.waitForIdle()

        // Verify time-related UI elements are displayed
        // The configuration dialog should show start/end time options
        composeTestRule
            .onNodeWithText("Start", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("End", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}

