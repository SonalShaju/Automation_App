package com.example.automationapp.domain.model

import com.example.automationapp.data.local.entity.*

data class CompleteRule(
    val rule: AutomationRule,
    val triggers: List<Trigger>,
    val actions: List<Action>
)