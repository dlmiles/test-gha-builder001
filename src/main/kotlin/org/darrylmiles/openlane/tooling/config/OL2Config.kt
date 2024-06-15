//
//
// SPDX-FileCopyrightText: Copyright 2023 Darryl L. Miles
// SPDX-License-Identifier: Apache2.0
//
//
package org.darrylmiles.openlane.tooling.config

import org.jline.jansi.AnsiConsole
import picocli.CommandLine
import java.io.File
import java.lang.foreign.SymbolLookup
import java.util.concurrent.Callable
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

@CommandLine.Command(name = "ol2config",
    version = ["ol2config~0.1~20240615~00112233"],
    description = ["OpenLane2 configuration file tool"]
)
class OL2Config : Callable<Int> {

    private var outputCommentMode: OutputCommentMode = OutputCommentMode.DEFAULT

    @CommandLine.Option(names=["--output-comment-default"], description=["Default comment mode"], hidden = true)
    private fun optionCommentDefault(value: Boolean) {
        outputCommentMode = OutputCommentMode.DEFAULT
    }
    @CommandLine.Option(names=["--output-comment-keep","--keep-comments"], description=["Keep comments in output"])
    private fun optionCommentKeep(value: Boolean) {
        outputCommentMode = OutputCommentMode.KEEP
    }
    @CommandLine.Option(names=["--output-comment-remove","--remove-comments"], description=["Remove comments from output"])
    private fun optionCommentRemove(value: Boolean) {
        outputCommentMode = OutputCommentMode.REMOVE
    }

    private var outputFormatMode: OutputFormatMode = OutputFormatMode.DEFAULT

    @CommandLine.Option(names=["--format-default"], description=["Default formatted output"], hidden = true)
    private fun optionFormatDefault(value: Boolean) {
        outputFormatMode = OutputFormatMode.DEFAULT
    }
    @CommandLine.Option(names=["--format-pretty","--pretty"], description=["Pretty formatted output"])
    private fun optionFormatPretty(value: Boolean) {
        outputFormatMode = OutputFormatMode.PRETTY
    }
    @CommandLine.Option(names=["--format-compact","--compact"], description=["Compact formatted output"])
    private fun optionFormatCompact(value: Boolean) {
        outputFormatMode = OutputFormatMode.COMPACT
    }
    @CommandLine.Option(names=["--format-minify","--minify"], description=["Minify formatted output"])
    private fun optionFormatMinify(value: Boolean) {
        outputFormatMode = OutputFormatMode.MINIFY
    }

    private var export = OutputFormatMimeType.DEFAULT

    @CommandLine.Option(names=["--export-default","--default"], description=["Output Default (auto-detect)"], hidden = true)
    private fun optionExportDefault(value: Boolean) {
        export = OutputFormatMimeType.DEFAULT
    }
    @CommandLine.Option(names=["--export-shell-csh","--export-csh","--csh"], description=["Output SHELL-BASH"])
    private fun optionExportShellCsh(value: Boolean) {
        export = OutputFormatMimeType.SHELL_CSH
    }
    @CommandLine.Option(names=["--export-shell-bash","--export-bash","--bash"], description=["Output SHELL-CSH"])
    private fun optionExportShellBash(value: Boolean) {
        export = OutputFormatMimeType.SHELL_BASH
    }
    @CommandLine.Option(names=["--export-properties","--properties"], description=["Output PROPERTIES"])
    private fun optionExportProperties(value: Boolean) {
        export = OutputFormatMimeType.PROPERTIES
    }
    @CommandLine.Option(names=["--export-yaml","--yaml"], description=["Output YAML"])
    private fun optionExportYAML(value: Boolean) {
        export = OutputFormatMimeType.YAML
    }
    @CommandLine.Option(names=["--export-toml","--toml"], description=["Output TOML"])
    private fun optionExportTOML(value: Boolean) {
        export = OutputFormatMimeType.TOML
    }
    @CommandLine.Option(names=["--export-xml","--xml"], description=["Output XML"])
    private fun optionExportXML(value: Boolean) {
        export = OutputFormatMimeType.XML
    }
    @CommandLine.Option(names=["--export-tcl","--tcl"], description=["Output TCL"])
    private fun optionExportTCL(value: Boolean) {
        export = OutputFormatMimeType.TCL
    }
    @CommandLine.Option(names=["--export-json","--json"], description=["Output JSON"])
    private fun optionExportJSON(value: Boolean) {
        export = OutputFormatMimeType.JSON
    }
    @CommandLine.Option(names=["--export-jsonc","--jsonc"], description=["Output JSONC"])
    private fun optionExportJsonC(value: Boolean) {
        export = OutputFormatMimeType.JSONC
    }
    @CommandLine.Option(names=["--export-json5","--json5"], description=["Output JSON5"])
    private fun optionExportJson5(value: Boolean) {
        export = OutputFormatMimeType.JSON5
    }

    private var targetOpenlaneVersion = -1

    @CommandLine.Option(names=["--openlane1","--ol1"], description=["Target OpenLane1"])
    private fun optionOpenlane1(value: Boolean) {
        targetOpenlaneVersion = 1
    }

    @CommandLine.Option(names=["--openlane2","--ol2"], description=["Target OpenLane2"])
    private fun optionOpenlane2(value: Boolean) {
        targetOpenlaneVersion = 2
    }

    @CommandLine.Option(names=["--auto"], description=["Automatic conversion"])
    private var auto = false

    private var force = 0
    @CommandLine.Option(names=["--force", "-f"], description=["-f force overwrite,%n-ff even if same content"])
    private fun optionForce(value: Boolean) {
        force++
    }

    @CommandLine.Option(names=["--check"], description=[""])
    private var check = false

    @CommandLine.Option(names=["--dry-run","-n"], description=["Just report, no modification"])
    private var dryrun = false

    @CommandLine.Option(names=["--warn-only"], description=[""])
    private var warnonly = false

    // FIXME this will apply metadata to values
    @CommandLine.Option(names=["--validate"], description=[""])
    private var validate = false

    private var verbosity: Int = 1

    @CommandLine.Option(names=["--verbose","-v"], description=["Verbose"])
    private fun optionVerbose(value: Boolean) {
        verbosity++
    }

    @CommandLine.Option(names=["--quiet","-q"], description=["Quiet"])
    private fun optionQuiet(value: Boolean) {
        verbosity = 0
    }

    @CommandLine.Option(names=["--version","-V"], description=["Show Version"], versionHelp = true)
    private fun optionVersion(value: Boolean) {
        // fetch META-INF and print
        // then exit 0 (if only option)
    }

    @CommandLine.Option(names=["--show-sbom"], description=["Show SBOM"])
    private fun optionShowSbom(value: Boolean) {
        // fetch resource ?
        // then exit 0 (if only option)
        // This requires access to symbol "sbom" and length from exe, to dump raw gzip
        // FIXME refactor/move this to GraalVM (or JRE21+) only class
        val s = SymbolLookup.loaderLookup()
        val a = s.find("sbom")
        val ms = a.get()
        val maxSize = ms.byteSize()     // this is probably not the symbol length (but the remaining exe segment length)
        println("SBOM @${ms.address()} size=${maxSize}")    // zero means not found
    }

    @CommandLine.Option(names=["--show-autocomplete"], description=["Show CLI autocomplete script"])
    private fun optionShowAutocomplete(value: Boolean) {
        //
    }

    @CommandLine.Option(names=["--help","-?"], description=["Show help"], usageHelp = true)
    private fun optionShowUsageHelp(value: Boolean) {
        // usage() ?
    }

    @CommandLine.Option(names=["--dump-config"], description=["Dump internal config"])
    private fun optionDumpConfig(value: Boolean) {
        // write out the internal default configuration, so it can be edited and provided back as a file
        val res =  OL2Config::class.java.classLoader.getResourceAsStream("default_config.yaml")
        res?.use {
            val content = it.readAllBytes()
            System.out.writeBytes(content)
        }
    }

    @CommandLine.Option(names=["--config"], description=["Config file"], paramLabel = "configPath")
    private var config: File? = null

    // Only one file may exist
    @CommandLine.Parameters(index = "0", arity = "0..1", description=["Source Config File"], paramLabel = "inputPath")
    private var file: File? = null

    @CommandLine.Parameters(index = "1", arity = "0..1", description=["Output file (- for stdout)"], paramLabel = "outputPath")
    private var output: String? = null

    @CommandLine.Option(names=["--output","-o"], description = ["Output file (- for stdout)"], paramLabel = "outputPath")
    private fun optionOutput(value: String) {
        output = value
    }


    private fun dumpOptions() {
        println("### OPTIONS")
        println("verbosity=$verbosity")
        println("file=$file")
        println("output=$output")
        println("config=$config")
        println("validate=$validate")
        println("warnonly=$warnonly")
        println("force=$force")
        println("check=$check")
        println("dryrun=$dryrun")
        println("openlane=$targetOpenlaneVersion")
        println("export=$export")
        println("outputFormatMode=$outputFormatMode")
        println("outputCommentMode=$outputCommentMode")
    }

    override fun call(): Int {
        // Arguments

        // status

        // properties
        // yaml
        // jsonc
        // json5

        println("OpenLane2 Config")

        val processor = OL2ConfigInternal()

        processor.autodetect()

        processor.foo()

        dumpOptions()

        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                Logger.getGlobal().level = Level.ALL
                Logger.getGlobal().addHandler(ConsoleHandler())

                val debug = (System.getenv("DEBUG") ?: "false") != "false"
                val logger = Logger.getLogger(this::class.java.canonicalName)
                if(debug) {
                    logger.info("INFO")
                    logger.fine("FINE")
                    logger.config("CONFIG")
                }

                val xlogger = Logger.getLogger("org.jline") // .utils.Log")
                if(debug) {
                    xlogger.level = Level.ALL
                } else {
                    xlogger.level = Level.SEVERE
                }

                System.setProperty("picocli.ansi", "true")

                //System.setProperty("org.jline.terminal.provider", "jansi")
                //System.setProperty("jansi", "true")
                kotlin.runCatching { AnsiConsole.systemInstall() }

                if(debug) {
                    println("${AnsiConsole.isInstalled()}")
                    // println("${AnsiConsole.getTerminal()}")
                    println("${AnsiConsole.out().type}")
                }

                // -Dpicocli.ansi=true  .setColorScheme()

                val target = OL2Config()
                val cmd = CommandLine(target)

                //cmd.setUsageHelpAutoWidth(true)
                val width = AnsiConsole.getTerminalWidth()
                if(width >= 55)     // picocli minimum
                    cmd.setUsageHelpWidth(width)

                val exitStatus = cmd.execute(*args)
                exitProcess(exitStatus)
            } catch(ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

}
