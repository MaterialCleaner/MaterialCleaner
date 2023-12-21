cmake_minimum_required(VERSION 3.18.1)
project(Cleaner)
set(CMAKE_CXX_STANDARD 20)

set(C_FLAGS "-Werror=format -fdata-sections -ffunction-sections -fno-exceptions -fno-rtti -fno-threadsafe-statics")
set(LINKER_FLAGS "-Wl,--hash-style=both")

if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    message("Builing Release...")

    set(C_FLAGS "${C_FLAGS} -O2 -fvisibility=hidden -fvisibility-inlines-hidden")
    set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections")
elseif (CMAKE_BUILD_TYPE STREQUAL "Debug")
    message("Builing Debug...")
endif ()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${C_FLAGS}")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${LINKER_FLAGS}")
set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} ${LINKER_FLAGS}")

find_package(cxx REQUIRED CONFIG)
find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)
find_library(z-lib z)

add_subdirectory(external)

include_directories(include)
aux_source_directory(genuine GENUINE)
aux_source_directory(include/android-base BASE)
add_library(cleaner SHARED
        ${GENUINE} ${BASE}
        include/misc.cpp include/socket.cpp
        FileUtils.cpp Mount.cpp ProcInfo.cpp
        )
target_link_libraries(cleaner ${log-lib} ${android-lib} ${jnigraphics-lib} ${z-lib} cxx::cxx absl::strings)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET cleaner POST_BUILD
            COMMAND ${CMAKE_STRIP} --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libcleaner.so")
endif ()

add_subdirectory(inline)
add_subdirectory(starter)
