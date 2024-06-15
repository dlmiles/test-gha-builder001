package org.darrylmiles.openlane.tooling.config

enum class OutputFormatMimeType {
    // inverse ol1<>ol2
    // config.tcl in config.json out
    // config.json in config.tcl out
    DEFAULT,
    TCL,
    JSON,
    JSONC,
    JSON5,
    YAML,
    TOML,
    XML,
    SHELL_CSH,
    SHELL_BASH,
    PROPERTIES
}
