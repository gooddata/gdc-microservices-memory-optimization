/*
 * Copyright (C) 2019-2019 GoodData Corporation
 */

import com.github.marcoferrer.krotoplus.config.ProtoBuildersGenOptions
import com.github.marcoferrer.krotoplus.generators.Generator
import com.github.marcoferrer.krotoplus.generators.Generator.Companion.AutoGenerationDisclaimer
import com.github.marcoferrer.krotoplus.proto.ProtoEnum
import com.github.marcoferrer.krotoplus.proto.ProtoFile
import com.github.marcoferrer.krotoplus.proto.ProtoMessage
import com.github.marcoferrer.krotoplus.proto.getGeneratedAnnotationSpec
import com.github.marcoferrer.krotoplus.utils.addFunctions
import com.github.marcoferrer.krotoplus.utils.memoize
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
import com.google.protobuf.compiler.PluginProtos
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName


/**
 * Remove after https://github.com/marcoferrer/kroto-plus/issues/5 will be integrated to Kroto+
 */
object VarArgExtensionGenerator : Generator {

    override val isEnabled: Boolean
        get() = context.config.protoBuildersCount > 0

    val outputFilenameSuffix = "BuildersVarArgExtensions"

    val statementTemplate = "return this.%N(values.toList())"

    override fun invoke(): PluginProtos.CodeGeneratorResponse {
        val responseBuilder = PluginProtos.CodeGeneratorResponse.newBuilder()

        context.schema.protoFiles.asSequence()
            .filter { it.protoMessages.isNotEmpty() }
            .forEach { protoFile ->
                for (options in context.config.protoBuildersList) {
                    if (isFileToGenerate(protoFile.name, options.filter)) {
                        buildFileSpec(protoFile, responseBuilder)
                    }
                }
            }

        return responseBuilder.build()
    }

    private fun buildFileSpec(
        protoFile: ProtoFile,
        responseBuilder: PluginProtos.CodeGeneratorResponse.Builder
    ) {
        val filename = "${protoFile.javaOuterClassname}${outputFilenameSuffix}"
        FileSpec.builder(protoFile.javaPackage, filename)
            .addComment(AutoGenerationDisclaimer)
            .addAnnotation(
                AnnotationSpec.builder(JvmName::class.asClassName())
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "-$filename")
                    .build()
            )
            .buildFunSpecs(protoFile.protoMessages)
            .build()
            .takeIf { it.members.isNotEmpty() }
            ?.let { responseBuilder.addFile(it.toResponseFileProto()) }
    }

    private fun FileSpec.Builder.buildFunSpecs(
        messageTypeList: List<ProtoMessage>
    ): FileSpec.Builder {
        messageTypeList.associate {
            it to it.descriptorProto.fieldList.filter { field -> field.label == LABEL_REPEATED }
        }
            .filterValues { it.isNotEmpty() }
            .map { (protoMessage, repeatedFields) ->
                repeatedFields.map { field ->
                    val fieldNameCamelCase = camelCaseFieldName(field.name)

                    this.addFunction(
                        FunSpec.builder("addAll$fieldNameCamelCase")
                            .addStatement(statementTemplate, "addAll$fieldNameCamelCase")
                            .receiver(protoMessage.builderClassName)
                            .addParameter("values", field.javaClassName, KModifier.VARARG)
                            .build()
                    )
                }
            }
        return this
    }

    private val DescriptorProtos.FieldDescriptorProto.javaClassName: ClassName
        get() = when (this@javaClassName.type!!) {
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 -> Long::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32,
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 -> Int::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> Double::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> Float::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> Boolean::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING -> String::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP -> TODO()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES -> ByteString::class.asClassName()
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE ->
                (context.schema.protoTypes[this@javaClassName.typeName] as ProtoMessage).className
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM ->
                (context.schema.protoTypes[this@javaClassName.typeName] as ProtoEnum).className
        }

    val camelCaseFieldName = { it: String ->
        // We cant use CaseFormat.UPPER_CAMEL since
        // protoc is lenient with malformed field names
        if (it.contains("_")) {
            it.split("_").joinToString(separator = "") { it.capitalize() }
        } else {
            it.capitalize()
        }
    }.memoize()
}
