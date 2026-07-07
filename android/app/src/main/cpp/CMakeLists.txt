cmake_minimum_required(VERSION 3.22.1)
project("scansetu_native")

add_library(scansetu_native SHARED
    native_bridge.cpp
)

find_library(log-lib log)

target_link_libraries(scansetu_native ${log-lib})