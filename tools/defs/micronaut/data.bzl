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
    "//tools/defs/micronaut:micronaut.bzl",
    _micronaut_library = "micronaut_library"
)
load(
    "//tools/defs/java:java.bzl",
    _maven = "maven",
)

MICRONAUT_DATA_DEPS = [
    _maven("javax.persistence:javax.persistence-api"),
    _maven("io.micronaut.data:micronaut-data-processor"),
    _maven("io.micronaut.data:micronaut-data-model"),
    _maven("io.micronaut.data:micronaut-data-document-model"),
    _maven("io.micronaut.data:micronaut-data-tx"),
]

MICRONAUT_DATA_RUNTIME_DEPS = [
    _maven("jakarta.persistence:jakarta.persistence-api"),
    _maven("io.micronaut.data:micronaut-data-runtime"),
]

MICRONAUT_DATA_JDBC_EXPORTS = [
    _maven("io.micronaut.data:micronaut-data-jdbc"),
    _maven("javax.validation:validation-api"),
]

MICRONAUT_DATA_JPA_EXPORTS = [
    _maven("io.micronaut.data:micronaut-data-hibernate-jpa"),
]

MICRONAUT_DATA_HIBERNATE_DEPS = [
    _maven("org.hibernate:hibernate-core"),
    _maven("org.hibernate:hibernate-hikaricp"),
    _maven("org.hibernate:hibernate-jcache"),
    _maven("io.micronaut.data:micronaut-data-tx-hibernate"),
]

MICRONAUT_DATA_HIBERNATE_RUNTIME_DEPS = [
    _maven("org.hibernate:hibernate-hikaricp"),
    _maven("org.hibernate:hibernate-jcache"),
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


## Re-export for convenience.
micronaut_library = _micronaut_library

def micronaut_data_library(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        plugins = [],
        exports = [],
        engine = None,
        engines = []):
    """Declare a target with support for Micronaut Data, using the specified engine."""
    engine_deps = []
    engine_runtime_deps = []
    engine_exports = []

    if engine:
        engine_runtime_deps = MICRONAUT_DATA_ENGINE_RUNTIME_DEPS.get(engine, [])
        engine_exports = MICRONAUT_DATA_ENGINE_EXPORTS.get(engine, [])
        if len(srcs) > 0:
            engine_deps = MICRONAUT_DATA_ENGINE_DEPS.get(engine, [])
    elif len(engines) > 0:
        [engine_runtime_deps.extend(i) for i in MICRONAUT_DATA_ENGINE_RUNTIME_DEPS.get(engine, [])]
        [engine_exports.extend(i) for i in MICRONAUT_DATA_ENGINE_EXPORTS.get(engine, [])]
        if len(srcs) > 0:
            [engine_deps.extend(i) for i in MICRONAUT_DATA_ENGINE_DEPS.get(engine, [])]

    _kargs = {
        "name": name,
        "runtime_deps": runtime_deps + engine_runtime_deps + MICRONAUT_DATA_RUNTIME_DEPS,
        "exports": exports + engine_exports,
        "plugins": plugins,
    }

    if len(srcs) > 0:
        _kargs["srcs"] = srcs
        _kargs["deps"] = deps + MICRONAUT_DATA_DEPS + engine_deps

    _micronaut_library(
        **_kargs
    )
