import platform

if __name__ == '__main__':
    # name
    system = platform.system().upper()
    os_name = None
    if system == "MAC OS X":
        os_name = "macos"
    elif system.startswith("WIN"):
        os_name = "Windows"
    elif system.startswith("LINUX"):
        os_name = "Linux"
    else:
        print("Unsupported OS: %s" % system)
        exit(1)

    machine = platform.machine().upper()
    os_arch = None
    if machine.startswith("X86_64") or machine.startswith("AMD64"):
        os_arch = "x64"
    elif machine.startswith("AARCH64"):
        os_arch = "arm64"
    else:
        print("Unsupported OS: %s" % machine)
        exit(1)

    print("%s-%s" % (os_name, os_arch))
    exit(0)
