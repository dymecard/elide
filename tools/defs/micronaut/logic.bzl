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

"""Provides helpers and macros used by Micronaut server-side logic."""

load(
    "//tools/defs:util.bzl",
    _target_name = "target_name",
)
load(
    "//tools/defs/model:model.bzl",
    _javaproto = "javaproto",
    _ktproto = "ktproto",
)
load(
    "//tools/defs/micronaut:micronaut.bzl",
    _micronaut_library = "micronaut_library",
    _maven = "maven",
)
load(
    "//tools/defs/kt:defs.bzl",
    _kt_jvm_library = "kt_jvm_library",
)

TARGET_POSTFIX_HEADERS = "hdrs"
TARGET_POSTFIX_IMPLEMENTATION = "impl"
TARGET_POSTFIX_COMPOSITE = "composite"

## Re-export useful aliases
maven = _maven
micronaut_library = _micronaut_library
javaproto = _javaproto
ktproto = _ktproto


def logic(name, impl = False):
    """
    Alias resolver for a logic module defined via `logic_library`. By default, the reference will return a target which
    consumes only interfaces from the target library. If implementation dependencies should be exported and included in
    the target affixed with this logic, pass `impl = True`.

    Use this method in a `java_library`, `kt_jvm_library`, or any other compatible target:
    ```starlark
    load(
        "@elide//tools/defs/micronaut:logic.bzl",
        "logic",
    )

    java_library(
      name = "my_lib",
      deps = [
        logic("//your/logic:here"),
      ],
    )
    ```

    Parameters:
        name: Name of the logic target to target. Can be a fully-qualified target path, local target path, or a target
          module entrypoint.
        impl: Whether the target needs implementation dependency edges drawn in the graph. Use of this is heavily
          discouraged as it will cause a re-build of all consuming modules for all target logic module changes.

    Returns:
        Calculated (private) target path for the desired logic dependency.
    """

    if impl:
        return _target_name(name, TARGET_POSTFIX_COMPOSITE)
    return name

def logic_library(
        name,
        hdrs = [],
        impl = [],
        deps = [],
        exports = [],
        private_deps = [],
        runtime_deps = [],
        private_runtime_deps = [],
        public_kwargs = {},
        default_visibility = ["//visibility:private"],
        export_visibility = ["//visibility:public"],
        **kwargs):
    """
    Macro for defining a server-side logic module via Kotlin or Java sources. By separating interfaces (`hdrs`) from
    implementation objects (`impl`), downstream consumers can choose to depend only on the logic module interface,
    without drawing dependencies to implementation code.

    To consume this logic library from any `java_library`-compatible target, use the `logic` alias method in the target
    `deps`.

    This macro sets up a few targets, all of which are `java_library`-compatible. These should not be consumed directly,
    otherwise your application may break if target patterns change in future releases (use the `logic` alias instead):
    - `{name}-hdrs`: Target which provides the declared `hdrs` objects.
    - `{name}-impl`: Target which provides the declared `impl` objects.
    - `{name}-composite`: Target which provides both declared modules as exports.
    - `{name}`: Main target, which acts as an alias to the generated `hdrs` target.

    Parameters:
        name: Name of the main target. A target will be installed (compatible with `java_library`) into the graph at
        this name which only consumes and provides the module's `hdrs`.

        hdrs: Public "headers" (interfaces) to export for this logic library. These should typically be Java or Kotlin
        interfaces, but can be any buildable Java or Kotlin source, really.

        impl: Private "implementation" (classes) to consider for this logic library. These classes are not typically
        exported to consuming targets, unless a special flag is provided (see docs for `logic` alias).

        deps: Dependencies to include on both the headers and implementation targets.
        exports: Dependencies or other targets to include on the header target as exports.
        private_deps: Dependencies which should only be appended to the implementation target.
        runtime_deps: Additional runtime dependencies to add to both targets.
        private_runtime_deps: Additional runtime dependencies to add only to the private implementation target.
        kwargs: Additional keyword arguments to apply to the implementation target.
        public_kwargs: Additional keyword arguments to apply to the header target.
    """

    combined_target = []
    headers_target = _target_name(name, TARGET_POSTFIX_HEADERS)

    _micronaut_library(
        name = headers_target,
        srcs = hdrs,
        deps = (deps or []),
        exports = (exports or []),
        runtime_deps = (runtime_deps or []),
        visibility = export_visibility or ["//visibility:public"],
        **public_kwargs
    )
    combined_target.append(
        headers_target
    )

    if len(impl) > 0:
        implementation_target = _target_name(name, TARGET_POSTFIX_IMPLEMENTATION)

        _micronaut_library(
            name = implementation_target,
            srcs = impl,
            deps = [headers_target] + (deps or []) + (private_deps or []),
            runtime_deps = (runtime_deps or []) + (private_runtime_deps or []),
            visibility = default_visibility or ["//visibility:private"],
            **kwargs
        )
        combined_target.append(
            implementation_target
        )

    # build composite target
    _kt_jvm_library(
        name = _target_name(name, TARGET_POSTFIX_COMPOSITE),
        exports = combined_target,
        visibility = export_visibility or ["//visibility:public"],
    )
    native.alias(
        name = name,
        actual = headers_target,
        visibility = export_visibility or ["//visibility:public"],
    )
