cmake_minimum_required(VERSION 3.13)

project(clea C)

include(ExternalProject)
set(CMAKE_C_FLAGS  "-W -Werror -Wextra -Wall -Wunreachable-code -pedantic -fno-builtin -std=c99 -DAES256=1 -DWITH_STDLIB=1")

if("${CMAKE_BUILD_TYPE}" STREQUAL "Debug")
    set(ECC_DEBUG debug)
else("${CMAKE_BUILD_TYPE}" STREQUAL "Debug")
    set(CMAKE_C_FLAGS  "${CMAKE_C_FLAGS} -Os -fdata-sections -ffunction-sections")
    set(CMAKE_EXE_LINKER_FLAGS "-Wl,--gc-sections -Wl,--strip-all")
endif("${CMAKE_BUILD_TYPE}" STREQUAL "Debug")

ExternalProject_Add(libecc
                    PREFIX libecc
                    GIT_REPOSITORY https://github.com/ANSSI-FR/libecc.git
                    GIT_TAG d26279b1e5e5e07bc21eb3bbe9d5cdc8bac4048e
                    GIT_PROGRESS true
                    PATCH_COMMAND patch -N -p1 < ${CMAKE_CURRENT_SOURCE_DIR}/libecc.patch
                    BUILD_IN_SOURCE true
                    CONFIGURE_COMMAND ""
                    BUILD_COMMAND make CFLAGS=${CMAKE_C_FLAGS} ${ECC_DEBUG}
                    INSTALL_COMMAND ""
                   )

ExternalProject_Add(tiny_aes
                    PREFIX tiny_aes
                    GIT_REPOSITORY https://github.com/kokke/tiny-AES-c.git
                    GIT_TAG 12e7744b4919e9d55de75b7ab566326a1c8e7a67
                    GIT_PROGRESS true
                    BUILD_IN_SOURCE true
                    CONFIGURE_COMMAND ""
                    BUILD_COMMAND make AES256=1 CBC=0 CTR=0 aes.a
                    INSTALL_COMMAND mv aes.a libaes.a
                   )

set(LIBECC_PATH ${CMAKE_CURRENT_BINARY_DIR}/libecc/src/libecc/src)
set(LIBTINY_AES_PATH ${CMAKE_CURRENT_BINARY_DIR}/tiny_aes/src/tiny_aes)

include_directories(${LIBECC_PATH} ${LIBTINY_AES_PATH} qrcodegen)
link_directories(${LIBECC_PATH}/../build ${LIBTINY_AES_PATH})

# CLEA library
add_library(clea STATIC aes-gcm hmac-sha256 ecies clea)
target_link_libraries(clea ec arith sign aes external_deps)
add_dependencies(clea libecc tiny_aes)

# Tests
add_library(test_util STATIC test_util)

add_executable(test_hmac-sha256-128 test_hmac-sha256-128)
target_link_libraries(test_hmac-sha256-128 clea)

add_executable(test_aes-gcm test_aes-gcm)
target_link_libraries(test_aes-gcm clea test_util) 

add_executable(test_ecies test_ecies)
target_link_libraries(test_ecies clea test_util) 

add_executable(test_clea test_clea)
target_link_libraries(test_clea clea test_util)

add_executable(simple_clea simple_clea)
target_link_libraries(simple_clea clea test_util)

# Raspberry PI specific rules
if("${PLATFORM}" STREQUAL "RPI")

ExternalProject_Add(openvg
                    PREFIX openvg
                    GIT_REPOSITORY https://github.com/ajstarks/openvg.git
                    GIT_TAG c6885d824eb8df762e1a654585bef54d9bf46764
                    GIT_PROGRESS true
                    BUILD_IN_SOURCE true
                    CONFIGURE_COMMAND ""
                    BUILD_COMMAND make
                    INSTALL_COMMAND ""
                   )

set(OPENVG_PATH ${CMAKE_CURRENT_BINARY_DIR}/openvg/src/openvg/)
include_directories(/opt/vc/include ${OPENVG_PATH})
link_directories(${OPENVG_PATH})

add_executable(simple_clea_rpi simple_clea_rpi qrcodegen/qrcodegen)
target_link_libraries(simple_clea_rpi clea shapes)
add_dependencies(simple_clea_rpi openvg)

endif("${PLATFORM}" STREQUAL "RPI")