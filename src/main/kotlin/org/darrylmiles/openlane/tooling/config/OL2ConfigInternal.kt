//
//
// SPDX-FileCopyrightText: Copyright 2023 Darryl L. Miles
// SPDX-License-Identifier: Apache2.0
//
//
package org.darrylmiles.openlane.tooling.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

class OL2ConfigInternal {

    private fun openResource(name: String): InputStream? {
        return OL2Config::class.java.classLoader.getResourceAsStream(name)
    }

    private fun configureMapper(mapper: ObjectMapper, jsonCSupport: Boolean, json5Support: Boolean, yamlSupport: Boolean) {
        mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)

        if(jsonCSupport) {
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        }
        if(json5Support) {
            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            mapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
            mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true)   // ALLOW_JAVA_COMMENTS ???

            mapper.configure(JsonParser.Feature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS, true)
            mapper.configure(JsonParser.Feature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS, true)
        }
        if(yamlSupport) {
            mapper.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
        }
    }

    internal fun autodetect() {
        // check input file existence
        // check ambiguous input state, force explict use

        // check for output file existence (set OL2 or OL1 mode based on unique name found)
        // assume latest (OL2) by default

        // parse in
        //   stdin ?
        //   file ?

        //   check ?
        //   validate ?
        //      warnonly ?

        // check timestamps of files ?
        //   force ? ignore difference

        // emit to ?
        //   dryrun ?  no modification allowed to files
        //     report status, same content ?
        //     when in dryrun then tool should loudly explain what it would do
        //     config.json is uptodate (OL2 config.yaml)
        //   stdout ?  no modification allowed to files
        //   output ?
        //     report status, same content length ?
        //     force-force ?  then overwrite anyway
        //     backup ?  write ?  replace ?  delete ?
    }

    private fun exportProperties(outputStream: OutputStream, value: Any) {
        val mapper = JavaPropsMapper()
        configureMapper(mapper, false, false, false)
        mapper.writeValue(outputStream, value)
    }

    private fun exportTOML(outputStream: OutputStream, value: Any) {
        val mapper = TomlMapper()
        configureMapper(mapper, false, false, false)
        mapper.writeValue(outputStream, value)
    }

    private fun exportYAML(outputStream: OutputStream, value: Any) {
        val mapper = YAMLMapper()
        configureMapper(mapper, false, false, true)
        mapper.writeValue(outputStream, value)
    }

    private fun exportXML(outputStream: OutputStream, value: Any) {
        val mapper = XmlMapper()
        configureMapper(mapper, false, false, false)
        mapper.writeValue(outputStream, value)
    }

    internal fun foo() {
        val baseDir = File(".").absoluteFile        // cwd

        val mapper = ObjectMapper()
        //mapper.registerModules()
        //mapper.registerKotlinModule()
        configureMapper(mapper, true, true, false)

        val fileJson = File(baseDir, "config.json")
        if(fileJson.isFile) {
            val fileJsonIn = FileInputStream(fileJson)
            fileJsonIn.use {
                val obj = mapper.readTree(fileJsonIn)
                println("${obj}")
            }
        }

        val fileJson5 = File(baseDir, "config.json5")
        if(fileJson5.isFile) {
            val fileJson5In = FileInputStream(fileJson5)
            fileJson5In.use {
                val obj = mapper.readTree(fileJson5In)
                println("${obj}")
            }
        }

        val yamlMapper = YAMLMapper()
        //yamlMapper.registerModules()
        //yamlMapper.registerKotlinModule()
        configureMapper(yamlMapper, false, false, true)

        val fileYaml = File(baseDir, "config.yaml")
        val obj: Any? = if(fileYaml.isFile) {
            val fileYamlIn = FileInputStream("config.yaml")
            fileYamlIn.use {
                val o = yamlMapper.readTree(fileYamlIn)
                println("${o}")
                o
            }
        } else null

        if(obj != null) {
            println("### PROPERTIES")
            exportProperties(System.out, obj)

            println("### TOML")
            exportTOML(System.out, obj)

            println("### YAML")
            exportYAML(System.out, obj)

            println("### XML")
            exportXML(System.out, obj)
            println("")
        }
    }

}
