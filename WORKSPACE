workspace(
  name = "gust",
  managed_directories = {"@npm": ["node_modules"]})

load("//defs:build.bzl", "install_dependencies")
load("//defs:config.bzl", "CHROMIUM", "FIREFOX", "SAUCE", "GRAALVM_VERSION", "GRAALVM_JDK_VERSION")
install_dependencies()

#
# RULES
#

## Closure
load("@io_bazel_rules_closure//closure:repositories.bzl", "rules_closure_dependencies", "rules_closure_toolchains")
rules_closure_dependencies(
    omit_com_google_auto_common = True,
    omit_com_google_closure_stylesheets = True)
rules_closure_toolchains()

## NodeJS
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = "591d2945b09ecc89fde53e56dd54cfac93322df3bc9d4747cb897ce67ba8cdbf",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/1.2.0/rules_nodejs-1.2.0.tar.gz"],
)

load("@build_bazel_rules_nodejs//:index.bzl",
     "node_repositories",
     "yarn_install")

node_repositories(
    package_json = ["//:package.json"],
    node_version = "10.13.0",
    yarn_version = "1.12.1")

yarn_install(
    name = "npm",
    package_json = "//:package.json",
    yarn_lock = "//:yarn.lock")

load("@npm//:install_bazel_dependencies.bzl",
     "install_bazel_dependencies")

install_bazel_dependencies()

# Setup TypeScript toolchain
load("@npm_bazel_typescript//:index.bzl", "ts_setup_workspace")
ts_setup_workspace()

## SASS
load("@rules_sass//:package.bzl", "rules_sass_dependencies")
rules_sass_dependencies()

load("@rules_sass//:defs.bzl", "sass_repositories")
sass_repositories()

## J2CL
load("@com_google_j2cl//build_defs:rules.bzl", "setup_j2cl_workspace")
setup_j2cl_workspace(
    omit_org_gwtproject_gwt=True,
    omit_com_google_jsinterop_annotations_head = True,
    omit_com_google_closure_stylesheets = True)

## Kotlin
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()

## GraalVM
load("@rules_graal//graal:graal_bindist.bzl", "graal_bindist_repository")

graal_bindist_repository(
  name = "graal",
  version = GRAALVM_VERSION,
  java_version = GRAALVM_JDK_VERSION,
)

## Java Testing
load("//defs/toolchain/java:repos.bzl", "gust_java_repositories")
gust_java_repositories()

load("@maven//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

## Web Testing
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
web_test_repositories()

load("@io_bazel_rules_webtesting//web:go_repositories.bzl", "go_repositories", "go_internal_repositories")
go_repositories()
go_internal_repositories()

load("@io_bazel_rules_webtesting//web:java_repositories.bzl", "java_repositories")
java_repositories()

load("@io_bazel_rules_webtesting//web:py_repositories.bzl", "py_repositories")
py_repositories()

load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
browser_repositories(chromium=CHROMIUM, firefox=FIREFOX, sauce=SAUCE)

## Docker
load("@io_bazel_rules_docker//repositories:repositories.bzl",
     container_repositories = "repositories")
load("@io_bazel_rules_docker//container:container.bzl",
     "container_pull")
load("@io_bazel_rules_docker//java:image.bzl",
     _java_image_repos = "repositories")
load("@io_bazel_rules_docker//repositories:deps.bzl",
     container_deps = "deps")

container_repositories()
container_deps()
_java_image_repos()

## JS Interop
load("@com_google_jsinterop_generator//build_defs:rules.bzl", "setup_jsinterop_generator_workspace")
load("@com_google_jsinterop_generator//build_defs:repository.bzl", "load_jsinterop_generator_repo_deps")
load_jsinterop_generator_repo_deps()
setup_jsinterop_generator_workspace()

## API Codegen
load("@com_google_api_codegen//rules_gapic/java:java_gapic_repositories.bzl", "java_gapic_repositories")
java_gapic_repositories()

load("@com_google_api//:repository_rules.bzl", "switched_rules_by_language")
switched_rules_by_language(
    name = "com_google_googleapis_imports",
    grpc = True)

## Protobuf
load("@build_stack_rules_proto//java:java_proto_compile.bzl", "java_proto_compile")

## Karma Setup
load("@npm_bazel_karma//:package.bzl", "npm_bazel_karma_dependencies")
npm_bazel_karma_dependencies()

## Web Testing Setup
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
 
web_test_repositories()
 
load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
 
browser_repositories(
    chromium = True,
    firefox = True,
)

## Toolchains (RBE)
load("@bazel_toolchains//rules:rbe_repo.bzl", "rbe_autoconfig")
rbe_autoconfig(name = "rbe_default")

## Stardoc
load("@io_bazel_stardoc//:setup.bzl", "stardoc_repositories")
stardoc_repositories()
