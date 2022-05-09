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

workspace(
  name = "elide",
  managed_directories = {"@npm": ["node_modules"]}
)

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
    "http_file",
)
load(
    "@bazel_tools//tools/build_defs/repo:git.bzl",
    "git_repository",
)
load(
    "//tools:config.bzl",
    "FIREBASE_VERSION",
    "GAX_VERSION",
    "GO_VERSION",
    "GRAALVM_VERSION",
    "GRPC_JAVA_VERSION",
    "GRPC_KT_VERSION",
    "GRPC_VERSION",
    "JAVA_LANGUAGE_LEVEL",
    "MICRONAUT_VERSION",
    "PROTOBUF_VERSION",
    "KOTLIN_SDK_VERSION",
)

http_archive(
    name = "com_google_protobuf",
    sha256 = "3bd7828aa5af4b13b99c191e8b1e884ebfa9ad371b0ce264605d347f135d2568",
    strip_prefix = "protobuf-%s" % PROTOBUF_VERSION,
    urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.tar.gz" % PROTOBUF_VERSION],
)

http_archive(
    name = "com_google_googleapis",
    sha256 = "8d799e239c09abd2154b56922fb1f33d139e62a205a0882639752aaaf9a89be3",
    strip_prefix = "googleapis-f8a290120b3a67e652742a221f73778626dc3081",
    urls = ["https://github.com/googleapis/googleapis/archive/f8a290120b3a67e652742a221f73778626dc3081.tar.gz"],
)

http_archive(
    name = "com_github_grpc_grpc",
    sha256 = "de2d3168e77e5ffb27758b07e87f6066fd0d8087fe272f278771e7780e6aaacb",
    strip_prefix = "grpc-%s" % GRPC_VERSION,
    urls = ["https://github.com/grpc/grpc/archive/v%s.zip" % GRPC_VERSION],
)

http_archive(
    name = "io_grpc_grpc_java",
    sha256 = "48b8cb8adee4b2336e9f646e17a10107b1c8de495e1302d28a17b4816d6a20ca",
    strip_prefix = "grpc-java-%s" % GRPC_JAVA_VERSION,
    url = "https://github.com/grpc/grpc-java/archive/refs/tags/v%s.zip" % GRPC_JAVA_VERSION,
)

http_archive(
    name = "com_github_grpc_grpc_kotlin",
    sha256 = "a1b0e40e4f4f7a88ce525cb8f729c27e2650a78b0a7f29c6b4824b0a22b4e290",
    strip_prefix = "grpc-kotlin-%s" % GRPC_KT_VERSION,
    url = "https://github.com/grpc/grpc-kotlin/archive/%s.tar.gz" % GRPC_KT_VERSION,
)

http_archive(
    name = "com_github_grpc_grpc_web",
    sha256 = "26e0e82a9bde617449185d546677377362cc46835014a3e663891aaf5ef1b9fe",
    strip_prefix = "grpc-web-8c5502186445e35002697f4bd8d1b820abdbed5d",
    url = "https://github.com/grpc/grpc-web/archive/8c5502186445e35002697f4bd8d1b820abdbed5d.tar.gz",
)

http_archive(
    name = "io_grpc_grpc_proto",
    sha256 = "3fb4f25978b16d79d663de64a304f59dcf1b60c5890376956973c8b1bae6f734",
    strip_prefix = "grpc-proto-ab96cf12ec7ce135e03d6ea91d96213fa4cb02af",
    urls = ["https://github.com/grpc/grpc-proto/archive/ab96cf12ec7ce135e03d6ea91d96213fa4cb02af.tar.gz"],
)

http_archive(
    name = "proto_common",
    sha256 = "215220fdbe924a338a789459dd630ce46f9195d3e73efeb3172e201b578a053d",
    strip_prefix = "api-common-protos-e16c55b094638b43a97edd0847614ab91e2461f7",
    build_file = "proto_common.bzl",
    urls = ["https://github.com/googleapis/api-common-protos/archive/e16c55b094638b43a97edd0847614ab91e2461f7.tar.gz"],
)

http_archive(
    name = "safe_html_types",
    sha256 = "2356090e7632f49ea581bb6f8808fa038a7433d433f3e8d7045a36f81fb39d65",
    strip_prefix = "safe-html-types-8507735457ea41a37dfa027fb176d49d5783c4ba",
    build_file = "safe_html_types.bzl",
    urls = ["https://github.com/google/safe-html-types/archive/8507735457ea41a37dfa027fb176d49d5783c4ba.tar.gz"],
)

http_archive(
    name = "rules_proto",
    sha256 = "66bfdf8782796239d3875d37e7de19b1d94301e8972b3cbd2446b332429b4df1",
    strip_prefix = "rules_proto-4.0.0",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/refs/tags/4.0.0.tar.gz",
        "https://github.com/bazelbuild/rules_proto/archive/refs/tags/4.0.0.tar.gz",
    ],
)

http_archive(
    name = "io_bazel_rules_closure",
    sha256 = "2e4aa34e75d57248a168535f66454a5291919728df6433102621125b63b82498",
    strip_prefix = "rules_closure-4d9d3a1fbe5a0ed9a5c84e2ce9b774513041a902",
    url = "https://github.com/dymecard/rules_closure/archive/4d9d3a1fbe5a0ed9a5c84e2ce9b774513041a902.tar.gz",
)

http_archive(
    name = "bazel_gazelle",
    sha256 = "de69a09dc70417580aabf20a28619bb3ef60d038470c7cf8442fafcf627c21cb",
    urls = [
        "https://storage.googleapis.com/bazel-mirror/github.com/bazelbuild/bazel-gazelle/releases/download/v0.24.0/bazel-gazelle-v0.24.0.tar.gz",
        "https://github.com/bazelbuild/bazel-gazelle/releases/download/v0.24.0/bazel-gazelle-v0.24.0.tar.gz",
    ],
)

http_archive(
    name = "bazel_skylib",
    sha256 = "af87959afe497dc8dfd4c6cb66e1279cb98ccc84284619ebfec27d9c09a903de",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.2.0/bazel-skylib-1.2.0.tar.gz",
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.2.0/bazel-skylib-1.2.0.tar.gz",
    ],
)

http_archive(
    name = "rules_python",
    sha256 = "55726ab62f7933d72a8e03d3fd809f1a66e8f67147a3ead6e88638ee8ac11410",
    strip_prefix = "rules_python-6e0cb652386870f72d8dab554a2ce0e4688c9ab5",
    url = "https://github.com/bazelbuild/rules_python/archive/6e0cb652386870f72d8dab554a2ce0e4688c9ab5.zip",
)

http_archive(
    name = "rules_java",
    sha256 = "1e06f514646feda2e3b7a5341b5153f5d9319c4048533ce3024762b422ac1c2d",
    strip_prefix = "rules_java-7a98cf2adcae6a0e2eae0d392f92ea19159fbef1",
    url = "https://github.com/bazelbuild/rules_java/archive/7a98cf2adcae6a0e2eae0d392f92ea19159fbef1.tar.gz",
)

http_archive(
    name = "io_bazel_rules_go",
    sha256 = "d6b2513456fe2229811da7eb67a444be7785f5323c6708b38d851d2b51e54d83",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_go/releases/download/v0.30.0/rules_go-v0.30.0.zip",
        "https://github.com/bazelbuild/rules_go/releases/download/v0.30.0/rules_go-v0.30.0.zip",
    ],
)

http_archive(
    name = "build_bazel_rules_apple",
    sha256 = "a5f00fd89eff67291f6cd3efdc8fad30f4727e6ebb90718f3f05bbf3c3dd5ed7",
    url = "https://github.com/bazelbuild/rules_apple/releases/download/0.33.0/rules_apple.0.33.0.tar.gz",
)

http_archive(
    name = "build_bazel_rules_swift",
    sha256 = "3e52a508cdc47a7adbad36a3d2b712e282cc39cc211b0d63efcaf608961eb36b",
    url = "https://github.com/bazelbuild/rules_swift/releases/download/0.26.0/rules_swift.0.26.0.tar.gz",
)

http_archive(
    name = "grpc_ecosystem_grpc_gateway",
    sha256 = "40029e77c82430366c884089375d6b933b5efed6b72645fab6f1337d1954cc0b",
    strip_prefix = "grpc-gateway-eefd4d7e7e0dd8e34da986bb039a75357e87ef9a",
    url = "https://github.com/grpc-ecosystem/grpc-gateway/archive/eefd4d7e7e0dd8e34da986bb039a75357e87ef9a.tar.gz",
)

http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "85ffff62a4c22a74dbd98d05da6cf40f497344b3dbf1e1ab0a37ab2a1a6ca014",
    strip_prefix = "rules_docker-0.23.0",
    urls = ["https://github.com/bazelbuild/rules_docker/releases/download/v0.23.0/rules_docker-v0.23.0.tar.gz"],
)

http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = "2644a66772938db8d8c760334a252f1687455daa7e188073f2d46283f2f6fbb7",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/4.6.2/rules_nodejs-4.6.2.tar.gz"],
)

http_archive(
    name = "rules_nodejs",
    sha256 = "f596117040134b9497a1049efe7a785924b4ff22557669780a0fa37e22b827bd",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/4.6.2/rules_nodejs-4.6.2.tar.gz"],
)

http_archive(
    name = "io_bazel_rules_webtesting",
    sha256 = "e9abb7658b6a129740c0b3ef6f5a2370864e102a5ba5ffca2cea565829ed825a",
    urls = [
        "https://github.com/bazelbuild/rules_webtesting/releases/download/0.3.5/rules_webtesting.tar.gz",
    ],
)

http_archive(
    name = "incremental_dom",
    sha256 = "33b060bb04c38b4c5c9c203de702dcbc482128be0cead6c5e9e9658dfae8b715",
    strip_prefix = "incremental-dom-a6c91ebc569110993c7c4130f0456504f624459a",
    url = "https://github.com/dymecard/incremental-dom/archive/a6c91ebc569110993c7c4130f0456504f624459a.tar.gz",
)

http_archive(
    name = "remote_java_tools",
    sha256 = "a7ac5922ee01e8b8fcb546ffc264ef314d0a0c679328b7fa4c432e5f54a86067",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/java/v11.6/java_tools-v11.6.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/java_v11.6/java_tools-v11.6.zip",
    ],
)

http_archive(
    name = "remote_java_tools_linux",
    sha256 = "15da4f84a7d39cd179acf3035d9def638eea6ba89a0ed8f4e8a8e6e1d6c8e328",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/java/v11.6/java_tools_linux-v11.6.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/java_v11.6/java_tools_linux-v11.6.zip",
    ],
)

http_archive(
    name = "remote_java_tools_windows",
    sha256 = "939f9d91f0df02851bbad8f5b1d26d24011329394cafe5668c1234e31ac2a1f7",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/java/v11.6/java_tools_windows-v11.6.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/java_v11.6/java_tools_windows-v11.6.zip",
    ],
)

http_archive(
    name = "remote_java_tools_darwin",
    sha256 = "f17ee54582b61f1ebd84c8fa2c54df796914cfbaac3cb821fb1286b55b080bc0",
    urls = [
        "https://mirror.bazel.build/bazel_java_tools/releases/java/v11.6/java_tools_darwin-v11.6.zip",
        "https://github.com/bazelbuild/java_tools/releases/download/java_v11.6/java_tools_darwin-v11.6.zip",
    ],
)

rules_scala_version = "e7a948ad1948058a7a5ddfbd9d1629d6db839933"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "76e1abb8a54f61ada974e6e9af689c59fd9f0518b49be6be7a631ce9fa45f236",
    strip_prefix = "rules_scala-%s" % rules_scala_version,
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
)

http_archive(
    name = "rules_pkg",
    sha256 = "62eeb544ff1ef41d786e329e1536c1d541bb9bcad27ae984d57f18f314018e66",
    url = "https://github.com/bazelbuild/rules_pkg/releases/download/0.6.0/rules_pkg-0.6.0.tar.gz",
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")

rules_pkg_dependencies()

load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")

rules_java_dependencies()

rules_java_toolchains()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
load("@com_google_protobuf//:protobuf_deps.bzl", "PROTOBUF_MAVEN_ARTIFACTS")

protobuf_deps()

rules_kotlin_version = "v1.6.0-RC1"  # v1.6.0-RC1

rules_kotlin_sha = "f1a4053eae0ea381147f5056bb51e396c5c494c7f8d50d0dee4cc2f9d5c701b0"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = rules_kotlin_sha,
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/%s/rules_kotlin_release.tgz" % rules_kotlin_version],
)

http_archive(
    name = "rules_graal",
    sha256 = "6643ab3726d7b5b0ab49476047fcc6032c425390fa75dc6e7f54ed0c5eea0f8d",
    strip_prefix = "rules_graal-b28894f10da85c95b803bd9f1f6e4f3bcc166aef",
    url = "https://github.com/andyscott/rules_graal/archive/b28894f10da85c95b803bd9f1f6e4f3bcc166aef.zip",
)

RULES_JVM_EXTERNAL_TAG = "4.2"

RULES_JVM_EXTERNAL_SHA = "cd1a77b7b02e8e008439ca76fd34f5b07aecb8c752961f9640dea15e9e5ba1ca"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "kotlinc_version")

kotlin_repositories(
    compiler_release = kotlinc_version(
        release = KOTLIN_SDK_VERSION,
        sha256 = "432267996d0d6b4b17ca8de0f878e44d4a099b7e9f1587a98edc4d27e76c215a",
    ),
)

register_toolchains("//tools/defs/kt/toolchain")

load("@io_bazel_rules_closure//closure:repositories.bzl", "rules_closure_dependencies", "rules_closure_toolchains")

rules_closure_dependencies(
    omit_com_google_errorprone_error_prone_annotations = True,
)

rules_closure_toolchains()

load("@build_bazel_rules_apple//apple:repositories.bzl", "apple_rules_dependencies")

apple_rules_dependencies()

load("@build_bazel_rules_swift//swift:repositories.bzl", "swift_rules_dependencies")

swift_rules_dependencies()

load("@build_bazel_rules_swift//swift:extras.bzl", "swift_rules_extra_dependencies")

swift_rules_extra_dependencies()

load("@build_bazel_apple_support//lib:repositories.bzl", "apple_support_dependencies")

apple_support_dependencies()

# Stores Scala version and other configuration
# 2.12 is a default version, other versions can be use by passing them explicitly:
# scala_config(scala_version = "2.11.12")
load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")

scala_config()

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")

go_rules_dependencies()

go_register_toolchains(
    version = GO_VERSION,
)

load("@grpc_ecosystem_grpc_gateway//:repositories.bzl", grpc_ecosystem_repos = "go_repositories")

grpc_ecosystem_repos()

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps", "grpc_test_only_deps")

grpc_deps()

grpc_test_only_deps()

load("@com_github_grpc_grpc//bazel:grpc_extra_deps.bzl", "grpc_extra_deps")

grpc_extra_deps()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

# optional: setup ScalaTest toolchain and dependencies
load("@io_bazel_rules_scala//testing:scalatest.bzl", "scalatest_repositories", "scalatest_toolchain")

scalatest_repositories()

scalatest_toolchain()

load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")

web_test_repositories()

load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.3.bzl", "browser_repositories")

browser_repositories(
    chromium = True,
    firefox = True,
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@io_bazel_rules_docker//repositories:repositories.bzl", container_repositories = "repositories")

container_repositories()

load("@io_bazel_rules_docker//repositories:deps.bzl", container_deps = "deps")

container_deps()

load("@io_bazel_rules_docker//java:image.bzl", java_image_repos = "repositories")

java_image_repos()

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")
load("@build_bazel_rules_nodejs//:index.bzl", "node_repositories", "yarn_install")
load("@io_bazel_rules_docker//container:container.bzl", "container_pull")


load("@com_github_grpc_grpc_kotlin//:repositories.bzl", "IO_GRPC_GRPC_KOTLIN_ARTIFACTS")
load("@com_github_grpc_grpc_kotlin//:repositories.bzl", "IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS")
load("@com_github_grpc_grpc_kotlin//:repositories.bzl", "grpc_kt_repositories")
load("@com_github_grpc_grpc_kotlin//:repositories.bzl", "io_grpc_grpc_java")

io_grpc_grpc_java()

load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_ARTIFACTS")
load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS")
load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_kt_repositories()

grpc_java_repositories()

node_repositories(
    node_version = "16.13.0",
    package_json = ["//:package.json"],
    yarn_version = "1.22.10",
)

yarn_install(
    name = "npm",
    package_json = "//:package.json",
    strict_visibility = True,
    yarn_lock = "//:yarn.lock",
)

container_pull(
    name = "ubuntu_base",
    digest = "sha256:1420d0b28078c665fe6d4ae1f382c0293acbb57ff39a1d85f7f115f806275c33",
    registry = "gcr.io",
    repository = "cloud-marketplace/google/ubuntu2004",
)

load("@rules_graal//graal:graal_bindist.bzl", "graal_bindist_repository")

graal_bindist_repository(
    name = "graal",
    java_version = JAVA_LANGUAGE_LEVEL,
    version = GRAALVM_VERSION,
)

load("//tools/defs/java/testing:junit5.bzl", "junit5_repositories")

junit5_repositories()

load("@io_bazel_rules_webtesting//web:java_repositories.bzl", "RULES_WEBTESTING_ARTIFACTS")

INJECTED_JVM_ARTIFACTS = (
    [i for i in RULES_WEBTESTING_ARTIFACTS if (not "guava" in i and not "gson" in i)] +
    [i for i in (
        IO_GRPC_GRPC_JAVA_ARTIFACTS +
        IO_GRPC_GRPC_KOTLIN_ARTIFACTS
    ) if (not "guava" in i and not "gson" in i)]
)

TEST_ARTIFACTS = [
    "io.micronaut.test:micronaut-test-junit5:3.0.5",
]

NEVERLINK_ARTIFACTS = [
    # None yet.
]

KWRAPPERS_VERSION = "pre.304-kotlin"

maven_install(
    artifacts = [
        "ch.qos.logback:logback-core:1.2.10",
        "ch.qos.logback:logback-classic:1.2.10",
        "com.fasterxml.jackson.core:jackson-annotations:2.13.2",
        "com.fasterxml.jackson.core:jackson-core:2.13.2",
        "com.fasterxml.jackson.core:jackson-databind:2.13.2.2",
        "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.13.2",
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2",
        "com.fasterxml.jackson.module:jackson-module-blackbird:2.13.2",
        "com.fasterxml.jackson.module:jackson-module-parameter-names:2.13.2",
        "com.google.api:api-common:2.1.4",
        "com.google.api:gax:%s" % GAX_VERSION,
        "com.google.api:gax-grpc:%s" % GAX_VERSION,
        "com.google.auto.value:auto-value:1.7.4",
        "com.google.auto.value:auto-value-annotations:1.7.4",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.code.gson:gson:2.8.9",
        "com.google.cloud:google-cloud-core:2.5.6",
        "com.google.cloud:google-cloud-core-grpc:2.5.6",
        "com.google.cloud:google-cloud-firestore:3.0.19",
        "com.google.cloud:google-cloud-spanner:6.21.2",
        "com.google.cloud:google-cloud-redis:2.3.0",
        "com.google.api.grpc:proto-google-cloud-firestore-v1:3.0.19",
        "com.google.cloud:native-image-support:0.12.10",
        "com.google.errorprone:error_prone_annotations:2.9.0",
        "com.google.guava:failureaccess:1.0.1",
        "com.google.guava:guava:30.1.1-android",
        "com.google.mug:mug:5.9",
        "com.google.mug:mug-guava:5.9",
        "com.google.mug:mug-protobuf:5.9",
        "com.google.jimfs:jimfs:1.2",
        "com.google.protobuf:protobuf-java:%s" % PROTOBUF_VERSION,
        "com.google.protobuf:protobuf-java-util:%s" % PROTOBUF_VERSION,
        "com.google.protobuf:protobuf-kotlin:%s" % PROTOBUF_VERSION,
        "com.google.truth:truth:1.1.3",
        "com.google.truth.extensions:truth-proto-extension:1.1.3",
        "com.google.truth.extensions:truth-java8-extension:1.1.3",
        "com.h2database:h2:2.1.212",
        "com.nixxcode.jvmbrotli:jvmbrotli:0.2.0",
        "com.zaxxer:HikariCP:3.4.5",
        "info.picocli:picocli:4.6.3",
        "it.ozimov:embedded-redis:0.7.1",
        "io.grpc:grpc-all:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-alts:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-android:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-auth:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-core:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-context:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-grpclb:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-stub:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-testing:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-netty:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-netty-shaded:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-okhttp:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-protobuf:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-protobuf-lite:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-xds:%s" % GRPC_JAVA_VERSION,
        "io.grpc:grpc-kotlin-stub:1.2.1",
        "io.reactivex.rxjava2:rxjava:2.2.21",
        "io.lettuce:lettuce-core:6.1.6.RELEASE",
        "io.micronaut:micronaut-aop:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-core:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-context:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-inject:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-inject-java:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-runtime:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-validation:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-graal:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-http-client:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-http-server-netty:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-jackson-core:%s" % MICRONAUT_VERSION,
        "io.micronaut:micronaut-jackson-databind:%s" % MICRONAUT_VERSION,
        "io.micronaut.grpc:micronaut-grpc-runtime:3.1.3",
        "io.micronaut.data:micronaut-data-tx:3.3.0",
        "io.micronaut.data:micronaut-data-tx-hibernate:3.3.0",
        "io.micronaut.data:micronaut-data-runtime:3.3.0",
        "io.micronaut.data:micronaut-data-model:3.3.0",
        "io.micronaut.data:micronaut-data-document-model:3.3.0",
        "io.micronaut.data:micronaut-data-processor:3.3.0",
        "io.micronaut.data:micronaut-data-document-processor:3.3.0",
        "io.micronaut.data:micronaut-data-jdbc:3.3.0",
        "io.micronaut.data:micronaut-data-hibernate-jpa:3.3.0",
        "io.micronaut.sql:micronaut-jdbc-hikari:4.2.2",
        "io.micronaut.grpc:micronaut-grpc-server-runtime:3.1.3",
        "io.micronaut.grpc:micronaut-grpc-client-runtime:3.1.3",
        "io.micronaut.kotlin:micronaut-kotlin-extension-functions:3.2.0",
        "io.micronaut.kotlin:micronaut-kotlin-runtime:3.2.0",
        "io.micronaut.redis:micronaut-redis-lettuce:5.2.0",
        "org.hibernate:hibernate-core:5.6.8.Final",
        "org.hibernate:hibernate-hikaricp:5.6.8.Final",
        "org.hibernate:hibernate-jcache:5.6.8.Final",
        "org.hibernate:hibernate-graalvm:5.6.8.Final",
        "org.jetbrains:annotations:23.0.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:1.6.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.0",
        "org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.0",
        "org.postgresql:postgresql:42.3.3",
        "org.reactivestreams:reactive-streams:1.0.3",
        "org.slf4j:slf4j-api:1.7.36",
        "org.threeten:threetenbp:1.5.2",
        "jakarta.inject:jakarta.inject-api:2.0.1",
        "jakarta.persistence:jakarta.persistence-api:3.0.0",
        "javax.annotation:javax.annotation-api:1.3.2",
        "javax.persistence:javax.persistence-api:2.2",
        "javax.validation:validation-api:2.0.1.Final",
        "org.testcontainers:testcontainers:1.16.3",
        "org.testcontainers:gcloud:1.16.3",
        "org.testcontainers:nginx:1.16.3",
        "org.testcontainers:junit-jupiter:1.16.3",
        maven.artifact("org.graalvm.nativeimage", "svm", GRAALVM_VERSION, neverlink = True),
    ] + INJECTED_JVM_ARTIFACTS + [
        maven.artifact(
            testonly = True,
            artifact = n.split(":")[1],
            group = n.split(":")[0],
            version = n.split(":")[2],
        )
        for n in TEST_ARTIFACTS
    ] + [
        maven.artifact(
            neverlink = True,
            artifact = n.split(":")[1],
            group = n.split(":")[0],
            version = n.split(":")[2],
        )
        for n in NEVERLINK_ARTIFACTS
     ],
    fetch_javadoc = True,
    fetch_sources = True,
    generate_compat_repositories = True,
    maven_install_json = "@//:maven_install.json",
    override_targets = dict(
        IO_GRPC_GRPC_KOTLIN_OVERRIDE_TARGETS.items() +
        IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS.items() + ({
            "org.junit.jupiter:junit-jupiter-api": "@org_junit_jupiter_junit_jupiter_api//jar",
            "org.junit.jupiter:junit-jupiter-engine": "@org_junit_jupiter_junit_jupiter_engine//jar",
            "org.junit.jupiter:junit-jupiter-params": "@org_junit_jupiter_junit_jupiter_params//jar",
            "org.junit.platform:junit-platform-commons": "@org_junit_platform_junit_platform_commons//jar",
            "org.junit.platform:junit-platform-console": "@org_junit_platform_junit_platform_console//jar",
            "org.junit.platform:junit-platform-engine": "@org_junit_platform_junit_platform_engine//jar",
            "org.junit.platform:junit-platform-launcher": "@org_junit_platform_junit_platform_launcher//jar",
            "org.junit.platform:junit-platform-suite-api": "@org_junit_platform_junit_platform_suite_api//jar",
        }).items(),
    ),
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
        "https://jcenter.bintray.com/",
        "https://repo.maven.apache.org/maven2",
    ],
    strict_visibility = True,
    version_conflict_policy = "pinned",
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

