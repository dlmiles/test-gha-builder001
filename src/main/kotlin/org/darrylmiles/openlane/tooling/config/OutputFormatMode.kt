package org.darrylmiles.openlane.tooling.config

enum class OutputFormatMode {
    // like original input
    DEFAULT,
    // EOLs and blank lines exist
    PRETTY,
    // EOLs exist
    COMPACT,
    // all allowed whitespace removed
    MINIFY
}
