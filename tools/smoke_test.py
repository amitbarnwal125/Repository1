#!/usr/bin/env python3
"""End-to-end smoke test run on an Android emulator in CI. Writes screenshots + report to out/."""
import os, re, subprocess, sys, time, xml.etree.ElementTree as ET

OUT = os.path.abspath("out"); os.makedirs(OUT, exist_ok=True)
PKG = "com.swara.music"
APK = sys.argv[1]
report = []

def sh(cmd, check=False, timeout=120):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f"{cmd}\n{r.stdout}\n{r.stderr}")
    return r.stdout + r.stderr

def log(msg):
    print(msg, flush=True); report.append(msg)

def shot(name):
    sh(f"adb exec-out screencap -p > {OUT}/{name}.png")
    log(f"screenshot {name}")

def nodes():
    sh("adb shell uiautomator dump /sdcard/ui.xml")
    xml = sh("adb shell cat /sdcard/ui.xml")
    xml = xml[xml.find("<?xml"):] if "<?xml" in xml else xml
    try:
        root = ET.fromstring(xml[: xml.rfind(">") + 1])
    except Exception:
        return []
    res = []
    for n in root.iter("node"):
        b = re.findall(r"\d+", n.get("bounds", ""))
        if len(b) == 4:
            x1, y1, x2, y2 = map(int, b)
            res.append((n.get("text", ""), n.get("content-desc", ""), (x1 + x2) // 2, (y1 + y2) // 2))
    return res

def find(pattern, wait=20, desc=False):
    end = time.time() + wait
    rx = re.compile(pattern, re.I)
    while time.time() < end:
        for text, cd, x, y in nodes():
            if rx.search(cd if desc else text) or (not desc and rx.search(cd)):
                return x, y
        time.sleep(1.5)
    return None

def tap(pattern, wait=20, desc=False, label=None):
    p = find(pattern, wait, desc)
    if p:
        sh(f"adb shell input tap {p[0]} {p[1]}")
        log(f"tapped '{label or pattern}' at {p}")
        time.sleep(1.5)
        return True
    log(f"NOT FOUND: '{label or pattern}'")
    return False

def media_state():
    out = sh("adb shell dumpsys media_session")
    m = re.findall(r"state=PlaybackState \{state=(\w+)\((\d+)\), position=(\d+)", out)
    if not m:
        m2 = re.findall(r"state=(\d+), position=(\d+)", out)
        return m2
    return m

log("== install ==")
log(sh(f"adb install -r -g {APK}", timeout=300).strip()[-200:])
sh(f"adb shell pm grant {PKG} android.permission.POST_NOTIFICATIONS")
sh("adb logcat -c")
sh(f"adb shell am start -W -n {PKG}/.MainActivity")
ok = find("Featured tracks", wait=90)
log(f"home loaded: {bool(ok)}")
time.sleep(3)
shot("01_home")

log("== play ==")
tap("^Play$", label="hero Play")
time.sleep(12)
shot("02_playing_mini")
log(f"media session: {media_state()}")
time.sleep(5)
log(f"media session after 5s: {media_state()}")

log("== player ==")
tap("Mini player", desc=True, label="mini player")
time.sleep(3)
shot("03_player")

log("== download from player ==")
if tap("^Download ", desc=True, label="download button"):
    got = find("^Downloaded$", wait=120, desc=True)
    log(f"download completed: {bool(got)}")
shot("04_player_downloaded")

log("== queue ==")
tap("^Queue$", desc=True, label="queue")
time.sleep(2)
shot("05_queue")
sh("adb shell input keyevent 4"); time.sleep(1)
sh("adb shell input keyevent 4"); time.sleep(2)

log("== notification shade ==")
sh("adb shell cmd statusbar expand-notifications"); time.sleep(3)
shot("06_notification")
sh("adb shell cmd statusbar collapse"); time.sleep(2)

log("== library ==")
tap("^Library$", label="Library tab")
time.sleep(3)
shot("07_library")

log("== search ==")
tap("^Search$", label="Search tab")
tap("Songs, artists", label="search field")
sh("adb shell input text sitar")
time.sleep(10)
sh("adb shell input keyevent 111")  # escape → hide keyboard
time.sleep(1)
shot("08_search")

log("== collection ==")
tap("^Home$", label="Home tab"); time.sleep(2)
sh("adb shell input swipe 540 1600 540 700 400"); time.sleep(2)
tap("Raga Mornings", label="Raga Mornings playlist")
time.sleep(8)
shot("09_collection")
sh("adb shell input keyevent 4"); time.sleep(2)

log("== details sheet ==")
sh("adb shell input swipe 540 700 540 1600 400"); time.sleep(1)
tap("Music sources and licences", desc=True, label="sources")
time.sleep(2)
shot("10_sources")
sh("adb shell input keyevent 4"); time.sleep(1)

log("== offline mode ==")
sh("adb shell svc wifi disable"); sh("adb shell svc data disable"); sh("adb shell cmd connectivity airplane-mode enable")
time.sleep(6)
tap("^Library$", label="Library tab (offline)")
time.sleep(2)
tap("^Play all$", label="Play all downloads offline")
time.sleep(6)
shot("11_offline_library")
log(f"media session offline: {media_state()}")
tap("^Home$", label="Home tab (offline)")
time.sleep(3)
log(f"offline banner shown: {bool(find('Offline', wait=10))}")
shot("12_offline_home")
sh("adb shell cmd connectivity airplane-mode disable"); sh("adb shell svc wifi enable"); sh("adb shell svc data enable")

log("== background playback ==")
sh("adb shell input keyevent 3")  # HOME
time.sleep(5)
log(f"media session in background: {media_state()}")
sh("adb shell input keyevent 26"); time.sleep(3)  # screen off → lock
sh("adb shell input keyevent 26"); time.sleep(3)  # wake to lock screen
shot("13_lockscreen")
log(f"media session while locked: {media_state()}")

crash = sh("adb logcat -d -b crash")
open(f"{OUT}/crash.txt", "w").write(crash)
full = sh(f"adb logcat -d -v brief")
lines = [l for l in full.splitlines() if re.search(r"AndroidRuntime|swara|ExoPlayer|MediaSession|DownloadManager|E/", l, re.I)]
open(f"{OUT}/logcat.txt", "w").write("\n".join(lines[-1500:]))
log(f"crash buffer lines: {len(crash.strip().splitlines())}")
open(f"{OUT}/report.txt", "w").write("\n".join(report))
