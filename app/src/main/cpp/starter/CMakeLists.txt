add_executable(libstarter.so
        starter.cpp misc.cpp selinux.cpp cgroup.cpp android.cpp)
target_link_libraries(libstarter.so ${log-lib} cxx::cxx)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(TARGET libstarter.so POST_BUILD
            COMMAND ${CMAKE_STRIP} --remove-section=.comment "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libstarter.so")
endif ()
