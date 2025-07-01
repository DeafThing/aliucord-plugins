version = "2.0.0"
description = "Enhanced system information command with streamlined data and improved UX"

aliucord {
    author("Vendicated", 343383572805058560L)
    changelog.set(
        """
            # v2.0.0 - Major Enhancement Update
            
            ## New Features
            - Added comprehensive device information (manufacturer, model)
            - Added storage usage tracking (internal/external with percentages)
            - Added CPU and kernel version information
            - Added Android security patch level monitoring
            - Added `detailed` parameter for technical users
            - Enhanced root detection with multiple methods
            
            ## UX Improvements
            - Clean, professional design without visual clutter
            - Streamlined information display (removed redundant fields)
            - Improved memory and storage usage formatting with percentages
            - Better uptime formatting (days, hours, minutes, seconds)
            - Enhanced public message format with readable code blocks
            - Professional embed design with timestamps
            - Logical organization of system information
            
            ## Technical Improvements
            - Removed redundant fields (brand/product vs device, duplicate build info)
            - Combined related information (build type + tags)
            - Truncated verbose output (ABIs, fingerprint) for readability
            - Enhanced error handling for all system calls
            - More accurate memory and storage calculations
            - Optimized performance and reduced overhead
            
            ## Information Included
            **Basic (8 fields):**
            - Device name, Android version, architecture
            - Root status, memory usage, internal storage
            - Security patch level, system uptime
            
            **Detailed (8 additional fields):**
            - Kernel version, CPU info, bootloader
            - Build type/tags, external storage
            - Hardware platform, supported ABIs, build fingerprint
        """.trimIndent()
    )
}