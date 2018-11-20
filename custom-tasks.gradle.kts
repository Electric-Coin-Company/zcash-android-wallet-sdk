val protoSrcDir = "src/main/proto"
val protoDestDir = "build/generated/source/wire"
val jniSrcDir = "src/main/rust"
val jniDestDir = "build/rust/target"


fun fileTreeWith(dir: String, vararg include: String): ConfigurableFileTree {
    return fileTree(mapOf("dir" to dir, "include" to include))
}

buildscript {
    dependencies {
        classpath("com.squareup.wire:wire-compiler:2.2.0")
    }

    repositories {
        mavenCentral()
    }

}

val generateProtobufClasses: TaskProvider<Task> = tasks.register("generateProtobufClasses").apply {
    configure {
        doFirst {
            println("**** CREATING PROTOS ****")
            delete(protoDestDir)
            mkdir(protoDestDir)
        }
        description = "Generate Java classes from protocol buffer (.proto) schema files for use with Square's Wire library"

        fileTreeWith(protoSrcDir, "**/*.proto").forEach { file ->
            doLast {
                javaexec {
                    main = "com.squareup.wire.WireCompiler"
                    classpath = buildscript.configurations.getByName("classpath")
                    args = listOf("--proto_path=$protoSrcDir", "--java_out=$protoDestDir", file.path)
                }
            }
        }
        inputs.files(fileTreeWith(protoSrcDir, "**/*.proto"))
        outputs.files(fileTreeWith(protoDestDir, "**"))
    }
}

val generateJniLibs: TaskProvider<Task> = tasks.register("generateJniLibs").apply {
    configure {
        doFirst {
            println("**** CREATING JNI LIBS ****")
            delete(jniDestDir)
            mkdir(jniDestDir)
        }
        description = "Generate *.so files for connecting to the Rust wallet logic through the JNI"
        doLast {
            exec {
                commandLine("./build-rust.sh")
            }
        }
        inputs.files(fileTreeWith(jniSrcDir, "**/*"))
        outputs.files(fileTreeWith(jniDestDir, "**/*.so"))
    }
}

tasks["preBuild"]!!.dependsOn(generateProtobufClasses)
tasks["preBuild"]!!.dependsOn(generateJniLibs)
