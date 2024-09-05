def ensure_path_submit():
    from pathlib import Path

    path = Path("submit")
    if not path.is_dir():
        print(f"==> \033[32mPath `\033[36m{path}\033[32m` creating...\033[0m")
        path.mkdir()


def pack(*args):
    from time import localtime, strftime, sleep
    from pathlib import Path
    from zipfile import ZipFile

    ensure_path_submit()

    filename = (
        "homework" + "_".join(args) + "_" +
        strftime("%y%m%d_%H%M%S", localtime()) + ".zip"
    )
    zipfile = Path("submit") / filename
    srcpath = Path("src")
    print(f"--> \033[32mTarget: `\033[36m{zipfile}\033[32m`\033[0m")

    print("--> \033[32mScanning...\033[0m")
    filelist = []
    for file in srcpath.glob("**/*.java"):
        print(".", end='', flush=True)
        sleep(0.02)
        filelist.append((str(file), str(file.relative_to(srcpath))))
    print()

    print("--> \033[32mPacking...\033[0m")
    with ZipFile(str(zipfile), "w") as zpf:
        for i, (file, relative) in enumerate(filelist, 1):
            percent = str(i * 100 // len(filelist))
            print(f"{' ' * (3 - len(percent))}{percent}% {relative}")
            zpf.write(file, arcname=relative)
            sleep(0.03)


tasks = {
    "pack": pack
}

default_task = "pack"


if __name__ == "__main__":
    import sys

    task = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] != "-" else default_task
    args = sys.argv[2:] if len(sys.argv) > 2 else []
    if task not in tasks:
        print(f"\033[31mUnknown task: `\033[36m{task}\033[31m`!\033[0m")
        print(f"\033[31mAvailable tasks: \033[36m{list(tasks.keys())}\033[0m")
        sys.exit(1)
    print(f"==> \033[32mTask: `\033[36m{task}\033[32m`; Args: \033[36m{args}\033[0m")
    tasks[task](*args)
    print("==> \033[32mDone\033[0m")
