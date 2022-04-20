##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##

"""Provides declarations used by Micronaut macros."""

load(
    "@rules_java//java:defs.bzl",
    _java_library = "java_library",
)
load(
    "//tools/defs/kt:defs.bzl",
    _kt_jvm_library = "kt_jvm_library",
)
load(
    "//tools/defs/java:java.bzl",
    _maven = "maven",
)
load(
    "//tools/defs/model:internals.bzl",
    _javaproto = "javaproto",
    _ktproto = "ktproto",
    _KT_POSTFIX = "KT_POSTFIX",
    _JAVA_POSTFIX = "JAVA_POSTFIX",
)
load(
    "//tools/defs/model:service.bzl",
    _javagrpc = "javagrpc",
    _ktgrpc = "ktgrpc",
    _KTGRPC_POSTFIX = "KTGRPC_POSTFIX",
    _JAVAGRPC_POSTFIX = "JAVAGRPC_POSTFIX",
)

ANNOTATION_PROCESSORS = [
    "io.micronaut.annotation.processing.TypeElementVisitorProcessor",
    "io.micronaut.annotation.processing.AggregatingTypeElementVisitorProcessor",
    "io.micronaut.annotation.processing.PackageConfigurationInjectProcessor",
    "io.micronaut.annotation.processing.BeanDefinitionInjectProcessor",
    "io.micronaut.annotation.processing.ServiceDescriptionProcessor",
]

MICRONAUT_DEPS = [
    "@elide//third_party/micronaut",
    _maven("com.google.guava:guava"),
]

MICRONAUT_RUNTIME_DEPS = [
    # None yet.
]

MICRONAUT_SERVICE_DEPS = [
    "@io_grpc_grpc_java//context",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//stub",
    _maven("com.google.protobuf:protobuf-java"),
    _maven("com.google.protobuf:protobuf-kotlin"),
    _maven("io.micronaut:micronaut-inject"),
    _maven("io.micronaut.grpc:micronaut-grpc-runtime"),
    _maven("io.micronaut.grpc:micronaut-grpc-server-runtime"),
]

MICRONAUT_KT_PLUGINS = [
    "@elide//tools/defs/kt/plugins:serialization",
]

def micronaut_library(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        plugins = [],
        **kwargs):
    """Designate a Micronaut (JVM) library."""
    rule = _java_library
    resolved_plugins = plugins
    if any([src.endswith(".kt") for src in srcs]):
        rule = _kt_jvm_library
        resolved_plugins = resolved_plugins + MICRONAUT_KT_PLUGINS
    _kwargs = {
        "name": name,
        "runtime_deps": runtime_deps + MICRONAUT_RUNTIME_DEPS,
        "plugins": resolved_plugins,
    }
    if len(srcs) > 0:
        _kwargs["srcs"] = srcs
        _kwargs["deps"] = deps + MICRONAUT_DEPS

    rule(
        **_kwargs
    )

def micronaut_service(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        plugins = [],
        protos = [],
        services = [],
        **kwargs):
    """Designate a Micronaut (JVM) library."""
    micronaut_library(
        name = name,
        srcs = srcs,
        runtime_deps = runtime_deps,
        deps = deps + MICRONAUT_SERVICE_DEPS + [
            _javaproto(p)
            for p in (protos + services)
            if (_KT_POSTFIX not in p and _JAVA_POSTFIX not in p)
        ] + [
            _javagrpc(s)
            for s in services
            if (_KTGRPC_POSTFIX not in s and _JAVAGRPC_POSTFIX not in s)
        ],
        plugins = plugins,
    )

maven = _maven
ktproto = _ktproto
javaproto = _javaproto
javagrpc = _javagrpc
ktgrpc = _ktgrpc
