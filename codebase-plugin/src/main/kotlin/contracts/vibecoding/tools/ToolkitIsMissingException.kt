package contracts.vibecoding.tools

class ToolkitIsMissingException(
    toolName: String?,
) : Exception(buildMessage(toolName)) {
    companion object {
        private fun buildMessage(toolName: String?): String =
            if (toolName == null) {
                "Missing tool in the registry"
            } else {
                "Toolkit is missing the tool '$toolName' in the vibecoding agent registry"
            }
    }
}
