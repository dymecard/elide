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

"""Provides declarations used by Micronaut Data macros."""

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

MICRONAUT_DATA_DEPS = [
    _maven("io.micronaut.data:micronaut-data-processor"),
]

MICRONAUT_DATA_JDBC_DEPS = [
    _maven("io.micronaut.data:micronaut-data-jdbc"),
]

MICRONAUT_DATA_HIKARI_DEPS = [
    _maven("io.micronaut.sql:micronaut-jdbc-hikari"),
]

MICRONAUT_DATA_HIKARI_RUNTIME_DEPS = [
    _maven("com.zaxxer:HikariCP"),
]

MICRONAUT_DATA_ENGINES = [
    "h2",
    "postgresql",
]

MICRONAUT_DATA_ENGINE_DEPS = {
    # Nothing yet.
}

MICRONAUT_DATA_ENGINE_EXPORTS = {
    # Nothing yet.
}

MICRONAUT_DATA_ENGINE_RUNTIME_DEPS = {
    "h2": [
        _maven("com.h2database:h2"),
    ],
    "postgresql": [
        _maven("org.postgresql:postgresql"),
    ],
}
