"""Render the local design proposal with Edge's DevTools protocol.

The device images are captured from DOM element bounds by the browser itself,
not cropped from full-page screenshots afterward.
"""

import asyncio
import base64
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
from urllib.request import urlopen

import websockets


HERE = Path(__file__).resolve().parent
EDGE = Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe")
PORT = 49231


def wait_for_page_socket():
    url = f"http://127.0.0.1:{PORT}/json"
    for _ in range(100):
        try:
            with urlopen(url, timeout=0.5) as response:
                pages = json.load(response)
            page = next(page for page in pages if page.get("type") == "page")
            return page["webSocketDebuggerUrl"]
        except (OSError, StopIteration):
            time.sleep(0.1)
    raise RuntimeError("Edge DevTools did not start")


async def render():
    temp_root = Path(tempfile.gettempdir()).resolve()
    profile = Path(tempfile.mkdtemp(prefix="nga-design-preview-", dir=temp_root)).resolve()
    assert profile.is_relative_to(temp_root) and profile.name.startswith("nga-design-preview-")
    flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    process = subprocess.Popen(
        [
            str(EDGE),
            "--headless=new",
            "--disable-gpu",
            "--hide-scrollbars",
            "--no-first-run",
            "--remote-allow-origins=*",
            f"--remote-debugging-port={PORT}",
            f"--user-data-dir={profile}",
            "about:blank",
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        creationflags=flags,
    )
    try:
        socket_url = await asyncio.to_thread(wait_for_page_socket)
        async with websockets.connect(socket_url, max_size=30_000_000) as socket:
            serial = 0

            async def command(method, params=None):
                nonlocal serial
                serial += 1
                token = serial
                await socket.send(json.dumps({"id": token, "method": method, "params": params or {}}))
                while True:
                    message = json.loads(await socket.recv())
                    if message.get("id") == token:
                        if "error" in message:
                            raise RuntimeError(message["error"])
                        return message.get("result", {})

            await command("Page.enable")
            await command("Runtime.enable")
            cases = [
                ("list", False, "device", 1600, 1080, "device-list.png"),
                ("post", False, "device", 1600, 1080, "device-post.png"),
                ("landscape", False, "landscape-device", 1600, 1080, "device-landscape.png"),
                ("fold", False, "fold-shell", 1600, 1080, "device-fold.png"),
                ("login", False, "device", 1600, 1080, "device-login.png"),
                ("list", True, "device", 1600, 1080, "device-list-dark.png"),
                ("list", False, "device", 320, 800, "device-list-320.png"),
            ]
            for view, dark, element_class, width, height, name in cases:
                await command(
                    "Emulation.setDeviceMetricsOverride",
                    {"width": width, "height": height, "deviceScaleFactor": 1, "mobile": False},
                )
                suffix = f"?preview={name}" + ("&theme=dark" if dark else "")
                await command("Page.navigate", {"url": (HERE / "index.html").as_uri() + suffix + "#" + view})
                await asyncio.sleep(0.6)
                expression = f"(() => {{const el = document.querySelector('.screen.active .{element_class}'); if (!el) return null; const r = el.getBoundingClientRect(); return {{x:r.x,y:r.y,width:r.width,height:r.height}}}})()"
                box = None
                for _ in range(20):
                    result = await command(
                        "Runtime.evaluate",
                        {"expression": expression, "returnByValue": True},
                    )
                    box = result.get("result", {}).get("value")
                    if box:
                        break
                    await asyncio.sleep(0.1)
                if box is None:
                    raise RuntimeError(f"Cannot find active {element_class} for {view}: {result}")
                if view in ("post", "landscape", "fold"):
                    dock_result = await command(
                        "Runtime.evaluate",
                        {
                            "expression": "(() => { const dock = document.querySelector('.screen.active .floating-dock'); const r = dock.getBoundingClientRect(); return {width:r.width,height:r.height,buttons:[...dock.querySelectorAll('button')].map(b => {const x=b.getBoundingClientRect(); return [x.width,x.height]})} })()",
                            "returnByValue": True,
                        },
                    )
                    dock = dock_result["result"]["value"]
                    assert all(width >= 44 and height >= 44 for width, height in dock["buttons"]), dock
                    print(f"{view} dock", dock)
                if view == "fold":
                    rail_result = await command(
                        "Runtime.evaluate",
                        {
                            "expression": "(() => {const s=document.querySelector('.screen.active .fold-system-status').getBoundingClientRect();const r=document.querySelector('.screen.active .fold-rail').getBoundingClientRect();const items=[...document.querySelectorAll('.screen.active .fold-rail .brand-mark,.screen.active .fold-rail button')].map(x=>x.getBoundingClientRect());return {statusBottom:s.bottom,railMid:(r.top+r.bottom)/2,groupTop:items[0].top,groupBottom:items.at(-1).bottom,buttons:items.slice(1).map(x=>[x.width,x.height])}})()",
                            "returnByValue": True,
                        },
                    )
                    rail = rail_result["result"]["value"]
                    assert rail["groupTop"] > rail["statusBottom"]
                    assert abs((rail["groupTop"] + rail["groupBottom"]) / 2 - rail["railMid"]) < 2
                    assert all(width >= 44 and height >= 44 for width, height in rail["buttons"])
                    print("fold navigation", rail)
                screenshot = await command(
                    "Page.captureScreenshot",
                    {
                        "format": "png",
                        "fromSurface": True,
                        "captureBeyondViewport": True,
                        "clip": {**box, "scale": 1},
                    },
                )
                (HERE / name).write_bytes(base64.b64decode(screenshot["data"]))
                full = await command(
                    "Page.captureScreenshot",
                    {"format": "png", "fromSurface": True, "captureBeyondViewport": False},
                )
                page_name = (
                    "preview-narrow.png" if width == 320 else
                    "preview-list-dark.png" if dark else
                    f"preview-{view}.png"
                )
                (HERE / page_name).write_bytes(base64.b64decode(full["data"]))
                if view in ("post", "landscape"):
                    await command(
                        "Runtime.evaluate",
                        {"expression": f"document.querySelector('.screen.active .content-scroll').scrollTop = {240 if view == 'post' else 190}"},
                    )
                    await asyncio.sleep(0.25)
                    scrolled = await command(
                        "Page.captureScreenshot",
                        {
                            "format": "png",
                            "fromSurface": True,
                            "captureBeyondViewport": True,
                            "clip": {**box, "scale": 1},
                        },
                    )
                    (HERE / f"device-{view}-scrolled.png").write_bytes(base64.b64decode(scrolled["data"]))
                print(name, box)
            await command(
                "Emulation.setDeviceMetricsOverride",
                {"width": 1024, "height": 390, "deviceScaleFactor": 1, "mobile": False},
            )
            await command(
                "Page.navigate",
                {"url": (HERE / "index.html").as_uri() + "?preview=fold-short#fold"},
            )
            await asyncio.sleep(0.6)
            short_result = await command(
                "Runtime.evaluate",
                {
                    "expression": "(() => {const r=document.querySelector('.screen.active .fold-rail');const before={client:r.clientHeight,scroll:r.scrollHeight};r.scrollTop=r.scrollHeight;return {...before,afterScroll:r.scrollTop}})()",
                    "returnByValue": True,
                },
            )
            short = short_result["result"]["value"]
            assert short["scroll"] > short["client"] and short["afterScroll"] > 0, short
            print("fold short navigation", short)
    finally:
        process.terminate()
        try:
            process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=8)
        if profile.is_relative_to(temp_root) and profile.name.startswith("nga-design-preview-"):
            shutil.rmtree(profile, ignore_errors=True)


if __name__ == "__main__":
    asyncio.run(render())
