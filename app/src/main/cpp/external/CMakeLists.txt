project(external)

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

set(ABSL_PROPAGATE_CXX_STD ON)
add_subdirectory(abseil-cpp)
