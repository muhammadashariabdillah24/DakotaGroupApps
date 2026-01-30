package com.dakotagroupstaff.ui.settings

data class HelpGuideItem(
    val title: String,
    val description: String,
    var isExpanded: Boolean = false
)
