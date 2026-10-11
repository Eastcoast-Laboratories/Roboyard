package roboyard.logic.platform

/**
 * Hand [text] to the platform share mechanism (Android share intent, iOS share
 * sheet, desktop clipboard) so the user can send it e.g. as a bug report.
 * @return true if the text was handed off, false otherwise
 */
expect fun shareText(subject: String, text: String): Boolean
